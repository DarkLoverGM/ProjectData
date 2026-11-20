package com.yt.backend.security;

/**
 * Ly do khong xac thuc duoc request. Frontend (src/utils/authFetch.js) dua vao `code` de biet
 * luc nao goi /api/user/token-refresh roi gui lai request.
 */
public enum AuthError {
    NO_TOKEN(401, "Không có token"),
    TOKEN_EXPIRED(401, "Token đã hết hạn"),
    TOKEN_INVALID(401, "Token không hợp lệ"),
    ACCOUNT_BANNED(403, "Tài khoản đã bị khoá"),
    FORBIDDEN(403, "Chỉ admin mới được truy cập");

    /** Ten attribute cua request luu loi (JwtAuthenticationFilter ghi, RestAuthenticationEntryPoint doc) */
    public static final String ATTR = AuthError.class.getName();

    public final int status;
    public final String message;

    AuthError(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
