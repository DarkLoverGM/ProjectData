package com.yt.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        String id,
        String userId,
        String userName,
        String type,
        String channelId,
        String channelName,
        String videoId,
        String videoTitle,
        String tierId,
        String tierName,
        Integer months,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String message,
        String provider,
        String status,
        Instant createdAt
) {}
