package com.yt.backend.dto.auth;

public record CaptchaResponse(boolean success, String message, String token, Svg data) {

    public record Svg(String svg) {}
}
