package com.yt.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** audience: all | admins | user (can userId) */
public record BroadcastRequest(
        @NotBlank(message = "Cần tiêu đề và nội dung")
        @Size(max = 120, message = "Tiêu đề tối đa 120 ký tự") String title,
        @NotBlank(message = "Cần tiêu đề và nội dung")
        @Size(max = 2000, message = "Nội dung tối đa 2000 ký tự") String content,
        @NotNull(message = "Đối tượng nhận không hợp lệ")
        @Pattern(regexp = "all|admins|user", message = "Đối tượng nhận không hợp lệ") String audience,
        String userId) {}
