package com.yt.backend.config;

import com.yt.backend.common.Json;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gioi han so request /api duoc xu ly CUNG LUC tren toan server (middleware/loadShedding.js).
 * Rate limit theo nguoi/may khong cuu duoc khi bi goi don tu rat nhieu noi; qua muc nay thi
 * tra 503 ngay thay vi de request xep hang giu RAM (body, ket noi DB...) den khi server sap.
 * Chay sau RateLimitFilter: request bi chan 429 tra ve ngay, khong chiem cho cua request that.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class LoadSheddingFilter extends OncePerRequestFilter {

    private final int maxInFlight;
    private final AtomicInteger inFlight = new AtomicInteger();

    public LoadSheddingFilter(@Value("${app.max-in-flight}") String maxInFlight) {
        // Number(process.env.MAX_IN_FLIGHT) || 100
        int n = Json.parseIntLoose(maxInFlight, 100);
        this.maxInFlight = n > 0 ? n : 100;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !RateLimitFilter.under(req.getRequestURI(), "/api");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (inFlight.incrementAndGet() > maxInFlight) {
            inFlight.decrementAndGet();
            res.setHeader("Retry-After", "5");
            RateLimiter.writeJson(res, 503, Json.of("code", "SERVER_BUSY",
                    "error", "Server đang quá tải, vui lòng thử lại sau vài giây"));
            return;
        }
        try {
            chain.doFilter(req, res);
        } finally {
            inFlight.decrementAndGet();
        }
    }
}
