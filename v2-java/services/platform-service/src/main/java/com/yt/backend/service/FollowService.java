package com.yt.backend.service;

import com.yt.backend.common.ApiException;
import com.yt.backend.common.MongoUtils;
import com.yt.backend.dto.MessageResponse;
import com.yt.backend.entity.Follow;
import com.yt.backend.entity.User;
import com.yt.backend.repository.FollowRepository;
import com.yt.backend.repository.UserRepository;
import com.mongodb.client.result.UpdateResult;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

/** Theo doi kenh (collection followers) */
@Service
public class FollowService {

    private final FollowRepository follows;
    private final UserRepository users;
    private final NotificationService notifications;
    private final MongoTemplate mongo;

    public FollowService(FollowRepository follows, UserRepository users, NotificationService notifications, MongoTemplate mongo) {
        this.follows = follows;
        this.users = users;
        this.notifications = notifications;
        this.mongo = mongo;
    }

    /** followerId follow followingId */
    public MessageResponse follow(String followerId, String followingId) {
        if (followerId.equals(followingId)) throw ApiException.badRequest("Không thể tự follow chính mình");
        if (!users.existsOneById(followingId)) throw ApiException.notFound("User not found");

        // Unique index (follower_id, following_id) dam bao khong bi trung, upsert de follow lai khong loi
        UpdateResult res = MongoUtils.ignoreDuplicate(() -> mongo.upsert(
                Query.query(Criteria.where("follower_id").is(followerId).and("following_id").is(followingId)),
                new Update().setOnInsert("id", MongoUtils.uuid()), Follow.class));

        // Chi bao khi vua follow moi (upsert co insert), follow lai khi dang follow thi thoi
        if (res != null && res.getUpsertedId() != null) {
            String name = users.findOneById(followerId).map(User::getUsername).orElse("Ai đó");
            notifications.send(followingId, "Người theo dõi mới", name + " đã theo dõi kênh của bạn", "/user/" + followerId);
        }
        return MessageResponse.ok("Followed successfully");
    }

    public MessageResponse unfollow(String followerId, String followingId) {
        follows.deleteByFollowerIdAndFollowingId(followerId, followingId);
        return MessageResponse.ok("Unfollowed successfully");
    }

    public boolean isFollowing(String followerId, String followingId) {
        return follows.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    /** Id nhung kenh ma userId dang follow */
    public List<String> getFollowingIds(String userId) {
        return follows.findByFollowerId(userId).stream().map(Follow::getFollowingId).distinct().toList();
    }

    public long countFollowers(String userId) {
        return follows.countByFollowingId(userId);
    }
}
