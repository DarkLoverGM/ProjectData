package com.yt.backend.dto.membership;

import com.yt.backend.entity.MembershipTier;

import java.util.Date;

public record TierDto(String id, String channelId, String name, long price, String perks, boolean active, Date createdAt) {

    public static TierDto from(MembershipTier t) {
        return new TierDto(t.getId(), t.getChannelId(), t.getName(), t.getPrice() == null ? 0 : t.getPrice(),
                t.getPerks(), t.isOnSale(), t.getCreatedAt());
    }
}
