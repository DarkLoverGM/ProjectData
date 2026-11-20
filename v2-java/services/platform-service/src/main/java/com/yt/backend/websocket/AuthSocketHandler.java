package com.yt.backend.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Handler chung cho /ws/admin va /ws/user (WebSocketConfig tao). Chi server day su kien;
 * client chi gui "ping" de giu ket noi qua proxy, server tra "pong".
 */
public class AuthSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthSocketHandler.class);
    private static final TextMessage PONG = new TextMessage("{\"type\":\"pong\"}");

    private final AuthSockets sockets;

    public AuthSocketHandler(AuthSockets sockets) {
        this.sockets = sockets;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sockets.register(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if (message.getPayload().contains("\"ping\"")) sockets.send(session, PONG);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("WebSocket error: {}", exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sockets.unregister(session);
    }
}
