package com.yt.backend.repository;

import com.yt.backend.entity.Follow;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/** Theo doi kenh. */
public interface FollowRepository extends MongoRepository<Follow, String> {

    boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);

    List<Follow> findByFollowerId(String followerId);

    List<Follow> findByFollowingIdAndFollowerIdNot(String followingId, String followerId);

    long countByFollowingId(String followingId);

    long countByFollowerId(String followerId);

    long deleteByFollowerIdAndFollowingId(String followerId, String followingId);

    long deleteByFollowerIdOrFollowingId(String followerId, String followingId);
}
