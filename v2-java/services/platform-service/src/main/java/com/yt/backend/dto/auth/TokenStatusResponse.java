package com.yt.backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

/** GET /api/user/token-status: han dung cua token trong cookie (trang dev/TokenStatus) */
public record TokenStatusResponse(long now, TokenInfo access, TokenInfo refresh, Config config) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TokenInfo(boolean present, Boolean valid, Long expiresAt) {}

    public record Config(String accessExpires, String refreshExpires) {}
}
