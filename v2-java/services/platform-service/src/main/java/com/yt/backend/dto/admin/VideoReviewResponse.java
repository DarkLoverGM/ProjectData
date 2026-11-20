package com.yt.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yt.backend.dto.studio.VideoMessageDto;
import com.yt.backend.dto.user.BanInfo;

import java.util.Date;
import java.util.List;
import java.util.Map;

/** Du lieu cho man duyet video. reviewUrl: trang player cua frontend (src/pages/admin/VideoReview.js) */
public record VideoReviewResponse(String id, String title, String slug, String description, String src, String thumbnail,
                                  @JsonProperty("video_group") String videoGroup, Map<String, Object> speciallist,
                                  Integer status, Date uploadAt, ModerationDto moderation, AppealDto appeal,
                                  @JsonProperty("media_updated_at") Date mediaUpdatedAt, List<VideoMessageDto> messages,
                                  List<String> tags, Stats stats, Uploader uploader, List<AuditDto> history,
                                  String reviewUrl) {

    public record Stats(long view, long likes, long dislikes, long comments) {}

    public record Uploader(String id, String username, String avatar, BanInfo ban, long videos, long blockedVideos) {}
}
