package com.yt.backend.dto.payment;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;

/**
 * 1 don thanh toan kem ten nguoi mua, kenh, goi, video.
 * status: pending | paid | failed | expired (pending qua han hien la expired).
 * membershipExpiresAt: chi co o chi tiet don hoi vien da thanh toan.
 */
public record OrderDto(String id, String type, String channelId, String videoId, String tierId, int months, long amount,
                       String message, String provider, String status, String note, Date createdAt, Date paidAt,
                       String buyerId, String buyerName, String channelName, String channelAvatar, String tierName,
                       String videoTitle, String videoSlug, String providerTxn,
                       @JsonInclude(JsonInclude.Include.NON_NULL) Date membershipExpiresAt) {

    public OrderDto withMembershipExpiresAt(Date expiresAt) {
        return new OrderDto(id, type, channelId, videoId, tierId, months, amount, message, provider, status, note, createdAt,
                paidAt, buyerId, buyerName, channelName, channelAvatar, tierName, videoTitle, videoSlug, providerTxn, expiresAt);
    }
}
