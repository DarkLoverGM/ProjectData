package com.yt.backend.repository;

import com.yt.backend.entity.VideoTag;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

/** Lien ket video - tag. */
public interface VideoTagRepository extends MongoRepository<VideoTag, String> {

    List<VideoTag> findByVideoId(String videoId);

    List<VideoTag> findByVideoIdIn(Collection<String> videoIds);

    List<VideoTag> findByTagId(String tagId);

    List<VideoTag> findByTagIdIn(Collection<String> tagIds);

    long deleteByVideoId(String videoId);

    long deleteByVideoIdIn(Collection<String> videoIds);
}
