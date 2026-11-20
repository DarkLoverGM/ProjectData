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

class UserRealtimeTest {

    private final UserRealtime realtime = new UserRealtime(new ObjectMapper());

    @AfterEach
    void tearDown() {
        realtime.shutdown();
    }

    private WebSocketSession session(String userId) {
        WebSocketSession s = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(AuthSockets.USER_ATTR, new AuthUser(userId, userId + "@example.com", userId, "user"));
        attributes.put(AuthSockets.EXPIRES_ATTR, System.currentTimeMillis() + 600_000L);
        when(s.getAttributes()).thenReturn(attributes);
        when(s.isOpen()).thenReturn(true);
        realtime.register(s);
        return s;
    }

    @Test
    void sendToReachesEveryTabOfThatUserOnly() throws Exception {
        WebSocketSession ownerTab1 = session("owner");
        WebSocketSession ownerTab2 = session("owner");
        WebSocketSession stranger = session("someone-else");

        realtime.sendTo("owner", new UserEvent.StudioVideo("v1", "Admin đã phản hồi", "\"Video\": ok"));

        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);
        verify(ownerTab1).sendMessage(sent.capture());
        verify(ownerTab2).sendMessage(any(TextMessage.class));
        verify(stranger, never()).sendMessage(any());
        String json = sent.getValue().getPayload();
        assertTrue(json.contains("\"type\":\"studio:video\""), json);
        assertTrue(json.contains("\"videoId\":\"v1\""), json);
    }

    @Test
    void closeUserDisconnectsBannedUser() throws Exception {
        WebSocketSession banned = session("u1");
        session("u2");

        realtime.closeUser("u1");

        verify(banned).close(AuthSockets.REVOKED);
        assertEquals(1, realtime.online());
    }
}
