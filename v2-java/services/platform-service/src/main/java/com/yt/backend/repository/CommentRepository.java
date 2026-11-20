package com.yt.backend.repository;

import com.yt.backend.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Binh luan. */
public interface CommentRepository extends MongoRepository<Comment, String> {

    Optional<Comment> findOneById(String id);

    List<Comment> findByVideoIdOrderByCreatedAtAsc(String videoId);

    List<Comment> findByUserId(String userId);

    List<Comment> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<Comment> findByIdIn(Collection<String> ids);

    List<Comment> findByParentIdIn(Collection<String> parentIds);

    List<Comment> findByVideoIdIn(Collection<String> videoIds);

    long countByVideoId(String videoId);

    long countByUserId(String userId);

    long deleteByIdIn(Collection<String> ids);
}
