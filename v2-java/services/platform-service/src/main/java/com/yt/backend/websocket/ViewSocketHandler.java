package com.yt.backend.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yt.backend.dto.video.VideoDto;
import com.yt.backend.service.VideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket o "/" (Servertest.js):
 * - { type: 'join' | 'leave', room } -> vao/ra phong (xem Realtime)
 * - { type: 'view', id }             -> tang luot xem, phat { type: 'video:stats', ... } cho nguoi dang xem video do
 */
@Component
public class ViewSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ViewSocketHandler.class);

    private final VideoService videoService;
    private final Realtime realtime;
    private final ObjectMapper mapper = new ObjectMapper();

    public ViewSocketHandler(VideoService videoService, Realtime realtime) {
        this.videoService = videoService;
        this.realtime = realtime;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode d = mapper.readTree(message.getPayload());
            if (realtime.handleMessage(session, d)) return;
            if (!"view".equals(d.path("type").asText())) return;

            VideoDto updated = videoService.incrementViewCount(d.path("id").asText());
            // Chi gui cho nguoi dang xem video nay, khong phai moi client
            realtime.broadcastVideoStats(updated);
        } catch (Exception e) {
            log.error("WebSocket message error", e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket error", exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        realtime.unregister(session);
    }
}
