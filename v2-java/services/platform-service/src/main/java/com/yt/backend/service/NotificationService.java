package com.yt.backend.service;

import com.yt.backend.common.MongoUtils;
import com.yt.backend.dto.notification.NotificationDto;
import com.yt.backend.entity.Follow;
import com.yt.backend.entity.Notification;
import com.yt.backend.entity.User;
import com.yt.backend.repository.FollowRepository;
import com.yt.backend.repository.NotificationRepository;
import com.yt.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/** Thong bao trong app (luu DB) + gui kem push (PushService) */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int BATCH = 1000;

    private final NotificationRepository notifications;
    private final FollowRepository follows;
    private final UserRepository users;
    private final PushService push;
    private final MongoTemplate mongo;

    public NotificationService(NotificationRepository notifications, FollowRepository follows, UserRepository users,
                               PushService push, MongoTemplate mongo) {
        this.notifications = notifications;
        this.follows = follows;
        this.users = users;
        this.push = push;
        this.mongo = mongo;
    }

    /**
     * Gui 1 thong bao cho nguoi dung. link: trang mo khi bam (null = trang chu).
     * Loi chi ghi log: thong bao la phu, khong duoc lam hong thao tac chinh.
     */
    public void send(String userId, String title, String content, String link) {
        if (userId == null || userId.isEmpty()) return;
        try {
            notifications.insert(Notification.create(MongoUtils.uuid(), userId, title, content, link));
            push.sendPush(List.of(userId), title, content, link);
        } catch (RuntimeException e) {
            log.error("Error sending notification", e);
        }
    }

    public void send(String userId, String title, String content) {
        send(userId, title, content, null);
    }

    /** Gui cung 1 thong bao cho nhieu nguoi, ghi theo lo de khong giu ca danh sach lon trong RAM. @return so da gui */
    public int sendMany(Collection<String> userIds, String title, String content) {
        List<String> ids = List.copyOf(userIds);
        for (int i = 0; i < ids.size(); i += BATCH) {
            List<String> chunk = ids.subList(i, Math.min(i + BATCH, ids.size()));
            notifications.insert(chunk.stream()
                    .map(id -> Notification.create(MongoUtils.uuid(), id, title, content, null)).toList());
            push.sendPush(chunk, title, content);
        }
        return ids.size();
    }

    /** Thong bao video moi cho moi nguoi dang follow kenh. @return so thong bao da gui */
    public int sendNotificationVideo(String title, String uploaderId) {
        try {
            User uploader = users.findOneById(uploaderId).orElse(null);
            if (uploader == null) return 0;
            List<String> followers = follows.findByFollowingIdAndFollowerIdNot(uploaderId, uploaderId).stream()
                    .map(Follow::getFollowerId).distinct().toList();
            if (followers.isEmpty()) return 0;
            return sendMany(followers, "Video mới", uploader.getUsername() + " vừa đăng video \"" + title + "\"");
        } catch (RuntimeException e) {
            log.error("Error sending video notification", e);
            return 0;
        }
    }

    /** Thong bao cua user, moi nhat truoc */
    public List<NotificationDto> getNotifications(String userId) {
        User owner = users.findOneById(userId).orElse(null);
        return notifications.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> NotificationDto.of(n, owner)).toList();
    }

    public void markAsRead(String id, String userId) {
        mongo.updateFirst(Query.query(Criteria.where("id").is(id).and("user_id").is(userId)),
                Update.update("is_read", 1), Notification.class);
    }

    public void markAllAsRead(String userId) {
        mongo.updateMulti(Query.query(Criteria.where("user_id").is(userId)), Update.update("is_read", 1), Notification.class);
    }

    public void deleteNotification(String id, String userId) {
        notifications.deleteOneByIdAndUserId(id, userId);
    }

    public long getUnreadCount(String userId) {
        return notifications.countByUserIdAndIsRead(userId, 0);
    }
}
