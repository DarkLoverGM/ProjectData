package com.yt.backend.controller;

import com.yt.backend.auth.AuthCookies;
import com.yt.backend.auth.BanList;
import com.yt.backend.auth.JwtService;
import com.yt.backend.common.ApiException;
import com.yt.backend.common.ImageUpload;
import com.yt.backend.config.RateLimits;
import com.yt.backend.dto.ErrorResponse;
import com.yt.backend.dto.MessageResponse;
import com.yt.backend.dto.auth.*;
import com.yt.backend.entity.User;
import com.yt.backend.security.AccessTokenReader;
import com.yt.backend.service.AuthService;
import com.yt.backend.service.CaptchaService;
import com.yt.backend.service.MailService;
import com.yt.backend.websocket.AdminEvent;
import com.yt.backend.websocket.AdminRealtime;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.function.Predicate;

/** Dang nhap / dang ky / refresh token / dang xuat (controllers/user/authController.js) */
@RestController
@RequestMapping("/api/user")
public class UserAuthController {

    private static final Logger log = LoggerFactory.getLogger(UserAuthController.class);

    private final AuthService authService;
    private final AuthCookies cookies;
    private final JwtService jwt;
    private final CaptchaService captchaService;
    private final MailService mailService;
    private final ImageUpload imageUpload;
    private final RateLimits rateLimits;
    private final AdminRealtime adminRealtime;

    public UserAuthController(AuthService authService, AuthCookies cookies, JwtService jwt,
                              CaptchaService captchaService, MailService mailService, ImageUpload imageUpload,
                              RateLimits rateLimits, AdminRealtime adminRealtime) {
        this.authService = authService;
        this.cookies = cookies;
        this.jwt = jwt;
        this.captchaService = captchaService;
        this.mailService = mailService;
        this.imageUpload = imageUpload;
        this.rateLimits = rateLimits;
        this.adminRealtime = adminRealtime;
    }

    /** Sai mat khau qua nhieu lan theo email -> 429 (RateLimits.passwordEmail) */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest body, HttpServletResponse res) {
        return rateLimits.byEmail(rateLimits.passwordEmail, body.email(), res, () -> {
            User user = authService.login(body.email(), body.password());
            TokenExpiry expiry = cookies.set(res, AuthService.toAuthUser(user));
            return ResponseEntity.ok(LoginResponse.of(expiry, UserSummary.from(user)));
        });
    }

    /** Gui ma xac minh email truoc khi dang ky. Bat buoc qua captcha de khong bi dung de spam email nguoi khac. */
    @PostMapping("/register/send-code")
    public ResponseEntity<?> sendRegisterCode(@Valid @RequestBody SendCodeRequest body, HttpServletResponse res) {
        return rateLimits.byEmail(rateLimits.otpSendEmail, body.email(), res, () -> {
            if (!captchaService.verifyCaptcha(body.captchaToken(), body.captchaInput())) {
                throw new ApiException(400, ErrorResponse.of("CAPTCHA_INVALID", "CAPTCHA sai hoặc đã hết hạn"));
            }
            String code = authService.createRegisterCode(body.email());
            try {
                mailService.sendRegisterCodeEmail(body.email(), code);
            } catch (Exception e) {
                log.error("Send register code error", e);
                authService.discardRegisterCode(body.email());
                throw new ApiException(500, "Không gửi được mã xác minh, vui lòng thử lại");
            }
            return ResponseEntity.ok(MessageResponse.ok("Đã gửi mã xác minh tới email"));
        });
    }

    /** multipart/form-data: username, email, password, code + `avatar` (khong bat buoc, khong co thi dung anh mac dinh) */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @ModelAttribute RegisterRequest body, HttpServletRequest req) {
        ImageUpload.Saved images = imageUpload.save(req, "avatar");
        try {
            String uploaded = images.store("avatar");
            UserSummary user = authService.register(body, uploaded != null ? uploaded : ImageUpload.DEFAULT_AVATAR);
            adminRealtime.publish(new AdminEvent.UserNew(user.id(), user.username()));
            return ResponseEntity.status(201).body(new RegisterResponse(true, "Đăng ký thành công", user));
        } catch (RuntimeException e) {
            images.removeAll();
            throw e;
        }
    }

    /** Doi refresh token (cookie `refreshToken`) lay cap token moi. Refresh token cu bi thay luon. */
    @PostMapping("/token-refresh")
    public TokenRefreshResponse tokenRefresh(HttpServletRequest req, HttpServletResponse res) {
        String userId = jwt.verifyRefresh(AccessTokenReader.readCookie(req, "refreshToken"));
        // Lay lai tu DB: user bi xoa thi khong refresh duoc, doi role co hieu luc ngay
        User user = authService.findActiveUser(userId);
        if (user == null) {
            cookies.clear(res);
            throw new ApiException(401, ErrorResponse.of("REFRESH_INVALID", userId == null
                    ? "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại" : "Không xác thực được người dùng"));
        }
        // 401 (khong phai 403) de frontend coi nhu het phien va dang xuat
        BanList.Ban ban = BanList.activeBan(user);
        if (ban != null) {
            cookies.clear(res);
            throw new ApiException(401, ErrorResponse.of("ACCOUNT_BANNED", BanList.banMessage(ban)));
        }
        return TokenRefreshResponse.of(cookies.set(res, AuthService.toAuthUser(user)));
    }

    /** Chi tra han dung cua token trong cookie cua chinh nguoi goi (de test) */
    @GetMapping("/token-status")
    public TokenStatusResponse tokenStatus(HttpServletRequest req) {
        return new TokenStatusResponse(System.currentTimeMillis(),
                describe(AccessTokenReader.readCookie(req, "token"), jwt::isAccessValid),
                describe(AccessTokenReader.readCookie(req, "refreshToken"), t -> jwt.verifyRefresh(t) != null),
                new TokenStatusResponse.Config(jwt.accessExpires(), jwt.refreshExpires()));
    }

    private TokenStatusResponse.TokenInfo describe(String token, Predicate<String> verify) {
        if (token == null || token.isEmpty()) return new TokenStatusResponse.TokenInfo(false, null, null);
        return new TokenStatusResponse.TokenInfo(true, verify.test(token), jwt.expiresAtOf(token));
    }

    @PostMapping("/logout")
    public MessageResponse logout(HttpServletResponse res) {
        cookies.clear(res);
        return MessageResponse.ok("Đăng xuất thành công");
    }
}
