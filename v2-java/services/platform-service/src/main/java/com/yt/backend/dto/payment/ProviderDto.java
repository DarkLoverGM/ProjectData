package com.yt.backend.dto.payment;

/** 1 cong thanh toan; enabled = false: server chua cau hinh key */
public record ProviderDto(String id, String label, boolean enabled) {}
