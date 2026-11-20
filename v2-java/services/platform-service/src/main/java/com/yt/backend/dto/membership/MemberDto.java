package com.yt.backend.dto.membership;

import java.util.Date;

/** 1 hoi vien cua kenh (Studio) */
public record MemberDto(String userId, String username, String avatar, String tierName, Date startedAt, Date expiresAt) {}
