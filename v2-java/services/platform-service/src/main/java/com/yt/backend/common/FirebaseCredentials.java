package com.yt.backend.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

/**
 * Service account Firebase dung chung cho Storage (FileStorage) va Cloud Messaging (PushService), giong utils/firebase.js.
 * Service account lay o Firebase Console > Project settings > Service accounts > Generate new private key.
 * Chay local: dat file JSON trong server-java/ va tro FIREBASE_SERVICE_ACCOUNT_PATH toi no (tinh tu server-java/).
 * Host khong co file: dat 3 bien FIREBASE_PROJECT_ID / FIREBASE_CLIENT_EMAIL / FIREBASE_PRIVATE_KEY.
 */
@Component
public class FirebaseCredentials {

    private static final Logger log = LoggerFactory.getLogger(FirebaseCredentials.class);

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    // 1 access token dung duoc cho ca Storage lan FCM
    private static final String SCOPE = "https://www.googleapis.com/auth/devstorage.read_write "
            + "https://www.googleapis.com/auth/firebase.messaging";

    private record Credential(String projectId, String clientEmail, PrivateKey privateKey) {}

    private final ObjectMapper mapper;
    private final Credential credential;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    private String accessToken;
    private long accessTokenExpiresAt;

    public FirebaseCredentials(ObjectMapper mapper,
                               @Value("${app.firebase.service-account-path}") String serviceAccountPath,
                               @Value("${app.firebase.project-id}") String projectId,
                               @Value("${app.firebase.client-email}") String clientEmail,
                               @Value("${app.firebase.private-key}") String privateKey) {
        this.mapper = mapper;
        Credential cred = null;
        try {
            cred = load(serviceAccountPath, projectId, clientEmail, privateKey);
        } catch (Exception e) {
            log.error("[firebase] Khong doc duoc service account Firebase: {}", e.getMessage());
        }
        this.credential = cred;
    }

    private Credential load(String file, String projectId, String clientEmail, String privateKey) throws Exception {
        if (!file.isBlank()) {
            JsonNode json = mapper.readTree(Path.of(file.trim()).toAbsolutePath().normalize().toFile());
            return new Credential(json.path("project_id").asText(), json.path("client_email").asText(),
                    parsePrivateKey(json.path("private_key").asText()));
        }
        if (!projectId.isBlank() && !clientEmail.isBlank() && !privateKey.isBlank()) {
            // Bien moi truong thuong giu "\n" dang chu, phai doi lai thanh xuong dong that
            return new Credential(projectId.trim(), clientEmail.trim(), parsePrivateKey(privateKey.replace("\\n", "\n")));
        }
        return null;
    }

    private static PrivateKey parsePrivateKey(String pem) throws Exception {
        String base64 = pem.replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "").replaceAll("\\s", "");
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }

    public boolean isConfigured() {
        return credential != null;
    }

    public String projectId() {
        return credential == null ? null : credential.projectId();
    }

    /** Access token OAuth cua service account (JWT bearer), giu lai toi khi gan het han */
    public synchronized String accessToken() throws IOException {
        if (credential == null) throw new IOException("Chua cau hinh service account Firebase");
        long now = System.currentTimeMillis();
        if (accessToken != null && now < accessTokenExpiresAt - 60_000) return accessToken;

        String assertion = Jwts.builder()
                .issuer(credential.clientEmail())
                .claim("scope", SCOPE)
                // Google bat buoc "aud" la chuoi, add() se ghi thanh mang ["..."] -> invalid_grant
                .audience().single(TOKEN_URL)
                .issuedAt(new Date(now / 1000 * 1000))
                .expiration(new Date(now / 1000 * 1000 + 3_600_000))
                .signWith(credential.privateKey(), Jwts.SIG.RS256)
                .compact();
        String form = "grant_type=" + FileStorage.encodeURIComponent("urn:ietf:params:oauth:grant-type:jwt-bearer")
                + "&assertion=" + assertion;
        HttpResponse<String> res;
        try {
            res = http.send(HttpRequest.newBuilder(URI.create(TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
        if (res.statusCode() / 100 != 2) throw new IOException("Firebase token error: " + res.body());
        JsonNode json = mapper.readTree(res.body());
        accessToken = json.path("access_token").asText();
        accessTokenExpiresAt = now + json.path("expires_in").asLong(3600) * 1000;
        return accessToken;
    }
}
