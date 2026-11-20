package com.yt.backend.repository;

import com.yt.backend.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/** Thong bao. */
public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    long countByUserIdAndIsRead(String userId, int isRead);

    long deleteOneByIdAndUserId(String id, String userId);

    long deleteByUserId(String userId);
}
