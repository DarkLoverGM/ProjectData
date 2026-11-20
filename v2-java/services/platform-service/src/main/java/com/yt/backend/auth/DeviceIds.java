package com.yt.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Moi trinh duyet co 1 device id rieng de rate limit dem theo tung may thay vi theo IP
 * (nhieu nguoi chung wifi = chung IP), giong middleware/deviceId.js.
 *
 * Id dang "<uuid>.<chu ky HMAC>" do server cap o POST /api/device, duoc luu 2 noi:
 *  - cookie device_id (httpOnly, trinh duyet tu gui)
 *  - localStorage, frontend gui lai qua header X-Device-Id (src/utils/deviceId.js)
 * Xoa 1 trong 2 thi noi con lai khoi phuc. Chu ky dam bao client khong tu bia id duoc;
 * so id moi moi IP xin duoc bi gioi han (deviceIssue trong RateLimits).
 */
@Component
public class DeviceIds {

    public static final String DEVICE_COOKIE = "device_id";
    public static final String DEVICE_HEADER = "X-Device-Id";
    /** Ten attribute chua device id hop le cua request (req.deviceId ben Node), null neu chua co */
    public static final String ATTR = DeviceIds.class.getName();

    private static final Duration ONE_YEAR = Duration.ofDays(365);
    private static final Pattern TOKEN_RE = Pattern.compile(
            "^([0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12})\\.([A-Za-z0-9_-]{43})$");

    private final byte[] secret;
    private final boolean secure;

    public DeviceIds(@Value("${app.cookie.secret}") String secret, @Value("${app.cookie.secure}") boolean secure) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.secure = secure;
    }

    private String sign(String id) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(id.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Tra ve token neu dung dinh dang va dung chu ky, nguoc lai null */
    public String verify(String token) {
        if (token == null) return null;
        Matcher m = TOKEN_RE.matcher(token);
        if (!m.matches()) return null;
        byte[] expected = sign(m.group(1)).getBytes(StandardCharsets.UTF_8);
        byte[] actual = m.group(2).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual) ? token : null;
    }

    public String issue() {
        String id = UUID.randomUUID().toString();
        return id + "." + sign(id);
    }

    public void setCookie(HttpServletResponse res, String token) {
        ResponseCookie cookie = ResponseCookie.from(DEVICE_COOKIE, token)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(secure)
                .path("/")
                .maxAge(ONE_YEAR)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** Device id hop le cua request (DeviceIdFilter da gan), null -> rate limit dem theo IP */
    public static String of(HttpServletRequest req) {
        Object v = req.getAttribute(ATTR);
        return v instanceof String s ? s : null;
    }
}
