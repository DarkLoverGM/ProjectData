package com.yt.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yt.backend.dto.user.BanInfo;

import java.util.Date;
import java.util.List;

/** Chi tiet 1 user cho admin: dang nhap bang gi, so lieu, binh luan gan day, lich su xu ly */
public record UserDetailResponse(String id, String username, String email, String avatar, String role,
                                 @JsonProperty("created_at") Date createdAt, List<String> loginMethods, BanInfo ban,
                                 Counts counts, List<RecentComment> recentComments, List<AuditDto> history) {

    public record Counts(long videos, long visibleVideos, long views, long comments, long followers, long following,
                         long reportsPending, long reportsTotal, long commentReports) {}

    public record RecentComment(String id, String content, @JsonProperty("created_at") Date createdAt,
                                @JsonProperty("video_id") String videoId, @JsonProperty("video_title") String videoTitle) {}
}
