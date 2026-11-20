package com.yt.backend.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yt.backend.entity.Notification;
import com.yt.backend.entity.User;

import java.util.Date;

/** 1 thong bao kem ten / avatar chu tai khoan (Header.js hien danh sach) */
public record NotificationDto(String id, String title, String content, String link,
                              @JsonProperty("user_id") String userId,
                              @JsonProperty("is_read") Integer isRead,
                              @JsonProperty("created_at") Date createdAt,
                              String username, String avatar) {

    public static NotificationDto of(Notification n, User owner) {
        return new NotificationDto(n.getId(), n.getTitle(), n.getContent(), n.getLink(), n.getUserId(), n.getIsRead(),
                n.getCreatedAt(), owner == null ? null : owner.getUsername(), owner == null ? null : owner.getAvatar());
    }
}
