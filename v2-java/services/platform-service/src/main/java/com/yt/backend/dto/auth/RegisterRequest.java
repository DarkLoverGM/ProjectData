package com.yt.backend.dto.auth;

import com.yt.backend.dto.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Form dang ky (multipart/form-data, anh dai dien gui o field `avatar` rieng) */
public record RegisterRequest(
        @NotBlank(message = "Thiếu thông tin bắt buộc")
        @Size(max = 50, message = "Username tối đa 50 ký tự") String username,
        @NotBlank(message = "Thiếu thông tin bắt buộc")
        @Pattern(regexp = ValidationPatterns.EMAIL, message = "Email không hợp lệ") String email,
        @NotBlank(message = "Thiếu thông tin bắt buộc")
        @Size(min = ValidationPatterns.PASSWORD_MIN, max = ValidationPatterns.PASSWORD_MAX,
                message = ValidationPatterns.PASSWORD_MESSAGE) String password,
        @NotBlank(message = "Thiếu thông tin bắt buộc") String code) {}
