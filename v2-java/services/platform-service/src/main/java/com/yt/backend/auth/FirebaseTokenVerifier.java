package com.yt.backend.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yt.backend.common.FirebaseCredentials;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import io.jsonwebtoken.ProtectedHeader;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Xac minh Firebase ID token (frontend lay bang signInWithPopup), thay cho getAuth().verifyIdToken() cua firebase-admin.
 * Theo tai lieu Firebase "Verify ID tokens using a third-party JWT library":
 *   - ky RS256 bang 1 trong cac public key cua securetoken@system.gserviceaccount.com (chon theo `kid`)
 *   - aud = project id, iss = https://securetoken.google.com/<project id>, sub khong rong
 *   - exp chua qua, iat / auth_time khong o tuong lai
 * Project id lay tu service account (FirebaseCredentials), phai cung project voi REACT_APP_FIREBASE_PROJECT_ID.
 */
@Component
public class FirebaseTokenVerifier {

    private static final String CERTS_URL =
            "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";
    private static final Pattern MAX_AGE = Pattern.compile("max-age=(\\d+)");
    // Dong ho may khach / Google lech nhau vai giay
    private static final long CLOCK_SKEW_SECONDS = 60;

    private final FirebaseCredentials credentials;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private Map<String, PublicKey> keys = Map.of();
    private long keysExpiresAt;

    public FirebaseTokenVerifier(FirebaseCredentials credentials, ObjectMapper mapper) {
        this.credentials = credentials;
        this.mapper = mapper;
    }

    public boolean isConfigured() {
        return credentials.isConfigured();
    }

    /**
     * @return claims cua token hop le
     * @throws JwtException token sai / het han / khac project
     * @throws IOException  khong tai duoc public key cua Google
     */
    public Claims verify(String idToken) throws IOException {
        String projectId = credentials.projectId();
        Map<String, PublicKey> current = publicKeys();
        Claims claims = Jwts.parser()
                .keyLocator(new LocatorAdapter<Key>() {
                    @Override
                    protected Key locate(ProtectedHeader header) {
                        if (!"RS256".equals(header.getAlgorithm())) throw new JwtException("Sai thuật toán ký: " + header.getAlgorithm());
                        Key key = header.getKeyId() == null ? null : current.get(header.getKeyId());
                        if (key == null) throw new JwtException("Không tìm thấy public key cho kid " + header.getKeyId());
                        return key;
                    }
                })
                .requireIssuer("https://securetoken.google.com/" + projectId)
                .requireAudience(projectId)
                .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                .build()
                .parseSignedClaims(idToken)
                .getPayload();

        long now = System.currentTimeMillis() / 1000 + CLOCK_SKEW_SECONDS;
        if (claims.getSubject() == null || claims.getSubject().isEmpty()) throw new JwtException("Thiếu sub");
        if (claims.getIssuedAt() == null || claims.getIssuedAt().getTime() / 1000 > now) throw new JwtException("iat không hợp lệ");
        Number authTime = claims.get("auth_time", Number.class);
        if (authTime == null || authTime.longValue() > now) throw new JwtException("auth_time không hợp lệ");
        return claims;
    }

    /** Public key doi vai gio 1 lan, giu lai theo Cache-Control max-age cua Google */
    private synchronized Map<String, PublicKey> publicKeys() throws IOException {
        long now = System.currentTimeMillis();
        if (now < keysExpiresAt) return keys;

        HttpResponse<String> res;
        try {
            res = http.send(HttpRequest.newBuilder(URI.create(CERTS_URL)).timeout(Duration.ofSeconds(15)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
        if (res.statusCode() / 100 != 2) throw new IOException("Firebase public key error: HTTP " + res.statusCode());

        Map<String, PublicKey> parsed = new HashMap<>();
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            JsonNode json = mapper.readTree(res.body());
            for (var it = json.fields(); it.hasNext(); ) {
                var e = it.next();
                byte[] pem = e.getValue().asText().getBytes(StandardCharsets.UTF_8);
                parsed.put(e.getKey(), factory.generateCertificate(new ByteArrayInputStream(pem)).getPublicKey());
            }
        } catch (Exception e) {
            throw new IOException("Không đọc được public key Firebase", e);
        }

        long maxAge = res.headers().firstValue("Cache-Control")
                .map(MAX_AGE::matcher).filter(Matcher::find).map(m -> Long.parseLong(m.group(1))).orElse(3600L);
        keys = Map.copyOf(parsed);
        keysExpiresAt = now + maxAge * 1000;
        return keys;
    }
}
