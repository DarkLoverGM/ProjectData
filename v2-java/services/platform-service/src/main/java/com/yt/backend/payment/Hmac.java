package com.yt.backend.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/** Ky / kiem tra chu ky HMAC dang hex ma MoMo (HmacSHA256) va VNPay (HmacSHA512) dung. */
final class Hmac {

    private Hmac() {}

    static String sha256(String key, String data) {
        return hex("HmacSHA256", key, data);
    }

    static String sha512(String key, String data) {
        return hex("HmacSHA512", key, data);
    }

    private static String hex(String algorithm, String key, String data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** So 2 chu ky hex, khong phan biet hoa/thuong, thoi gian so sanh khong doi (chong timing attack) */
    static boolean matches(String expected, String actual) {
        if (expected == null || actual == null) return false;
        return MessageDigest.isEqual(
                expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                actual.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }
}
