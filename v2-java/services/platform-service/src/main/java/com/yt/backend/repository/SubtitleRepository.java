package com.yt.backend.repository;

import com.yt.backend.entity.Subtitle;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Phu de. */
public interface SubtitleRepository extends MongoRepository<Subtitle, String> {

    List<Subtitle> findByVideoId(String videoId);

    Optional<Subtitle> findOneByVideoIdAndLang(String videoId, String lang);

    long deleteByVideoIdAndLang(String videoId, String lang);

    long deleteByVideoIdIn(Collection<String> videoIds);
}
