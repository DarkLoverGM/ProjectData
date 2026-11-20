package com.yt.backend.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yt.backend.dto.video.VideoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * "Phong" tren WebSocket (utils/realtime.js): client gui { type: 'join', room } / { type: 'leave', room },
 * controller goi broadcast(room, payload) de day su kien toi moi nguoi trong phong.
 * Hien dung cho binh luan va luot xem/like: phong `video:<videoId>`.
 */
@Component
public class Realtime {

    private static final Logger log = LoggerFactory.getLogger(Realtime.class);

    private static final Pattern ROOM_PATTERN = Pattern.compile("^video:[\\w-]{1,64}$");
    private static final int MAX_ROOMS_PER_SOCKET = 20;
    private static final String ROOMS_ATTR = "rooms";

    private final ObjectMapper mapper;
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    public Realtime(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public static String videoRoom(Object videoId) {
        return "video:" + videoId;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> roomsOf(WebSocketSession socket) {
        return (Set<String>) socket.getAttributes().computeIfAbsent(ROOMS_ATTR, k -> ConcurrentHashMap.newKeySet());
    }

    private void join(WebSocketSession socket, String room) {
        if (!ROOM_PATTERN.matcher(room).matches()) return;
        Set<String> joined = roomsOf(socket);
        if (joined.contains(room) || joined.size() >= MAX_ROOMS_PER_SOCKET) return;
        rooms.computeIfAbsent(room, k -> ConcurrentHashMap.newKeySet()).add(socket);
        joined.add(room);
    }

    private void leave(WebSocketSession socket, String room) {
        rooms.computeIfPresent(room, (k, members) -> {
            members.remove(socket);
            return members.isEmpty() ? null : members;
        });
        roomsOf(socket).remove(room);
    }

    /** Goi khi socket dong de roi moi phong */
    public void unregister(WebSocketSession socket) {
        for (String room : Set.copyOf(roomsOf(socket))) leave(socket, room);
    }

    /** Xu ly tin nhan join/leave. Tra true neu da xu ly (de handler chung bo qua) */
    public boolean handleMessage(WebSocketSession socket, JsonNode msg) {
        String type = msg.path("type").asText();
        if ("join".equals(type)) {
            join(socket, msg.path("room").asText(""));
            return true;
        }
        if ("leave".equals(type)) {
            leave(socket, msg.path("room").asText(""));
            return true;
        }
        return false;
    }

    /** Gui su kien toi moi nguoi dang o trong phong `video:<id>` */
    public void broadcast(String videoId, RealtimeEvent payload) {
        String room = videoRoom(videoId);
        Set<WebSocketSession> members = rooms.get(room);
        if (members == null) return;

        TextMessage msg;
        try {
            msg = new TextMessage(mapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.error("WebSocket payload error", e);
            return;
        }
        for (WebSocketSession s : members) {
            if (!s.isOpen()) continue;
            try {
                // sendMessage khong thread-safe tren cung 1 session
                synchronized (s) {
                    s.sendMessage(msg);
                }
            } catch (IOException e) {
                log.warn("WebSocket send error: {}", e.getMessage());
            }
        }
    }

    /** Luot xem / like / dislike moi cua 1 video cho nguoi dang xem video do */
    public void broadcastVideoStats(VideoDto video) {
        if (video == null || video.id() == null) return;
        broadcast(video.id(), new RealtimeEvent.VideoStats(video.id(), video.stats().view(), video.stats().like(),
                video.stats().dislike()));
    }
}
