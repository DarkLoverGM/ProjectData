package com.yt.backend.repository;

import com.yt.backend.entity.VideoReaction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.Optional;

/** Like / dislike video. */
public interface VideoReactionRepository extends MongoRepository<VideoReaction, String> {

    Optional<VideoReaction> findOneByVideoIdAndUserId(String videoId, String userId);

    long countByVideoIdAndAction(String videoId, String action);

    long deleteByVideoIdAndUserIdAndAction(String videoId, String userId, String action);

    long deleteByUserId(String userId);

    long deleteByVideoIdIn(Collection<String> videoIds);
}
