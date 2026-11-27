package com.yt.video.controller;

import com.yt.video.dto.VideoRequest;
import com.yt.video.dto.VideoResponse;
import com.yt.video.service.VideoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService service;

    public VideoController(VideoService service) {
        this.service = service;
    }

    @GetMapping
    public List<VideoResponse> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String channelId
    ) {
        return service.getAll(keyword, category, channelId);
    }

    @GetMapping("/{id}")
    public VideoResponse getById(@PathVariable String id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<VideoResponse> create(@Valid @RequestBody VideoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public VideoResponse update(@PathVariable String id, @Valid @RequestBody VideoRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/view")
    public VideoResponse incrementViews(@PathVariable String id) {
        return service.incrementViews(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
