package com.yt.payment.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record OrderRequest(
        @NotBlank(message = "Mã người dùng (userId) không được để trống")
        String userId,

        @NotBlank(message = "Loại đơn hàng không được để trống")
        @Pattern(regexp = "MEMBERSHIP|SUPER_THANKS", message = "Loại đơn hàng phải là MEMBERSHIP hoặc SUPER_THANKS")
        String type,

        String tierId,

        String videoId,

        @Min(value = 1, message = "Số tháng đăng ký phải lớn hơn 0")
        @Max(value = 24, message = "Số tháng đăng ký tối đa 24 tháng")
        Integer months,

        @Positive(message = "Số tiền ủng hộ phải lớn hơn 0")
        BigDecimal amount,

        @NotBlank(message = "Phương thức thanh toán không được để trống")
        @Pattern(regexp = "MOMO|VNPAY|BANK", message = "Phương thức thanh toán phải là MOMO, VNPAY hoặc BANK")
        String provider,

        @Size(max = 300, message = "Lời nhắn tối đa 300 ký tự")
        String message
) {}
