package com.yt.backend.dto.user;

import com.yt.backend.dto.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Form doi thong tin (JSON hoac multipart kem anh avatar / banner).
 * Field rong / khong gui = giu nguyen. Doi mat khau khi gui ca oldPassword lan newPassword.
 */
public record UpdateProfileRequest(
        @Size(max = 50, message = "Username tối đa 50 ký tự") String username,
        @Pattern(regexp = "^$|" + ValidationPatterns.EMAIL, message = "Email không hợp lệ") String email,
        String oldPassword,
        @Size(max = ValidationPatterns.PASSWORD_MAX, message = ValidationPatterns.PASSWORD_MESSAGE) String newPassword,
        @NotBlank(message = "Thiếu mã CAPTCHA") String captcha,
        @NotBlank(message = "Thiếu mã CAPTCHA") String captchaToken) {}
