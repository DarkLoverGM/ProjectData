package com.yt.backend.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Tao don thanh toan.
 * Hoi vien:     { type: "membership", provider, tierId, months }
 * Super Thanks: { type: "super_thanks", provider, videoId, amount, message }
 * Rang buoc rieng tung loai (so thang hop le, muc tien...) kiem tra trong PaymentService.
 */
public record CheckoutRequest(
        @NotBlank(message = "Loại giao dịch không hợp lệ")
        @Pattern(regexp = "membership|super_thanks", message = "Loại giao dịch không hợp lệ") String type,
        @NotBlank(message = "Cổng thanh toán không khả dụng") String provider,
        String tierId,
        Integer months,
        String videoId,
        Long amount,
        @Size(max = 200, message = "Lời nhắn tối đa 200 ký tự") String message) {}
