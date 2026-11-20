package com.yt.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportUserRequest(
        @NotBlank(message = "Vui lòng chọn lý do báo cáo")
        @Size(max = 255, message = "Lý do tối đa 255 ký tự") String reason,
        @Size(max = 2000, message = "Chi tiết tối đa 2000 ký tự") String detail) {}
