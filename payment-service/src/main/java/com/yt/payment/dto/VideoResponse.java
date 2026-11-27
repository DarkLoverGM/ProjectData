package com.yt.payment.dto;

public record VideoResponse(
        String id,
        String title,
        String channelId,
        String category,
        boolean membersOnly
) {}
