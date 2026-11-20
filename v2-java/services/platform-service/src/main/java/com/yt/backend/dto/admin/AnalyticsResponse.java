package com.yt.backend.dto.admin;

import java.util.List;

/** So lieu theo ngay + bang xep hang (trang Phan tich) */
public record AnalyticsResponse(int days, List<String> labels, Series series, Reactions reactions,
                                List<TopVideo> topVideos, List<TopCreator> topCreators, List<TopTag> topTags) {

    public record Series(List<Long> users, List<Long> videos, List<Long> comments, List<Long> reports) {}

    public record Reactions(long like, long dislike) {}

    public record TopVideo(String id, String title, Long view, Integer status, String username, long likes, long comments) {}

    public record TopCreator(String id, String username, long views, long videos, long followers) {}

    public record TopTag(String id, String name, long videos) {}
}
