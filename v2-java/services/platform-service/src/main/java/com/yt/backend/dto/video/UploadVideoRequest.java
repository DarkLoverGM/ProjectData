package com.yt.backend.dto.video;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Field chu cua form dang video (multipart). File video / thumb nhan rieng.
 * tags: chuoi JSON ["a","b"]; subtitles: chuoi JSON { vi: [cues], en: [cues] }
 */
public record UploadVideoRequest(
        @NotBlank(message = "Thiếu dữ liệu bắt buộc (title)")
        @Size(max = 100, message = "Tiêu đề tối đa 100 ký tự") String title,
        @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự") String description,
        String group,
        String tags,
        String subtitles) {}
