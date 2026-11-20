package com.yt.backend.repository;

import com.yt.backend.entity.VideoMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

/** Trao doi nguoi dang - admin. */
public interface VideoMessageRepository extends MongoRepository<VideoMessage, String> {

    List<VideoMessage> findByVideoIdOrderByCreatedAtAsc(String videoId);

    List<VideoMessage> findByVideoIdAndRoleNotOrderByCreatedAtDesc(String videoId, String role, Pageable pageable);

    List<VideoMessage> findByVideoIdIn(Collection<String> videoIds);

    long deleteByVideoIdIn(Collection<String> videoIds);
}
