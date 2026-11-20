package com.yt.backend.dto.auth;

import com.yt.backend.dto.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank(message = "Vui lòng nhập email") String email,
        @NotBlank(message = "Thiếu mật khẩu mới")
        @Size(min = ValidationPatterns.PASSWORD_MIN, max = ValidationPatterns.PASSWORD_MAX,
                message = ValidationPatterns.PASSWORD_MESSAGE) String newPassword) {}
