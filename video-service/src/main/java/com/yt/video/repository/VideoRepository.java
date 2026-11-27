package com.yt.video.repository;

import com.yt.video.entity.Video;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface VideoRepository extends MongoRepository<Video, String> {
    List<Video> findByChannelId(String channelId);
    List<Video> findByCategoryIgnoreCase(String category);
    List<Video> findByTitleContainingIgnoreCase(String keyword);
}
