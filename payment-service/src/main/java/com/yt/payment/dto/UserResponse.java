package com.yt.payment.dto;

public record UserResponse(
        String id,
        String username,
        String email,
        String channelName,
        String role
) {}
