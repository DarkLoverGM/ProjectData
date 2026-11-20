package com.yt.backend.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UserReportStatusRequest(
        @NotNull(message = "Trạng thái không hợp lệ")
        @Pattern(regexp = "pending|resolved|dismissed", message = "Trạng thái không hợp lệ") String status) {}
