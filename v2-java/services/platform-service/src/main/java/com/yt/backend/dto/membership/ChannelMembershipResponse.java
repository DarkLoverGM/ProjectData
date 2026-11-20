package com.yt.backend.dto.membership;

import java.util.Date;
import java.util.List;

/** Thong tin hoi vien cua 1 kenh cho nguoi dang xem (membership = null: chua tham gia) */
public record ChannelMembershipResponse(String channelId, List<TierDto> tiers, long memberCount,
                                        MyMembership membership, boolean isOwner) {

    public record MyMembership(String tierId, String tierName, Date startedAt, Date expiresAt) {}
}
