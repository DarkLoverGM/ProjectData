package com.yt.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

/** 1 bao cao binh luan kem noi dung binh luan, tac gia, nguoi bao cao, tieu de video */
public record CommentReportRow(String id, String reason, String detail, @JsonProperty("created_at") Date createdAt,
                               Integer status, @JsonProperty("comment_id") String commentId,
                               @JsonProperty("video_id") String videoId,
                               @JsonProperty("comment_content") String commentContent,
                               @JsonProperty("comment_author") String commentAuthor,
                               String reporter, @JsonProperty("video_title") String videoTitle) {}
