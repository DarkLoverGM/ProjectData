package com.yt.backend.repository;

import com.yt.backend.entity.CommentReaction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Like / dislike binh luan. */
public interface CommentReactionRepository extends MongoRepository<CommentReaction, String> {

    Optional<CommentReaction> findOneByCommentIdAndUserId(String commentId, String userId);

    List<CommentReaction> findByUserIdAndCommentIdIn(String userId, Collection<String> commentIds);

    long countByCommentIdAndAction(String commentId, String action);

    long deleteByCommentIdAndUserIdAndAction(String commentId, String userId, String action);

    long deleteByCommentIdIn(Collection<String> commentIds);

    long deleteByUserId(String userId);
}
