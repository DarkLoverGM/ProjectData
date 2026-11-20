package com.yt.backend.dto.auth;

/** Thoi diem het han (ms) cua access token va refresh token vua cap */
public record TokenExpiry(long accessExpiresAt, long refreshExpiresAt) {}
