package com.yt.backend.dto.user;

import com.yt.backend.dto.video.VideoDto;

import java.util.List;

/** GET /api/user/userdata/{id}: trang profile cong khai */
public record PublicProfileResponse(String id, String username, String avatar, String banner, int videoCount,
                                    List<VideoDto> videos, long followersCount) {}
