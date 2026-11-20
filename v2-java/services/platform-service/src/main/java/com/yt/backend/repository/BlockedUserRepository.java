package com.yt.backend.repository;

import com.yt.backend.entity.BlockedUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/** Chan nguoi dung. */
public interface BlockedUserRepository extends MongoRepository<BlockedUser, String> {

    List<BlockedUser> findByUserId(String userId);

    long deleteByUserIdAndBlockedId(String userId, String blockedId);

    long deleteByUserIdOrBlockedId(String userId, String blockedId);
}
