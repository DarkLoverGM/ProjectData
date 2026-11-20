package com.yt.backend.dto.admin;

public record DeleteUserResponse(boolean success, String username, long videos, long comments) {}
