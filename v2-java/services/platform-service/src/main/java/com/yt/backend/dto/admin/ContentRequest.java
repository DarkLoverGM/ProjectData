package com.yt.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContentRequest(
        @NotBlank(message = "Nội dung trống")
        @Size(max = 1000, message = "Tối đa 1000 ký tự") String content) {}
