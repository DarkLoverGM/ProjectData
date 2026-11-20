package com.yt.backend.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yt.backend.auth.AuthUser;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Cac ket noi WebSocket da dang nhap (AuthHandshakeInterceptor xac thuc luc ket noi): /ws/admin, /ws/user.
 * - Access token chi song 15 phut nen ket noi cung chi duoc giu toi luc token het han:
 *   het han thi dong voi ma TOKEN_EXPIRED, client refresh token roi ket noi lai.
 *   Nho vay nguoi bi khoa / bi ha quyen khong nghe duoc su kien qua 1 vong token.
 * - closeUser(): ngat ngay 1 nguoi (bi khoa, bi xoa, bi ha quyen admin).
 * Luu trong RAM: chi dung khi chay 1 process (giong Realtime, BanList).
 */
public abstract class AuthSockets {

    private static final Logger log = LoggerFactory.getLogger(AuthSockets.class);

    /** Ma dong ket noi rieng (4000-4999 danh cho ung dung) */
    public static final CloseStatus TOKEN_EXPIRED = new CloseStatus(4401, "TOKEN_EXPIRED");
    public static final CloseStatus REVOKED = new CloseStatus(4403, "REVOKED");

    static final String USER_ATTR = "authUser";
    static final String EXPIRES_ATTR = "tokenExpiresAt";
    private static final long SWEEP_SECONDS = 15;

    private final ObjectMapper mapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService sweeper;

    protected AuthSockets(ObjectMapper mapper, String name) {
        this.mapper = mapper;
        this.sweeper = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, name + "-ws-sweeper");
            t.setDaemon(true);
            return t;
        });
        sweeper.scheduleWithFixedDelay(this::closeExpired, SWEEP_SECONDS, SWEEP_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    void shutdown() {
        sweeper.shutdownNow();
    }

    public static AuthUser userOf(WebSocketSession s) {
        return (AuthUser) s.getAttributes().get(USER_ATTR);
    }

    void register(WebSocketSession s) {
        sessions.add(s);
    }

    void unregister(WebSocketSession s) {
        sessions.remove(s);
    }

    public int online() {
        return sessions.size();
    }

    /** Dong moi ket noi cua 1 user */
    public void closeUser(String userId) {
        for (WebSocketSession s : sessions) {
            AuthUser u = userOf(s);
            if (u != null && u.id().equals(userId)) close(s, REVOKED);
        }
    }

    /** Gui su kien toi moi ket noi thoa dieu kien (null = tat ca) */
    protected void sendWhere(Object event, String userId) {
        if (sessions.isEmpty()) return;
        TextMessage msg;
        try {
            msg = new TextMessage(mapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.error("WebSocket payload error", e);
            return;
        }
        long now = System.currentTimeMillis();
        for (WebSocketSession s : sessions) {
            if (userId != null) {
                AuthUser u = userOf(s);
                if (u == null || !userId.equals(u.id())) continue;
            }
            if (expired(s, now)) {
                close(s, TOKEN_EXPIRED);
                continue;
            }
            send(s, msg);
        }
    }

    void send(WebSocketSession s, TextMessage msg) {
        if (!s.isOpen()) {
            sessions.remove(s);
            return;
        }
        try {
            // sendMessage khong thread-safe tren cung 1 session
            synchronized (s) {
                s.sendMessage(msg);
            }
        } catch (IOException e) {
            log.warn("WebSocket send error: {}", e.getMessage());
        }
    }

    private static boolean expired(WebSocketSession s, long now) {
        Object exp = s.getAttributes().get(EXPIRES_ATTR);
        return exp instanceof Long at && at > 0 && at <= now;
    }

    private void closeExpired() {
        long now = System.currentTimeMillis();
        for (WebSocketSession s : sessions) {
            if (expired(s, now)) close(s, TOKEN_EXPIRED);
        }
    }

    private void close(WebSocketSession s, CloseStatus status) {
        sessions.remove(s);
        try {
            s.close(status);
        } catch (IOException e) {
            log.debug("WebSocket close error: {}", e.getMessage());
        }
    }
}
