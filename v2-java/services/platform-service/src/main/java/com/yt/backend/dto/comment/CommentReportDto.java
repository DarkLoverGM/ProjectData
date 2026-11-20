package com.yt.backend.dto.comment;

import java.util.Date;

/** Bao cao binh luan vua gui. status: 0 = cho xu ly */
public record CommentReportDto(String id, String commentId, String userId, String videoId, String reason,
                               String detail, Date createdAt, int status) {}
