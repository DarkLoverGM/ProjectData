package com.yt.backend.websocket;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.auth.BanList;
import com.yt.backend.auth.JwtService;
import com.yt.backend.security.AccessTokenReader;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Xac thuc luc mo /ws/admin, /ws/user: access token (cookie `token`) hop le, khong bi khoa, va (adminOnly) role admin.
 * Sai thi tu choi handshake: 401 (khong co / het han token -> client refresh roi thu lai), 403 (khong du quyen / bi khoa).
 * Kiem tra Origin do OriginHandshakeInterceptor chay truoc (WebSocketConfig) lo, chong trang la mo socket bang cookie nguoi dung.
 */
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwt;
    private final BanList banList;
    private final AccessTokenReader tokens;
    private final boolean adminOnly;

    public AuthHandshakeInterceptor(JwtService jwt, BanList banList, AccessTokenReader tokens, boolean adminOnly) {
        this.jwt = jwt;
        this.banList = banList;
        this.tokens = tokens;
        this.adminOnly = adminOnly;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servlet)) return reject(response, HttpStatus.BAD_REQUEST);
        String token = tokens.read(servlet.getServletRequest());
        if (token == null) return reject(response, HttpStatus.UNAUTHORIZED);

        AuthUser user;
        try {
            user = jwt.verifyAccess(token);
        } catch (JwtException | IllegalArgumentException e) {
            return reject(response, HttpStatus.UNAUTHORIZED);
        }
        if (user.id() == null) return reject(response, HttpStatus.UNAUTHORIZED);
        if (banList.isBanned(user.id())) return reject(response, HttpStatus.FORBIDDEN);
        if (adminOnly && !"admin".equals(user.role())) return reject(response, HttpStatus.FORBIDDEN);

        attributes.put(AuthSockets.USER_ATTR, user);
        attributes.put(AuthSockets.EXPIRES_ATTR, jwt.expiresAtOf(token));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
                               Exception exception) {
    }

    private static boolean reject(ServerHttpResponse response, HttpStatus status) {
        response.setStatusCode(status);
        return false;
    }
}
