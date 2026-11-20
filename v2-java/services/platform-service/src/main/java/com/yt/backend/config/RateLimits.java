package com.yt.backend.config;

import com.yt.backend.security.AccessTokenReader;
import com.yt.backend.auth.AuthUser;
import com.yt.backend.auth.DeviceIds;
import com.yt.backend.common.ApiException;
import com.yt.backend.common.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Gioi han so request de client loi / spam khong lam server qua tai (middleware/rateLimit.js).
 * Dem trong RAM (RateLimiter) nen chi dung khi chay 1 process.
 *
 * Dem theo gi (xem nginx/README.md):
 *  - byIdentity: user da dang nhap -> user id; khach -> device id (DeviceIds); chua co device id -> IP.
 *    Vuot muc thi bat giai captcha (CAPTCHA_REQUIRED) chu khong chan han, vi device id/IP co the dung chung.
 *  - byIp: tran chung, muc cao hon nhieu, chan han. Chong doi device id lien tuc de ne gioi han.
 *  - byEmail: dang nhap / OTP theo email dich, chan han. Doi may, doi IP cung khong ne duoc.
 *
 * apiLimiter / authLimiter / uploadLimiter / captchaLimiter / deviceIssue chay trong RateLimitFilter;
 * 3 limiter theo email can body nen controller goi qua byEmail(...).
 */
@Component
public class RateLimits {

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;
    private static final String TOO_MANY = "TOO_MANY_REQUESTS";
    private static final String CAPTCHA_REQUIRED = "CAPTCHA_REQUIRED";
    private static final String API_MESSAGE = "Bạn gửi quá nhiều yêu cầu, vui lòng thử lại sau ít phút";
    private static final String AUTH_MESSAGE = "Thử quá nhiều lần, vui lòng đợi 60 phút rồi thử lại";
    private static final String UPLOAD_MESSAGE = "Bạn tải lên quá nhiều, vui lòng thử lại sau";
    private static final String CAPTCHA_MESSAGE = "Bạn thao tác quá nhanh, vui lòng xác nhận captcha để tiếp tục";

    // Lay / giai captcha khong bi apiIdentity tinh (khong thi het luot la khong giai duoc),
    // thay vao do co gioi han rieng captcha
    public static final List<String> CAPTCHA_PATHS = List.of("/api/user/create-captcha", "/api/user/captcha/solve");

    static final List<String> AUTH_PATHS = List.of(
            "/api/user/login",
            "/api/user/register",        // gom ca /register/send-code
            "/api/user/forgot-password", // request / verify / resend-otp / reset
            "/api/user/find-user",
            "/api/user/change-password",
            "/api/user/verify-captcha",
            "/api/auth/firebase");
    static final List<String> UPLOAD_PATHS = List.of("/api/video/upload", "/api/videos/upload", "/api/studio/videos");

    private final AccessTokenReader auth;

    /* ---------- Toan bo /api ---------- */
    // 300 request / phut / nguoi (vuot -> captcha), 3000 / phut / IP (vuot -> chan)
    final RateLimiter apiIp;
    final RateLimiter apiIdentity;
    // Lay + giai captcha: 20 lan / 10 phut / nguoi
    final RateLimiter captcha;

    /* ---------- Dang nhap, dang ky, OTP, quen mat khau ---------- */
    // 5 lan / 60 phut / nguoi (vuot -> captcha), 50 / 60 phut / IP (vuot -> chan)
    final RateLimiter authIp;
    final RateLimiter authIdentity;

    /* ---------- Upload / sua video ---------- */
    // 20 lan / gio / nguoi, 200 / gio / IP, bo qua GET (chi doc du lieu)
    final RateLimiter uploadIp;
    final RateLimiter uploadIdentity;

    /* ---------- Cap device id moi ---------- */
    // 30 id / gio / IP. May da co id hop le goi lai thi khong bi tinh.
    final RateLimiter deviceIssue;

    /* ---------- Theo email (goi o tung controller) ---------- */
    // Sai mat khau (login, change-password): 10 lan / gio / email, dang nhap dung khong bi tinh
    public final RateLimiter passwordEmail;
    // Gui OTP (dang ky + quen mat khau dung chung): 5 mail / gio / email, gui loi khong bi tinh
    public final RateLimiter otpSendEmail;
    // Nhap sai OTP quen mat khau: 5 lan / 15 phut / email
    public final RateLimiter otpVerifyEmail;

    public RateLimits(AccessTokenReader auth) {
        this.auth = auth;
        Function<HttpServletRequest, String> byIdentity = this::identityKey;
        Function<HttpServletRequest, String> byIp = ClientIp::of;
        Predicate<HttpServletRequest> none = r -> false;
        Predicate<HttpServletRequest> skipGet = r -> "GET".equals(r.getMethod());

        apiIp = new RateLimiter(MINUTE, 3000, TOO_MANY, API_MESSAGE, byIp, none, false, false);
        apiIdentity = new RateLimiter(MINUTE, 300, CAPTCHA_REQUIRED, CAPTCHA_MESSAGE, byIdentity,
                r -> CAPTCHA_PATHS.contains(r.getRequestURI()), false, false);
        captcha = new RateLimiter(10 * MINUTE, 20, TOO_MANY, API_MESSAGE, byIdentity, none, false, false);

        authIp = new RateLimiter(HOUR, 50, TOO_MANY, AUTH_MESSAGE, byIp, none, false, false);
        authIdentity = new RateLimiter(HOUR, 5, CAPTCHA_REQUIRED, CAPTCHA_MESSAGE, byIdentity, none, false, false);

        uploadIp = new RateLimiter(HOUR, 200, TOO_MANY, UPLOAD_MESSAGE, byIp, skipGet, false, false);
        uploadIdentity = new RateLimiter(HOUR, 20, TOO_MANY, UPLOAD_MESSAGE, byIdentity, skipGet, false, false);

        deviceIssue = new RateLimiter(HOUR, 30, TOO_MANY, "Quá nhiều thiết bị mới từ mạng này, vui lòng thử lại sau",
                byIp, r -> DeviceIds.of(r) != null, false, false);

        passwordEmail = new RateLimiter(HOUR, 10, TOO_MANY,
                "Tài khoản này nhập sai mật khẩu quá nhiều lần, vui lòng thử lại sau 1 giờ", null, none, true, false);
        otpSendEmail = new RateLimiter(HOUR, 5, TOO_MANY,
                "Email này đã nhận quá nhiều mã, vui lòng thử lại sau 1 giờ", null, none, false, true);
        otpVerifyEmail = new RateLimiter(15 * MINUTE, 5, TOO_MANY,
                "Nhập sai mã quá nhiều lần, vui lòng đợi 15 phút rồi thử lại", null, none, true, false);
    }

    /** User da dang nhap -> user id; khach -> device id; chua co device id -> IP */
    String identityKey(HttpServletRequest req) {
        AuthUser user = auth.peek(req);
        if (user != null) return "u:" + user.id();
        String deviceId = DeviceIds.of(req);
        return deviceId != null ? "d:" + deviceId : "ip:" + ClientIp.of(req);
    }

    /** Giai captcha dung -> xoa luot da dung cua nguoi nay o cac limiter tra CAPTCHA_REQUIRED */
    public void resetCaptchaLimits(HttpServletRequest req) {
        String key = identityKey(req);
        apiIdentity.resetKey(key);
        authIdentity.resetKey(key);
    }

    /**
     * Chay handler duoi limiter theo email (gan o tung route sau khi da co body.email).
     * Khong co email thi bo qua limiter. skipSuccessful/skipFailed tinh theo status cua response.
     */
    public ResponseEntity<?> byEmail(RateLimiter limiter, String email, HttpServletResponse res,
                                     Supplier<ResponseEntity<?>> handler) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return handler.get();

        String key = "e:" + normalized;
        RateLimiter.Hit hit = limiter.hit(key);
        RateLimiter.setHeaders(res, hit);
        if (hit.blocked()) {
            limiter.release(key, 429);
            return ResponseEntity.status(429).body(limiter.body());
        }
        int status = 500;
        try {
            ResponseEntity<?> result = handler.get();
            status = result.getStatusCode().value();
            return result;
        } catch (ApiException e) {
            status = e.getStatus();
            throw e;
        } finally {
            limiter.release(key, status);
        }
    }
}
