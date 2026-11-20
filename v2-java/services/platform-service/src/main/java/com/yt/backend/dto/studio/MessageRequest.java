package com.yt.backend.dto.studio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** resubmit = true: video dang bi chan duoc danh dau "cho duyet lai" */
public record MessageRequest(
        @NotBlank(message = "Nội dung trống")
        @Size(max = 1000, message = "Tối đa 1000 ký tự") String content,
        Boolean resubmit) {}
