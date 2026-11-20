package com.yt.backend.dto.user;

public record FollowStatusResponse(boolean success, boolean isFollowing, long followersCount) {}
