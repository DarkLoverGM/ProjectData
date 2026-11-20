package com.yt.backend.dto.auth;

public record CaptchaRequest(String captchaToken, String userInput) {}
