package com.yt.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;

/**
 * Body loi chung: { success: false, message, code? }. Field null khong ghi ra JSON.
 * code: ma de frontend xu ly rieng (CAPTCHA_INVALID, ACCOUNT_BANNED...).
 * retryAfter / bannedUntil: chi vai loi co (gui lai ma qua som, tai khoan bi khoa).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(boolean success, String code, String message, Long retryAfter, Date bannedUntil) {

    public static ErrorResponse of(String message) {
        return new ErrorResponse(false, null, message, null, null);
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(false, code, message, null, null);
    }
}
