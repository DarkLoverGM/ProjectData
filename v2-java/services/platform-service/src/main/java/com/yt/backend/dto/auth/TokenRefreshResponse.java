package com.yt.backend.dto.auth;

public record TokenRefreshResponse(boolean success, String message, long accessExpiresAt, long refreshExpiresAt) {

    public static TokenRefreshResponse of(TokenExpiry expiry) {
        return new TokenRefreshResponse(true, "Làm mới token thành công", expiry.accessExpiresAt(), expiry.refreshExpiresAt());
    }
}
