package com.yt.backend.dto.auth;

import com.yt.backend.dto.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Gui ma xac minh email truoc khi dang ky, bat buoc qua captcha */
public record SendCodeRequest(
        @NotBlank(message = "Email không hợp lệ")
        @Pattern(regexp = ValidationPatterns.EMAIL, message = "Email không hợp lệ") String email,
        String captchaToken,
        String captchaInput) {}
