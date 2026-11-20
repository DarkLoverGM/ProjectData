package com.yt.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpVerifyRequest(
        @NotBlank(message = "Vui lòng nhập email") String email,
        @NotBlank(message = "Vui lòng nhập mã OTP")
        @Pattern(regexp = "^\\d{6}$", message = "OTP gồm 6 chữ số") String otp) {}
