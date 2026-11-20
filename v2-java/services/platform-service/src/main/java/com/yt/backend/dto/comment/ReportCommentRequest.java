package com.yt.backend.dto.comment;

import jakarta.validation.constraints.Size;

/** commentId chi dung o duong dan cu /api/blockComment (ModerationController) */
public record ReportCommentRequest(
        String commentId,
        @Size(max = 255, message = "Lý do tối đa 255 ký tự") String reason,
        @Size(max = 2000, message = "Chi tiết tối đa 2000 ký tự") String detail) {}
