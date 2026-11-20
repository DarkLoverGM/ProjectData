package com.yt.backend.dto.user;

import java.util.List;

/** GET /api/user/userinfo: user dang dang nhap + danh sach dang follow / da chan */
public record UserInfoResponse(String id, String username, String email, String role, String avatar, String banner,
                               List<String> following, List<String> blockedUsers) {}
