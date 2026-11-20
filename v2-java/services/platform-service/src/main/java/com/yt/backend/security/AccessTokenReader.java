package com.yt.backend.security;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.auth.BanList;
import com.yt.backend.auth.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Doc access token cua request: header "Authorization: Bearer ..." hoac cookie `token` (frontend dung cookie).
 * Dung o JwtAuthenticationFilter va RateLimits (rate limit chay truoc Spring Security nen can tu doc).
 */
@Component
public class AccessTokenReader {

    private static final String PEEK_ATTR = AccessTokenReader.class.getName() + ".peek";
    private static final Object NO_USER = new Object();

    private final JwtService jwt;
    private final BanList banList;

    public AccessTokenReader(JwtService jwt, BanList banList) {
        this.jwt = jwt;
        this.banList = banList;
    }

    public static String readCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    /** Token tho, khong co thi null */
    public String read(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        String token = readCookie(req, "token");
        return token == null || token.isEmpty() ? null : token;
    }

    /**
     * User cua token ma khong chan request (token hong / het han / bi khoa -> null).
     * Ket qua duoc nho trong request de cac limiter khong phai giai ma lai.
     */
    public AuthUser peek(HttpServletRequest req) {
        Object cached = req.getAttribute(PEEK_ATTR);
        if (cached != null) return cached == NO_USER ? null : (AuthUser) cached;
        AuthUser user = null;
        String token = read(req);
        if (token != null) {
            try {
                user = jwt.verifyAccess(token);
                if (banList.isBanned(user.id())) user = null;
            } catch (JwtException | IllegalArgumentException e) {
                user = null;
            }
        }
        req.setAttribute(PEEK_ATTR, user == null ? NO_USER : user);
        return user;
    }
}
