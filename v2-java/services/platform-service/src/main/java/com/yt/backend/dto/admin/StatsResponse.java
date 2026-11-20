package com.yt.backend.dto.admin;

import java.util.Date;
import java.util.List;

/** So lieu tong quan cua trang admin */
public record StatsResponse(Users users, Videos videos, Total comments, Reports reports, Total tags,
                            List<RecentVideo> recentVideos) {

    public record Users(long total, long admins, long banned) {}

    public record Videos(long total, long visible, long views) {}

    public record Total(long total) {}

    public record Reports(long total, long pending, long usersPending, long appeals) {}

    public record RecentVideo(String id, String title, String slug, Long view, Integer status, Date uploadAt, String username) {}
}
