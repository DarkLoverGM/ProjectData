package com.yt.backend.dto.studio;

import java.util.List;

public record StudioListResponse(List<StudioVideoItem> items, Summary summary) {

    public record Summary(long videos, long views, long likes, long comments, long blocked) {}
}
