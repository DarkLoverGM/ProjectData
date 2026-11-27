package com.yt.video.service;

import com.yt.video.dto.VideoRequest;
import com.yt.video.dto.VideoResponse;
import com.yt.video.entity.Video;
import com.yt.video.exception.ResourceNotFoundException;
import com.yt.video.repository.VideoRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class VideoService {

    private final VideoRepository repository;

    public VideoService(VideoRepository repository) {
        this.repository = repository;
    }

    private VideoResponse toResponse(Video video) {
        return new VideoResponse(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getThumbnail(),
                video.getVideoUrl(),
                video.getChannelId(),
                video.getCategory(),
                video.getViews(),
                video.isMembersOnly(),
                video.getStatus(),
                video.getUploadAt()
        );
    }

    public List<VideoResponse> getAll(String keyword, String category, String channelId) {
        List<Video> videos;
        if (keyword != null && !keyword.isBlank()) {
            videos = repository.findByTitleContainingIgnoreCase(keyword.trim());
        } else if (category != null && !category.isBlank()) {
            videos = repository.findByCategoryIgnoreCase(category.trim());
        } else if (channelId != null && !channelId.isBlank()) {
            videos = repository.findByChannelId(channelId.trim());
        } else {
            videos = repository.findAll();
        }
        return videos.stream().map(this::toResponse).toList();
    }

    public VideoResponse getById(String id) {
        Video video = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy video: " + id));
        return toResponse(video);
    }

    public VideoResponse create(VideoRequest request) {
        String id = "vid-" + UUID.randomUUID().toString().substring(0, 8);
        Video video = new Video(
                id,
                request.title().trim(),
                request.description() == null ? "" : request.description().trim(),
                (request.thumbnail() == null || request.thumbnail().isBlank())
                        ? "https://picsum.photos/seed/" + id + "/640/360"
                        : request.thumbnail().trim(),
                request.videoUrl() == null ? "" : request.videoUrl().trim(),
                request.channelId().trim(),
                request.category().trim(),
                0L,
                Boolean.TRUE.equals(request.membersOnly()),
                (request.status() == null || request.status().isBlank()) ? "PUBLIC" : request.status().toUpperCase(),
                Instant.now()
        );
        return toResponse(repository.save(video));
    }

    public VideoResponse update(String id, VideoRequest request) {
        Video video = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy video: " + id));

        video.setTitle(request.title().trim());
        video.setDescription(request.description() == null ? "" : request.description().trim());
        if (request.thumbnail() != null && !request.thumbnail().isBlank()) {
            video.setThumbnail(request.thumbnail().trim());
        }
        if (request.videoUrl() != null) {
            video.setVideoUrl(request.videoUrl().trim());
        }
        video.setChannelId(request.channelId().trim());
        video.setCategory(request.category().trim());
        if (request.membersOnly() != null) {
            video.setMembersOnly(request.membersOnly());
        }
        if (request.status() != null && !request.status().isBlank()) {
            video.setStatus(request.status().toUpperCase());
        }

        return toResponse(repository.save(video));
    }

    public VideoResponse incrementViews(String id) {
        Video video = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy video: " + id));
        video.setViews(video.getViews() + 1);
        return toResponse(repository.save(video));
    }

    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy video: " + id);
        }
        repository.deleteById(id);
    }
}
