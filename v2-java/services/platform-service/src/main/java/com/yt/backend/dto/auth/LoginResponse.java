package com.yt.backend.dto.auth;

public record LoginResponse(boolean success, String message, long accessExpiresAt, long refreshExpiresAt,
                            UserSummary user) {

    public static LoginResponse of(TokenExpiry expiry, UserSummary user) {
        return new LoginResponse(true, "Đăng nhập thành công", expiry.accessExpiresAt(), expiry.refreshExpiresAt(), user);
    }
}
