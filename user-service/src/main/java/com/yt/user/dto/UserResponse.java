package com.yt.user.dto;

import java.time.Instant;

public record UserResponse(
        String id,
        String username,
        String email,
        String channelName,
        String avatar,
        String bio,
        String role,
        Instant createdAt
) {}
