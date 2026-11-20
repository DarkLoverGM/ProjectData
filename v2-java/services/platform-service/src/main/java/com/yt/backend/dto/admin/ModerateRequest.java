package com.yt.backend.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Quyet dinh sau khi xem video. rules: id tieu chuan vi pham (VideoRules), atTime: giay trong video */
public record ModerateRequest(
        @NotNull(message = "Quyết định không hợp lệ")
        @Pattern(regexp = "block|approve", message = "Quyết định không hợp lệ") String decision,
        List<String> rules,
        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự") String note,
        Double atTime) {}
