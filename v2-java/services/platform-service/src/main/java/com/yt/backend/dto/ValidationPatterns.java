package com.yt.backend.dto;

/** Hang so dung chung trong cac annotation Bean Validation cua DTO */
public final class ValidationPatterns {

    private ValidationPatterns() {}

    /** Giong emailRules ben frontend (src/utils/validation.js) */
    public static final String EMAIL = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

    public static final int PASSWORD_MIN = 6;
    /** BCrypt chi dung 72 byte dau */
    public static final int PASSWORD_MAX = 72;
    public static final String PASSWORD_MESSAGE = "Mật khẩu từ 6 đến 72 ký tự";
}
