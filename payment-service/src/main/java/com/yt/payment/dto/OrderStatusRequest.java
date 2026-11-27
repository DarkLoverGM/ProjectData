package com.yt.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OrderStatusRequest(
        @NotBlank(message = "Trạng thái không được để trống")
        @Pattern(regexp = "PENDING|PAID|CANCELLED", message = "Trạng thái phải là PENDING, PAID hoặc CANCELLED")
        String status
) {}
