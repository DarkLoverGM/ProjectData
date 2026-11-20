package com.yt.backend.config;

import com.yt.backend.auth.BanList;
import com.yt.backend.auth.JwtService;
import com.yt.backend.security.AccessTokenReader;
import com.yt.backend.websocket.AdminRealtime;
import com.yt.backend.websocket.AuthHandshakeInterceptor;
import com.yt.backend.websocket.AuthSocketHandler;
import com.yt.backend.websocket.AuthSockets;
import com.yt.backend.websocket.UserRealtime;
import com.yt.backend.websocket.ViewSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * - "/": frontend mo ws://host:3001 (luot xem, binh luan realtime), khong can dang nhap.
 *   Chi bat request upgrade WebSocket, GET "/" binh thuong van toi RootController.
 * - "/ws/admin": trang quan tri nhan su kien moi (AdminRealtime). Chi admin.
 * - "/ws/user": su kien rieng cua nguoi dung da dang nhap, vd. Studio (UserRealtime).
 * 2 kenh dang nhap xac thuc bang cookie `token`; Origin phai la chinh server, CLIENT_URL / SERVER_URL
 * hoac frontend trong danh sach CORS, de trang web khac khong mo duoc socket bang cookie cua nguoi dung
 * (Cross-Site WebSocket Hijacking).
 */
@Configuration
public class WebSocketConfig {

    @Bean
    public WebSocketHandlerMapping viewSocketMapping(ViewSocketHandler handler, AdminRealtime adminRealtime,
                                                     UserRealtime userRealtime, JwtService jwt, BanList banList,
                                                     AccessTokenReader tokens,
                                                     @Value("${app.client-url}") String clientUrl,
                                                     @Value("${app.server-url}") String serverUrl) {
        // Danh sach rong van cho phep cung origin; them cac dia chi cau hinh
        List<String> origins = new ArrayList<>(WebConfig.ALLOWED_ORIGINS);
        origins.add(clientUrl);
        origins.add(serverUrl);

        WebSocketHandlerMapping mapping = new WebSocketHandlerMapping();
        mapping.setUrlMap(Map.of(
                "/", new WebSocketHttpRequestHandler(handler, new DefaultHandshakeHandler()),
                "/ws/admin", authenticated(adminRealtime, new AuthHandshakeInterceptor(jwt, banList, tokens, true), origins),
                "/ws/user", authenticated(userRealtime, new AuthHandshakeInterceptor(jwt, banList, tokens, false), origins)));
        mapping.setWebSocketUpgradeMatch(true);
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return mapping;
    }

    private static WebSocketHttpRequestHandler authenticated(AuthSockets sockets, AuthHandshakeInterceptor auth,
                                                             List<String> origins) {
        WebSocketHttpRequestHandler h = new WebSocketHttpRequestHandler(new AuthSocketHandler(sockets),
                new DefaultHandshakeHandler());
        h.setHandshakeInterceptors(List.of(new OriginHandshakeInterceptor(origins), auth));
        return h;
    }
}
