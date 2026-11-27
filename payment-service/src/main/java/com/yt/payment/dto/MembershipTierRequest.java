package com.yt.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MembershipTierRequest(
        @NotBlank(message = "Mã kênh (channelId) không được để trống")
        String channelId,

        @NotBlank(message = "Tên gói hội viên không được để trống")
        @Size(max = 100, message = "Tên gói tối đa 100 ký tự")
        String name,

        @NotNull(message = "Giá gói không được để trống")
        @Positive(message = "Giá gói phải lớn hơn 0")
        BigDecimal price,

        @Size(max = 500, message = "Quyền lợi tối đa 500 ký tự")
        String perks,

        Boolean active
) {}
