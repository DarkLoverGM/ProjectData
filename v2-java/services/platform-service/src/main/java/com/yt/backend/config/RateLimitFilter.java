package com.yt.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Chay cac limiter theo duong dan, giong thu tu app.use(...) trong Servertest.js:
 * /api (apiLimiter) -> cac route dang nhap (authLimiter) -> upload (uploadLimiter) -> captcha (captchaLimiter),
 * rieng POST /api/device co deviceIssueLimiter. Request bi chan tra 429 ngay, khong toi controller.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimits limits;

    public RateLimitFilter(RateLimits limits) {
        this.limits = limits;
    }

    /** Giong app.use(path): khop ca duong dan con ben duoi (/a khop /a va /a/b, khong khop /ab) */
    static boolean under(String uri, String prefix) {
        return uri.equals(prefix) || uri.startsWith(prefix.endsWith("/") ? prefix : prefix + "/");
    }

    private static boolean underAny(String uri, List<String> prefixes) {
        for (String p : prefixes) if (under(uri, p)) return true;
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !under(req.getRequestURI(), "/api");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String uri = req.getRequestURI();
        List<RateLimiter> applicable = new ArrayList<>(List.of(limits.apiIp, limits.apiIdentity));
        if (underAny(uri, RateLimits.AUTH_PATHS)) {
            applicable.add(limits.authIp);
            applicable.add(limits.authIdentity);
        }
        if (underAny(uri, RateLimits.UPLOAD_PATHS)) {
            applicable.add(limits.uploadIp);
            applicable.add(limits.uploadIdentity);
        }
        if (underAny(uri, RateLimits.CAPTCHA_PATHS)) applicable.add(limits.captcha);
        if ("POST".equals(req.getMethod()) && (uri.equals("/api/device") || uri.equals("/api/device/"))) {
            applicable.add(limits.deviceIssue);
        }

        for (RateLimiter limiter : applicable) {
            if (limiter.skip.test(req)) continue;
            RateLimiter.Hit hit = limiter.hit(limiter.keyGenerator.apply(req));
            RateLimiter.setHeaders(res, hit);
            if (hit.blocked()) {
                limiter.reject(res);
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
