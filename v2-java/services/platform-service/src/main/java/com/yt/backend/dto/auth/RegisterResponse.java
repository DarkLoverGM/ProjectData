package com.yt.backend.dto.auth;

public record RegisterResponse(boolean success, String message, UserSummary data) {}
