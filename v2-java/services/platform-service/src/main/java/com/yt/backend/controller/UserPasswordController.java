package com.yt.backend.controller;

import com.yt.backend.common.ApiException;
import com.yt.backend.config.RateLimits;
import com.yt.backend.dto.MessageResponse;
import com.yt.backend.dto.auth.EmailRequest;
import com.yt.backend.dto.auth.OtpVerifyRequest;
import com.yt.backend.dto.auth.PasswordChangeRequest;
import com.yt.backend.dto.auth.PasswordResetRequest;
import com.yt.backend.entity.User;
import com.yt.backend.service.MailService;
import com.yt.backend.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Doi mat khau + quen mat khau qua OTP email (controllers/user/passwordController.js) */
@RestController
@RequestMapping("/api/user")
public class UserPasswordController {

    private static final Logger log = LoggerFactory.getLogger(UserPasswordController.class);
    /**
     * Tra cung 1 thong bao du email co ton tai hay khong, de nguoi la khong dung form nay
     * do xem email nao da dang ky (user enumeration).
     */
    private static final String OTP_SENT = "Nếu email đã đăng ký, mã OTP đã được gửi tới email";

    private final UserService userService;
    private final MailService mailService;
    private final RateLimits rateLimits;

    public UserPasswordController(UserService userService, MailService mailService, RateLimits rateLimits) {
        this.userService = userService;
        this.mailService = mailService;
        this.rateLimits = rateLimits;
    }

    /** Doi mat khau bang email + mat khau cu. Sai mat khau qua nhieu lan theo email -> 429 */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest body, HttpServletResponse res) {
        return rateLimits.byEmail(rateLimits.passwordEmail, body.email(), res, () -> {
            User user = userService.findUserByEmail(body.email());
            if (user == null) throw ApiException.notFound("Người dùng không tồn tại");
            userService.changePassword(user.getId(), body.oldPassword(), body.newPassword());
            return ResponseEntity.ok(MessageResponse.ok("Đổi mật khẩu thành công"));
        });
    }

    /** Buoc 1 quen mat khau: gui OTP. Gui qua nhieu lan toi 1 email -> 429 (RateLimits.otpSendEmail) */
    @PostMapping({"/find-user", "/forgot-password/request"})
    public ResponseEntity<?> requestOtp(@Valid @RequestBody EmailRequest body, HttpServletResponse res) {
        return rateLimits.byEmail(rateLimits.otpSendEmail, body.email(), res, () -> sendOtp(body.email(), false));
    }

    @PostMapping("/forgot-password/resend-otp")
    public ResponseEntity<?> resendOtp(@Valid @RequestBody EmailRequest body, HttpServletResponse res) {
        return rateLimits.byEmail(rateLimits.otpSendEmail, body.email(), res, () -> sendOtp(body.email(), true));
    }

    private ResponseEntity<?> sendOtp(String email, boolean resend) {
        UserService.Otp otp = userService.createOTP(email);
        if (otp != null) {
            try {
                if (resend) mailService.sendOTPEmail(email, otp.code(), "Mã OTP đặt lại mật khẩu (Gửi lại)", true);
                else mailService.sendOTPEmail(email, otp.code());
            } catch (Exception e) {
                log.error("Error sending OTP", e);
                throw new ApiException(500, "Không gửi được OTP, vui lòng thử lại");
            }
        }
        return ResponseEntity.ok(MessageResponse.ok(OTP_SENT));
    }

    /** Nhap sai OTP qua nhieu lan theo email -> 429 (RateLimits.otpVerifyEmail) */
    @PostMapping("/forgot-password/verify")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerifyRequest body, HttpServletResponse res) {
        return rateLimits.byEmail(rateLimits.otpVerifyEmail, body.email(), res,
                () -> ResponseEntity.ok(userService.verifyOTP(body.email(), body.otp())));
    }

    @PostMapping("/forgot-password/reset")
    public MessageResponse resetPassword(@Valid @RequestBody PasswordResetRequest body) {
        String userId = userService.requireVerifiedOTP(body.email());
        userService.resetPassword(userId, body.newPassword());
        return MessageResponse.ok("Đặt lại mật khẩu thành công");
    }
}
