package com.yt.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MembershipTierResponse(
        String id,
        String channelId,
        String name,
        BigDecimal price,
        String perks,
        boolean active,
        Instant createdAt
) {}
