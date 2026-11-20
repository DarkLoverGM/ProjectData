package com.yt.backend.dto.admin;

import com.yt.backend.dto.user.BanInfo;

/** 1 dong trong danh sach nguoi dung (admin) */
public record AdminUserRow(String id, String username, String email, String avatar, String role, BanInfo ban,
                           long videos, long followers, long pendingReports) {}
