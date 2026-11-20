package com.yt.backend.websocket;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.auth.BanList;
import com.yt.backend.auth.JwtService;
import com.yt.backend.security.AccessTokenReader;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthHandshakeInterceptorTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123";
    private static final AuthUser ADMIN = new AuthUser("a1", "admin@example.com", "admin", "admin");
    private static final AuthUser USER = new AuthUser("u1", "user@example.com", "user", "user");

    private final JwtService jwt = new JwtService(SECRET, SECRET + "_refresh", "15m", "7d");
    private final BanList banList = new BanList(null);
    private final AccessTokenReader tokens = new AccessTokenReader(jwt, banList);
    private final AuthHandshakeInterceptor adminChannel = new AuthHandshakeInterceptor(jwt, banList, tokens, true);
    private final AuthHandshakeInterceptor userChannel = new AuthHandshakeInterceptor(jwt, banList, tokens, false);

    private record Result(boolean accepted, int status, Map<String, Object> attributes) {}

    private Result handshake(String token) {
        return handshake(adminChannel, token);
    }

    private Result handshake(AuthHandshakeInterceptor interceptor, String token) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/ws/admin");
        if (token != null) req.setCookies(new Cookie("token", token));
        MockHttpServletResponse res = new MockHttpServletResponse();
        Map<String, Object> attributes = new HashMap<>();
        boolean ok = interceptor.beforeHandshake(new ServletServerHttpRequest(req), new ServletServerHttpResponse(res),
                null, attributes);
        return new Result(ok, res.getStatus(), attributes);
    }

    @Test
    void acceptsAdminAndRemembersUserAndTokenExpiry() {
        Result r = handshake(jwt.generateToken(ADMIN));
        assertTrue(r.accepted());
        assertEquals(ADMIN.id(), ((AuthUser) r.attributes().get(AuthSockets.USER_ATTR)).id());
        assertTrue((Long) r.attributes().get(AuthSockets.EXPIRES_ATTR) > System.currentTimeMillis());
    }

    @Test
    void rejectsMissingOrForgedToken() {
        assertEquals(401, handshake(null).status());
        assertFalse(handshake(null).accepted());

        // Ky bang secret khac -> sai chu ky
        JwtService other = new JwtService(SECRET + "-other", SECRET + "_refresh", "15m", "7d");
        Result forged = handshake(other.generateToken(ADMIN));
        assertFalse(forged.accepted());
        assertEquals(401, forged.status());
    }

    @Test
    void rejectsNonAdmin() {
        Result r = handshake(jwt.generateToken(USER));
        assertFalse(r.accepted());
        assertEquals(403, r.status());
    }

    @Test
    void userChannelAcceptsAnyLoggedInUserButNotGuestsOrBanned() {
        Result ok = handshake(userChannel, jwt.generateToken(USER));
        assertTrue(ok.accepted());
        assertEquals(USER.id(), ((AuthUser) ok.attributes().get(AuthSockets.USER_ATTR)).id());

        assertEquals(401, handshake(userChannel, null).status());

        banList.set(USER.id(), new Date(System.currentTimeMillis() + 60_000));
        Result banned = handshake(userChannel, jwt.generateToken(USER));
        assertFalse(banned.accepted());
        assertEquals(403, banned.status());
    }

    @Test
    void rejectsBannedAdmin() {
        banList.set(ADMIN.id(), new Date(System.currentTimeMillis() + 60_000));
        Result r = handshake(jwt.generateToken(ADMIN));
        assertFalse(r.accepted());
        assertEquals(403, r.status());
    }
}
