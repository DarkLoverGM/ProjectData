package com.yt.backend.dto.auth;

import com.yt.backend.dto.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailRequest(
        @NotBlank(message = "Vui lòng nhập email")
        @Pattern(regexp = ValidationPatterns.EMAIL, message = "Email không hợp lệ") String email) {}
