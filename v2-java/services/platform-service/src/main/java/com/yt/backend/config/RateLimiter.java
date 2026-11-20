package com.yt.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yt.backend.common.Json;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 1 bo dem kieu express-rate-limit (MemoryStore): moi key 1 cua so co dinh windowMs tinh tu lan goi dau,
 * qua `limit` lan thi tra 429 { code, error }. Dem trong RAM nen chi dung khi chay 1 process.
 * Cac limiter cu the nam o RateLimits.
 */
public final class RateLimiter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final class Counter {
        final long resetAt;
        int hits;

        Counter(long resetAt) {
            this.resetAt = resetAt;
        }
    }

    /** Ket qua 1 lan dem: so lan da goi trong cua so hien tai + thoi diem reset */
    public record Hit(RateLimiter limiter, String key, int count, long resetAt) {
        public boolean blocked() {
            return count > limiter.limit;
        }
    }

    final long windowMs;
    final int limit;
    final String code;
    final String message;
    final Function<HttpServletRequest, String> keyGenerator;
    final Predicate<HttpServletRequest> skip;
    final boolean skipSuccessfulRequests;
    final boolean skipFailedRequests;

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private volatile long nextCleanup = 0;

    RateLimiter(long windowMs, int limit, String code, String message,
                Function<HttpServletRequest, String> keyGenerator, Predicate<HttpServletRequest> skip,
                boolean skipSuccessfulRequests, boolean skipFailedRequests) {
        this.windowMs = windowMs;
        this.limit = limit;
        this.code = code;
        this.message = message;
        this.keyGenerator = keyGenerator;
        this.skip = skip;
        this.skipSuccessfulRequests = skipSuccessfulRequests;
        this.skipFailedRequests = skipFailedRequests;
    }

    /** Tinh them 1 lan cho key */
    public Hit hit(String key) {
        long now = System.currentTimeMillis();
        cleanup(now);
        Counter c = counters.compute(key, (k, old) -> {
            Counter cur = old == null || old.resetAt <= now ? new Counter(now + windowMs) : old;
            cur.hits++;
            return cur;
        });
        return new Hit(this, key, c.hits, c.resetAt);
    }

    /** Bo 1 lan da tinh (skipSuccessfulRequests / skipFailedRequests) */
    public void decrement(String key) {
        counters.computeIfPresent(key, (k, c) -> {
            c.hits = Math.max(0, c.hits - 1);
            return c;
        });
    }

    /** Xoa luot da dung cua key (vd. giai captcha dung) */
    public void resetKey(String key) {
        counters.remove(key);
    }

    /** Request xong: bo lan dem neu limiter khong tinh request thanh cong / that bai */
    public void release(String key, int status) {
        if ((skipSuccessfulRequests && status < 400) || (skipFailedRequests && status >= 400)) decrement(key);
    }

    // Cua so het han thi xoa de map khong phinh ra mai
    private void cleanup(long now) {
        if (now < nextCleanup) return;
        nextCleanup = now + Math.min(windowMs, 60_000);
        counters.values().removeIf(c -> c.resetAt <= now);
    }

    /** Header RateLimit-* (standardHeaders: 'draft-7') de client biet con bao nhieu luot */
    public static void setHeaders(HttpServletResponse res, Hit hit) {
        RateLimiter l = hit.limiter();
        long resetSec = Math.max(0, (long) Math.ceil((hit.resetAt() - System.currentTimeMillis()) / 1000.0));
        res.setHeader("RateLimit-Policy", l.limit + ";w=" + l.windowMs / 1000);
        res.setHeader("RateLimit", "limit=" + l.limit + ", remaining=" + Math.max(0, l.limit - hit.count())
                + ", reset=" + resetSec);
        if (hit.blocked()) res.setHeader("Retry-After", String.valueOf(resetSec));
    }

    /** Body 429 */
    public Map<String, Object> body() {
        return Json.of("code", code, "error", message);
    }

    /** Ghi 429 thang vao response (dung trong filter, chua qua DispatcherServlet) */
    public void reject(HttpServletResponse res) throws IOException {
        writeJson(res, 429, body());
    }

    static void writeJson(HttpServletResponse res, int status, Map<String, Object> body) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json; charset=utf-8");
        res.getOutputStream().write(MAPPER.writeValueAsString(body).getBytes(StandardCharsets.UTF_8));
    }
}
