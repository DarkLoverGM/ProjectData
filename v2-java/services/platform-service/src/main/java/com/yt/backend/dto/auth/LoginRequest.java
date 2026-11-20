package com.yt.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Thiếu email hoặc mật khẩu") String email,
        @NotBlank(message = "Thiếu email hoặc mật khẩu") String password) {}
