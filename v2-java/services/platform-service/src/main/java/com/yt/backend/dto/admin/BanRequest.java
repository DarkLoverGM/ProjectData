package com.yt.backend.dto.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** days = 0: khoa vinh vien */
public record BanRequest(
        @NotNull(message = "Số ngày phải từ 0 (vĩnh viễn) đến 3650")
        @DecimalMin(value = "0", message = "Số ngày phải từ 0 (vĩnh viễn) đến 3650")
        @DecimalMax(value = "3650", message = "Số ngày phải từ 0 (vĩnh viễn) đến 3650") Double days,
        @Size(max = 500, message = "Lý do tối đa 500 ký tự") String reason) {}
