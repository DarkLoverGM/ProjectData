package com.yt.backend.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCommentRequest(
        @NotBlank(message = "Nội dung bình luận trống")
        @Size(max = 1000, message = "Bình luận tối đa 1000 ký tự") String text,
        String parentId) {}
