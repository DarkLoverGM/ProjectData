package com.yt.backend.dto.payment;

/** Don vua tao + link trang thanh toan cua cong (frontend chuyen nguoi dung sang do) */
public record CheckoutResponse(boolean success, String orderId, String payUrl, long amount) {}
