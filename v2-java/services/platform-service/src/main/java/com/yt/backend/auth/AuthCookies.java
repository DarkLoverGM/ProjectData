package com.yt.backend.auth;

import com.yt.backend.dto.auth.TokenExpiry;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Ghi ca access token (`token`) lan refresh token (`refreshToken`) vao cookie (utils/authCookies.js).
 * Cookie `token` song bang refresh token: access token het han van duoc gui len,
 * server tra 401 TOKEN_EXPIRED de frontend biet ma refresh (thay vi coi nhu khach).
 */
@Component
public class AuthCookies {

    // Refresh token chi duoc gui toi /api/user/* (token-refresh, logout), khong di kem moi request
    private static final String REFRESH_COOKIE_PATH = "/api/user";

    private final JwtService jwt;
    private final boolean secure;

    public AuthCookies(JwtService jwt, @Value("${app.cookie.secure}") boolean secure) {
        this.jwt = jwt;
        this.secure = secure;
    }

    public boolean isSecure() {
        return secure;
    }

    /** Ghi 2 cookie, tra ve thoi diem het han cua tung token */
    public TokenExpiry set(HttpServletResponse res, AuthUser user) {
        String accessToken = jwt.generateToken(user);
        String refreshToken = jwt.generateRefreshToken(user);
        long accessExpiresAt = jwt.expiresAtOf(accessToken);
        long refreshExpiresAt = jwt.expiresAtOf(refreshToken);
        Duration maxAge = Duration.ofMillis(Math.max(0, refreshExpiresAt - System.currentTimeMillis()));

        res.addHeader(HttpHeaders.SET_COOKIE, cookie("token", accessToken, "/", maxAge).toString());
        res.addHeader(HttpHeaders.SET_COOKIE, cookie("refreshToken", refreshToken, REFRESH_COOKIE_PATH, maxAge).toString());

        return new TokenExpiry(accessExpiresAt, refreshExpiresAt);
    }

    public void clear(HttpServletResponse res) {
        res.addHeader(HttpHeaders.SET_COOKIE, cookie("token", "", "/", Duration.ZERO).toString());
        res.addHeader(HttpHeaders.SET_COOKIE, cookie("refreshToken", "", REFRESH_COOKIE_PATH, Duration.ZERO).toString());
    }

    private ResponseCookie cookie(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(secure)
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
