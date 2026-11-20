package com.yt.backend.dto.notification;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** FCM token: chuoi base64url kem ':' / '-' / '_', dai ~150-200 ky tu */
public record PushTokenRequest(
        @NotNull(message = "Token không hợp lệ")
        @Pattern(regexp = "^[\\w:-]{20,4096}$", message = "Token không hợp lệ") String token) {}
