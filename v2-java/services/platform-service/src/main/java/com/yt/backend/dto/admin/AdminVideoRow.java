package com.yt.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

/** 1 dong trong danh sach video (admin) */
public record AdminVideoRow(String id, String title, String slug, String thumbnail, Long view, Integer status,
                            Date uploadAt, @JsonProperty("video_group") String videoGroup,
                            @JsonProperty("user_id") String userId, Moderation moderation, Appeal appeal,
                            String username, long comments) {

    public record Moderation(String state, List<String> rules) {}

    public record Appeal(Boolean pending) {}
}
