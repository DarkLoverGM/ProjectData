package com.yt.backend.dto.studio;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Sua video trong Studio (multipart, thumbnail gui o field `thumb` rieng). Field null = khong doi.
 * tags: chuoi JSON ["a","b"]; membersOnly: "true" / "false"; visibility: public | unlisted | private
 */
public record UpdateVideoRequest(
        @Pattern(regexp = ".*\\S.*", message = "Tiêu đề không được để trống")
        @Size(max = 100, message = "Tiêu đề tối đa 100 ký tự") String title,
        @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự") String description,
        String tags,
        @Pattern(regexp = "true|false", message = "membersOnly phải là true hoặc false") String membersOnly,
        @Pattern(regexp = "public|unlisted|private", message = "Chế độ hiển thị không hợp lệ") String visibility) {}
