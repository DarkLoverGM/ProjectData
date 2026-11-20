package com.yt.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yt.backend.auth.AuthCookies;
import com.yt.backend.auth.BanList;
import com.yt.backend.auth.FirebaseTokenVerifier;
import com.yt.backend.common.ApiException;
import com.yt.backend.dto.ErrorResponse;
import com.yt.backend.dto.auth.LoginResponse;
import com.yt.backend.dto.auth.UserSummary;
import com.yt.backend.entity.User;
import com.yt.backend.security.AccessTokenReader;
import com.yt.backend.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dang nhap bang Google (OAuth 2.0 Authorization Code flow), controllers/user/googleAuthController.js:
 *   GET /api/auth/google          -> chuyen sang trang chon tai khoan Google
 *   GET /api/auth/google/callback -> Google goi lai, ghi cookie dang nhap roi chuyen ve frontend
 * Key nam trong server/.env: GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, GOOGLE_REDIRECT_URI, CLIENT_URL
 *
 * Dang nhap Google qua Firebase Authentication (frontend hien tai dung cach nay, src/utils/firebaseAuth.js):
 *   POST /api/auth/firebase { idToken } -> xac minh ID token, ghi cookie dang nhap, tra ve user
 * Can service account Firebase (FIREBASE_SERVICE_ACCOUNT_PATH...) va bat Google trong
 * Firebase Console > Authentication > Sign-in method.
 */
@RestController
@RequestMapping("/api/auth")
public class GoogleAuthController {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthController.class);

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";

    // Cookie `state` chong CSRF: callback chi nhan neu `state` Google tra ve khop cookie nay
    private static final String STATE_COOKIE = "google_oauth_state";
    private static final String STATE_COOKIE_PATH = "/api/auth/google";
    private static final Duration STATE_COOKIE_MAX_AGE = Duration.ofMinutes(10);

    private final AuthService authService;
    private final AuthCookies cookies;
    private final FirebaseTokenVerifier firebaseTokens;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final SecureRandom random = new SecureRandom();

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String clientUrl;

    public GoogleAuthController(AuthService authService, AuthCookies cookies, FirebaseTokenVerifier firebaseTokens,
                                ObjectMapper mapper,
                                @Value("${app.google.client-id}") String clientId,
                                @Value("${app.google.client-secret}") String clientSecret,
                                @Value("${app.google.redirect-uri}") String redirectUri,
                                @Value("${app.client-url}") String clientUrl) {
        this.authService = authService;
        this.cookies = cookies;
        this.firebaseTokens = firebaseTokens;
        this.mapper = mapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.clientUrl = clientUrl;
    }

    private static String form(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static void redirect(HttpServletResponse res, String url) throws IOException {
        res.sendRedirect(url);
    }

    // Loi thi quay ve trang login kem ?oauth_error=..., Login.js hien thong bao
    private void redirectWithError(HttpServletResponse res, String code) throws IOException {
        redirect(res, clientUrl + "/login?oauth_error=" + URLEncoder.encode(code, StandardCharsets.UTF_8));
    }

    private void setStateCookie(HttpServletResponse res, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(STATE_COOKIE, value)
                .httpOnly(true)
                .sameSite("Lax") // lax: van duoc gui khi Google redirect (GET top-level) ve callback
                .secure(cookies.isSecure())
                .path(STATE_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @GetMapping("/google")
    public void google(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (clientId.isEmpty() || redirectUri.isEmpty()) {
            log.error("Google OAuth: thiếu GOOGLE_CLIENT_ID / GOOGLE_REDIRECT_URI trong server/.env");
            redirectWithError(res, "config");
            return;
        }

        // Cookie `state` phai nam cung host voi callback. Vao bang host khac (vd. IP LAN trong khi
        // GOOGLE_REDIRECT_URI la localhost) thi chuyen sang dung host truoc, neu khong callback bao loi state
        URI callback = URI.create(redirectUri);
        String callbackOrigin = callback.getScheme() + "://" + callback.getRawAuthority();
        String requestOrigin = req.getScheme() + "://" + req.getHeader(HttpHeaders.HOST);
        if (!requestOrigin.equals(callbackOrigin)) {
            String query = req.getQueryString();
            redirect(res, callbackOrigin + req.getRequestURI() + (query == null ? "" : "?" + query));
            return;
        }

        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String state = HexFormat.of().formatHex(bytes);
        setStateCookie(res, state, STATE_COOKIE_MAX_AGE);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("client_id", clientId);
        params.put("redirect_uri", redirectUri);
        params.put("response_type", "code");
        params.put("scope", "openid email profile");
        params.put("state", state);
        params.put("prompt", "select_account");
        redirect(res, GOOGLE_AUTH_URL + "?" + form(params));
    }

    @GetMapping("/google/callback")
    public void callback(HttpServletRequest req, HttpServletResponse res,
                         @RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         @RequestParam(required = false) String error) throws IOException {
        String savedState = AccessTokenReader.readCookie(req, STATE_COOKIE);
        setStateCookie(res, "", Duration.ZERO);

        // Nguoi dung bam Huy tren trang Google
        if (error != null && !error.isEmpty()) {
            redirectWithError(res, "access_denied".equals(error) ? "cancelled" : "google");
            return;
        }
        if (code == null || code.isEmpty() || state == null || state.isEmpty()
                || savedState == null || !state.equals(savedState)) {
            redirectWithError(res, "state");
            return;
        }

        try {
            // Doi code lay access token (server-to-server, co client secret)
            Map<String, String> tokenParams = new LinkedHashMap<>();
            tokenParams.put("code", code);
            tokenParams.put("client_id", clientId);
            tokenParams.put("client_secret", clientSecret);
            tokenParams.put("redirect_uri", redirectUri);
            tokenParams.put("grant_type", "authorization_code");
            HttpResponse<String> tokenRes = http.send(HttpRequest.newBuilder(URI.create(GOOGLE_TOKEN_URL))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .timeout(Duration.ofSeconds(15))
                            .POST(HttpRequest.BodyPublishers.ofString(form(tokenParams)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode tokens = mapper.readTree(tokenRes.body());
            if (tokenRes.statusCode() / 100 != 2) {
                log.error("Google OAuth token error: {} {}", tokens.path("error").asText(), tokens.path("error_description").asText());
                redirectWithError(res, "google");
                return;
            }

            HttpResponse<String> profileRes = http.send(HttpRequest.newBuilder(URI.create(GOOGLE_USERINFO_URL))
                            .header("Authorization", "Bearer " + tokens.path("access_token").asText())
                            .timeout(Duration.ofSeconds(15))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode profile = mapper.readTree(profileRes.body());
            if (profileRes.statusCode() / 100 != 2) {
                log.error("Google OAuth userinfo error: {}", profile);
                redirectWithError(res, "google");
                return;
            }

            User user = authService.loginWithGoogle(
                    text(profile, "sub"), text(profile, "email"), profile.path("email_verified").asBoolean(false),
                    text(profile, "name"), text(profile, "picture"));
            if (user == null) {
                redirectWithError(res, "email_unverified");
                return;
            }

            if (BanList.activeBan(user) != null) {
                redirectWithError(res, "banned");
                return;
            }

            cookies.set(res, AuthService.toAuthUser(user));
            redirect(res, clientUrl + "/main");
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("Google OAuth callback error", e);
            redirectWithError(res, "server");
        }
    }

    public record FirebaseLoginRequest(String idToken) {}

    @PostMapping("/firebase")
    public LoginResponse firebase(@RequestBody(required = false) FirebaseLoginRequest body, HttpServletResponse res) {
        String idToken = body == null ? null : body.idToken();
        if (idToken == null || idToken.isEmpty()) throw ApiException.badRequest("Thiếu idToken");

        if (!firebaseTokens.isConfigured()) {
            log.error("Firebase Auth: thiếu service account (FIREBASE_SERVICE_ACCOUNT_PATH ...) trong server-java/.env");
            throw new ApiException(500, "Server chưa cấu hình đăng nhập Google");
        }

        Claims claims;
        try {
            claims = firebaseTokens.verify(idToken);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Firebase verifyIdToken error: {}", e.getMessage());
            throw new ApiException(401, "Phiên đăng nhập Google không hợp lệ, vui lòng thử lại");
        } catch (IOException e) {
            log.error("Firebase verifyIdToken error", e);
            throw new ApiException(500, "Lỗi server");
        }

        // `sub` lay Google ID goc (khong phai Firebase uid) de khop google_id cua tai khoan da lien ket
        // tu truoc khi chuyen sang Firebase
        String googleId = googleIdOf(claims);
        if (googleId == null) throw ApiException.badRequest("Chỉ hỗ trợ đăng nhập bằng tài khoản Google");

        User user = authService.loginWithGoogle(googleId, claims.get("email", String.class),
                Boolean.TRUE.equals(claims.get("email_verified", Boolean.class)),
                claims.get("name", String.class), claims.get("picture", String.class));
        if (user == null) throw ApiException.forbidden("Email Google chưa được xác minh");

        BanList.Ban ban = BanList.activeBan(user);
        if (ban != null) throw new ApiException(403, ErrorResponse.of("ACCOUNT_BANNED", BanList.banMessage(ban)));

        return LoginResponse.of(cookies.set(res, AuthService.toAuthUser(user)), UserSummary.from(user));
    }

    /** firebase.identities["google.com"][0], null neu token khong phai dang nhap bang Google */
    private static String googleIdOf(Claims claims) {
        if (!(claims.get("firebase") instanceof Map<?, ?> firebase)) return null;
        if (!"google.com".equals(firebase.get("sign_in_provider"))) return null;
        if (!(firebase.get("identities") instanceof Map<?, ?> identities)) return null;
        if (!(identities.get("google.com") instanceof List<?> ids) || ids.isEmpty()) return null;
        Object id = ids.get(0);
        return id == null || id.toString().isEmpty() ? null : id.toString();
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }
}
