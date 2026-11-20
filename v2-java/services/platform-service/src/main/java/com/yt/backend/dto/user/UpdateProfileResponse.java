package com.yt.backend.dto.user;

public record UpdateProfileResponse(boolean success, String message, String avatar, String banner) {}
