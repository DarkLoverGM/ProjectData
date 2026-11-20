package com.yt.backend.dto.auth;

import com.yt.backend.dto.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Doi mat khau bang email + mat khau cu (trang Quen mat khau / Doi thong tin) */
public record PasswordChangeRequest(
        @NotBlank(message = "Thiếu email") String email,
        @NotBlank(message = "Mật khẩu cũ không đúng") String oldPassword,
        @NotBlank(message = "Thiếu mật khẩu mới")
        @Size(min = ValidationPatterns.PASSWORD_MIN, max = ValidationPatterns.PASSWORD_MAX,
                message = ValidationPatterns.PASSWORD_MESSAGE) String newPassword) {}
