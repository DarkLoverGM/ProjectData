package com.yt.backend.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** status: 0 | 1 (video: an / hien; bao cao binh luan: cho xu ly / da xu ly) */
public record StatusRequest(
        @NotNull(message = "Status phải là 0 hoặc 1")
        @Min(value = 0, message = "Status phải là 0 hoặc 1")
        @Max(value = 1, message = "Status phải là 0 hoặc 1") Integer status) {}
