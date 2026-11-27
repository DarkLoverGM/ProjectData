package com.yt.video.dto;

import java.time.Instant;

public record VideoResponse(
        String id,
        String title,
        String description,
        String thumbnail,
        String videoUrl,
        String channelId,
        String category,
        long views,
        boolean membersOnly,
        String status,
        Instant uploadAt
) {}
