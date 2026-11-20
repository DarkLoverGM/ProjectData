package com.yt.backend.security;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.auth.BanList;
import com.yt.backend.auth.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Doc JWT cua moi request va gan user vao SecurityContext (controller lay bang @AuthenticationPrincipal AuthUser).
 * - Khong co token: di tiep nhu khach, endpoint can dang nhap se bi RestAuthenticationEntryPoint chan.
 * - Token het han / sai: tra 401 TOKEN_EXPIRED / TOKEN_INVALID ngay, frontend refresh token roi gui lai.
 *   (Tru cac route tu xu ly phien dang nhap: login, register, refresh, logout, Google, cong thanh toan goi ve)
 * - Tai khoan bi admin khoa (BanList): coi nhu khach; endpoint can dang nhap tra 403 ACCOUNT_BANNED.
 *   Nho BanList nam trong RAM nen nguoi bi khoa bi chan ngay ca khi access token con han.
 * Khong phai @Component: chi chay trong chuoi filter cua Spring Security (SecurityConfig tao).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> SESSION_PATHS = List.of(
            "/api/user/login", "/api/user/logout", "/api/user/register", "/api/user/token-refresh",
            "/api/auth/google", "/api/auth/firebase", "/api/payments/momo/", "/api/payments/vnpay/");

    private final JwtService jwt;
    private final BanList banList;
    private final AccessTokenReader tokens;
    private final SecurityErrorWriter errors;

    public JwtAuthenticationFilter(JwtService jwt, BanList banList, AccessTokenReader tokens, SecurityErrorWriter errors) {
        this.jwt = jwt;
        this.banList = banList;
        this.tokens = tokens;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String token = tokens.read(req);
        if (token == null) {
            chain.doFilter(req, res);
            return;
        }

        AuthError error = null;
        try {
            AuthUser user = jwt.verifyAccess(token);
            if (banList.isBanned(user.id())) {
                req.setAttribute(AuthError.ATTR, AuthError.ACCOUNT_BANNED);
            } else {
                String role = "ROLE_" + String.valueOf(user.role()).toUpperCase(Locale.ROOT);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority(role))));
            }
        } catch (ExpiredJwtException e) {
            error = AuthError.TOKEN_EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            error = AuthError.TOKEN_INVALID;
        }

        if (error != null) {
            String uri = req.getRequestURI();
            boolean api = uri.startsWith("/api/");
            if (api && SESSION_PATHS.stream().noneMatch(uri::startsWith)) {
                errors.write(res, error);
                return;
            }
            req.setAttribute(AuthError.ATTR, error);
        }
        chain.doFilter(req, res);
    }
}
