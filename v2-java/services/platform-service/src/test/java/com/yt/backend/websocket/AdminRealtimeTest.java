package com.yt.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yt.backend.auth.AuthUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminRealtimeTest {

    private final AdminRealtime realtime = new AdminRealtime(new ObjectMapper());

    @AfterEach
    void tearDown() {
        realtime.shutdown();
    }

    private WebSocketSession session(String userId, long tokenExpiresAt) {
        WebSocketSession s = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(AuthSockets.USER_ATTR, new AuthUser(userId, userId + "@example.com", userId, "admin"));
        attributes.put(AuthSockets.EXPIRES_ATTR, tokenExpiresAt);
        when(s.getAttributes()).thenReturn(attributes);
        when(s.isOpen()).thenReturn(true);
        realtime.register(s);
        return s;
    }

    private static long inMinutes(int m) {
        return System.currentTimeMillis() + m * 60_000L;
    }

    @Test
    void publishSendsEventJsonToEveryAdmin() throws Exception {
        WebSocketSession a = session("a1", inMinutes(10));
        WebSocketSession b = session("a2", inMinutes(10));

        realtime.publish(new AdminEvent.CommentReported("r1", "c1", "v1", "spam"));

        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);
        verify(a).sendMessage(sent.capture());
        verify(b).sendMessage(any(TextMessage.class));
        String json = sent.getValue().getPayload();
        assertTrue(json.contains("\"type\":\"report:comment\""), json);
        assertTrue(json.contains("\"reportId\":\"r1\""), json);
    }

    @Test
    void expiredTokenIsClosedInsteadOfReceivingEvents() throws Exception {
        WebSocketSession expired = session("a1", System.currentTimeMillis() - 1);

        realtime.publish(new AdminEvent.UserNew("u1", "moi"));

        verify(expired, never()).sendMessage(any());
        verify(expired).close(AuthSockets.TOKEN_EXPIRED);
        assertEquals(0, realtime.online());
    }

    @Test
    void closeUserOnlyDisconnectsThatAdmin() throws Exception {
        WebSocketSession demoted = session("a1", inMinutes(10));
        WebSocketSession other = session("a2", inMinutes(10));

        realtime.closeUser("a1");

        verify(demoted).close(AuthSockets.REVOKED);
        verify(other, never()).close(any());
        assertEquals(1, realtime.online());
    }
}
