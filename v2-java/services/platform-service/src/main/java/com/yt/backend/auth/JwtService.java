package com.yt.backend.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ky / kiem tra JWT giong jsonwebtoken ben Node (HS256, secret dang chuoi),
 * nen token cua 2 server dung chung duoc.
 */
@Component
public class JwtService {

    private static final Pattern DURATION = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)\\s*(ms|s|m|h|d|w|y)?\\s*$");

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final String accessExpires;
    private final String refreshExpires;
    private final long accessMs;
    private final long refreshMs;
    private final ObjectMapper mapper = new ObjectMapper();

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.refresh-secret}") String refreshSecret,
            @Value("${app.jwt.access-expires}") String accessExpires,
            @Value("${app.jwt.refresh-expires}") String refreshExpires) {
        this.accessKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessExpires = accessExpires;
        this.refreshExpires = refreshExpires;
        this.accessMs = parseDuration(accessExpires);
        this.refreshMs = parseDuration(refreshExpires);
    }

    /** "15m", "7d", "30s" -> mili giay, giong thu vien ms (khong co don vi = ms) */
    static long parseDuration(String value) {
        Matcher m = DURATION.matcher(value);
        if (!m.matches()) throw new IllegalArgumentException("Thoi han token khong hop le: " + value);
        double n = Double.parseDouble(m.group(1));
        String unit = m.group(2) == null ? "ms" : m.group(2);
        long factor = switch (unit) {
            case "ms" -> 1L;
            case "s" -> 1000L;
            case "m" -> 60_000L;
            case "h" -> 3_600_000L;
            case "d" -> 86_400_000L;
            case "w" -> 604_800_000L;
            default -> 31_557_600_000L; // y
        };
        return (long) (n * factor);
    }

    public String accessExpires() {
        return accessExpires;
    }

    public String refreshExpires() {
        return refreshExpires;
    }

    public String generateToken(AuthUser user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claim("id", user.id())
                .claim("email", user.email())
                .claim("username", user.username())
                .claim("role", user.role())
                .issuedAt(new Date(now / 1000 * 1000))
                .expiration(new Date((now + accessMs) / 1000 * 1000))
                .signWith(accessKey, Jwts.SIG.HS256)
                .compact();
    }

    /** Refresh token chi chua id, thong tin con lai lay lai tu DB moi lan refresh */
    public String generateRefreshToken(AuthUser user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claim("id", user.id())
                .claim("type", "refresh")
                .issuedAt(new Date(now / 1000 * 1000))
                .expiration(new Date((now + refreshMs) / 1000 * 1000))
                .signWith(refreshKey, Jwts.SIG.HS256)
                .compact();
    }

    /** @throws ExpiredJwtException khi het han, JwtException khi sai/hong */
    public AuthUser verifyAccess(String token) {
        Claims c = Jwts.parser().verifyWith(accessKey).build().parseSignedClaims(token).getPayload();
        return new AuthUser(
                str(c.get("id")), str(c.get("email")), str(c.get("username")), str(c.get("role")));
    }

    public boolean isAccessValid(String token) {
        try {
            verifyAccess(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** @return id user neu refresh token hop le, nguoc lai null */
    public String verifyRefresh(String token) {
        if (token == null || token.isEmpty()) return null;
        try {
            Claims c = Jwts.parser().verifyWith(refreshKey).build().parseSignedClaims(token).getPayload();
            return "refresh".equals(c.get("type")) ? str(c.get("id")) : null;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /** exp * 1000 doc thang tu payload, khong kiem tra chu ky (jwt.decode ben Node) */
    public long expiresAtOf(String token) {
        try {
            String[] parts = token.split("\\.");
            JsonNode payload = mapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            return payload.path("exp").asLong(0) * 1000;
        } catch (Exception e) {
            return 0;
        }
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    public Map<String, Object> config() {
        return Map.of("accessExpires", accessExpires, "refreshExpires", refreshExpires);
    }
}
