package com.yt.backend.dto.auth;

import com.yt.backend.entity.User;

/** Thong tin co ban cua user dang dang nhap (khong co mat khau) */
public record UserSummary(String id, String email, String username, String role, String avatar) {

    public static UserSummary from(User u) {
        return new UserSummary(u.getId(), u.getEmail(), u.getUsername(), u.getRole(), u.getAvatar());
    }
}
