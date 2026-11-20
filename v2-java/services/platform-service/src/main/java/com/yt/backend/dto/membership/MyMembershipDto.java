package com.yt.backend.dto.membership;

import java.util.Date;

/** 1 kenh ma nguoi dung dang la hoi vien (trang Giao dich cua toi) */
public record MyMembershipDto(String channelId, String channelName, String channelAvatar, String tierId,
                              String tierName, long price, Date startedAt, Date expiresAt) {}
