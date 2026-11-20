package com.yt.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yt.backend.common.FirebaseCredentials;
import com.yt.backend.entity.PushToken;
import com.yt.backend.repository.PushTokenRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Push notification qua Firebase Cloud Messaging (FCM HTTP v1), giong utils/push.js.
 * Moi trinh duyet bat thong bao co 1 FCM token, luu o collection `push_tokens` { token, user_id, updated_at }
 * (client dang ky qua POST /api/user/push-token). Thong bao van luu DB nhu cu, push chi la "ban" them
 * ra ngoai de nguoi dung thay ca khi da dong tab. Chua cau hinh service account -> bo qua, khong loi.
 */
@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);

    // FCM v1 gui tung token 1 -> gui song song theo lo de khong mo qua nhieu request cung luc
    private static final int CONCURRENCY = 100;

    private final PushTokenRepository tokens;
    private final MongoTemplate mongo;
    private final FirebaseCredentials firebase;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public PushService(PushTokenRepository tokens, MongoTemplate mongo, FirebaseCredentials firebase, ObjectMapper mapper) {
        this.tokens = tokens;
        this.mongo = mongo;
        this.firebase = firebase;
        this.mapper = mapper;
        if (!firebase.isConfigured()) log.warn("[push] Thiếu service account Firebase -> không gửi push notification");
    }

    public void saveToken(String userId, String token) {
        mongo.upsert(Query.query(Criteria.where("token").is(token)),
                new Update().set("user_id", userId).set("updated_at", new Date()), PushToken.class);
    }

    public void removeToken(String token) {
        tokens.deleteOneByToken(token);
    }

    /**
     * Gui push toi moi thiet bi cua cac user, chay nen nen khong lam cham request.
     * Gui dang data-only, service worker (public/firebase-messaging-sw.js) tu hien thong bao
     * va mo `link` khi bam -> chay duoc ca tren http://localhost. Loi chi ghi log.
     */
    public void sendPush(Collection<?> userIds, String title, String body, String link) {
        if (!firebase.isConfigured() || userIds == null || userIds.isEmpty()) return;
        List<String> ids = userIds.stream().map(String::valueOf).toList();
        Map<String, String> data = Map.of(
                "title", title == null ? "" : title,
                "body", body == null ? "" : body,
                "link", link == null || link.isEmpty() ? "/" : link);
        CompletableFuture.runAsync(() -> {
            try {
                deliver(ids, data);
            } catch (Exception e) {
                log.error("[push] Lỗi gửi push", e);
            }
        });
    }

    public void sendPush(Collection<?> userIds, String title, String body) {
        sendPush(userIds, title, body, "/");
    }

    private void deliver(List<String> userIds, Map<String, String> data) throws Exception {
        List<String> targets = tokens.findByUserIdIn(userIds).stream().map(PushToken::getToken).distinct().toList();
        if (targets.isEmpty()) return;

        URI url = URI.create("https://fcm.googleapis.com/v1/projects/" + firebase.projectId() + "/messages:send");
        String accessToken = firebase.accessToken();
        List<String> dead = new ArrayList<>();
        for (int i = 0; i < targets.size(); i += CONCURRENCY) {
            List<String> chunk = targets.subList(i, Math.min(i + CONCURRENCY, targets.size()));
            List<CompletableFuture<HttpResponse<String>>> sends = new ArrayList<>();
            for (String token : chunk) {
                String message = mapper.writeValueAsString(Map.of("message", Map.of(
                        "token", token,
                        "data", data,
                        "webpush", Map.of("headers", Map.of("Urgency", "high")))));
                sends.add(http.sendAsync(HttpRequest.newBuilder(url)
                        .timeout(Duration.ofSeconds(30))
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(message)).build(),
                        HttpResponse.BodyHandlers.ofString()));
            }
            for (int j = 0; j < sends.size(); j++) {
                try {
                    HttpResponse<String> res = sends.get(j).join();
                    if (res.statusCode() / 100 == 2) continue;
                    if (isDeadToken(res)) dead.add(chunk.get(j));
                    else log.warn("[push] FCM {}: {}", res.statusCode(), res.body());
                } catch (Exception e) {
                    log.warn("[push] Không gửi được tới 1 thiết bị: {}", e.getMessage());
                }
            }
        }
        if (!dead.isEmpty()) tokens.deleteByTokenIn(dead);
    }

    /** Token da bi go (huy quyen, xoa du lieu trinh duyet...) hoac sai dinh dang -> xoa khoi DB */
    private static boolean isDeadToken(HttpResponse<String> res) {
        String body = res.body() == null ? "" : res.body();
        return res.statusCode() == 404 || body.contains("UNREGISTERED")
                || (res.statusCode() == 400 && body.contains("registration token"));
    }
}
