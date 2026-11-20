package com.yt.backend.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yt.backend.auth.AuthUser;
import com.yt.backend.common.ApiException;
import com.yt.backend.dto.video.GroupedVideos;
import com.yt.backend.dto.video.TagCount;
import com.yt.backend.dto.video.TagDto;
import com.yt.backend.dto.video.VideoDto;
import com.yt.backend.entity.Tag;
import com.yt.backend.service.TagService;
import com.yt.backend.service.UserService;
import com.yt.backend.service.VideoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Tim kiem va tag: /api/search (controllers/SearchController.js), /api/tag/* (controllers/TagController.js) */
@RestController
@RequestMapping("/api")
public class DiscoveryController {

    /** Ket qua tim kiem: { success, main, short } */
    public record SearchResponse(boolean success, List<VideoDto> main, @JsonProperty("short") List<VideoDto> shorts) {}

    public record TagsResponse(List<TagCount> tags) {}

    /** Video gan 1 tag: { tag: { name, slug }, main, short } */
    public record TagVideosResponse(TagDto tag, List<VideoDto> main, @JsonProperty("short") List<VideoDto> shorts) {}

    private final TagService tagService;
    private final VideoService videoService;
    private final UserService userService;

    public DiscoveryController(TagService tagService, VideoService videoService, UserService userService) {
        this.tagService = tagService;
        this.videoService = videoService;
        this.userService = userService;
    }

    /** GET /api/search?q=<tu khoa|random> (trang ket qua: pages/SearchResult.js) */
    @GetMapping("/search")
    public SearchResponse search(@AuthenticationPrincipal AuthUser user, @RequestParam(required = false) String q) {
        GroupedVideos data = videoService.searchVideos(q);
        // An video cua nguoi da bi chan, giong /api/videos/all
        if (user != null) data = data.withoutUploaders(userService.getBlockedUsers(user.id()));
        return new SearchResponse(true, data.main(), data.shorts());
    }

    /** Tag duoc dung nhieu nhat */
    @GetMapping("/tag")
    public TagsResponse popular(@RequestParam(required = false) String limit) {
        return new TagsResponse(tagService.getPopularTags(TagService.toLimit(limit, 20, 50)));
    }

    /** Goi y khi dang go tag luc upload */
    @GetMapping("/tag/suggest")
    public TagsResponse suggest(@RequestParam(required = false) String q, @RequestParam(required = false) String limit) {
        return new TagsResponse(tagService.suggestTags(q == null ? "" : q, TagService.toLimit(limit, 8, 20)));
    }

    /** Video gan tag nay, tach main / short */
    @GetMapping("/tag/{slug}")
    public TagVideosResponse byTag(@PathVariable String slug) {
        Tag tag = tagService.getTagBySlug(slug);
        if (tag == null) throw ApiException.notFound("Tag không tồn tại");
        GroupedVideos videos = videoService.getVideosByTag(tag.getId());
        return new TagVideosResponse(new TagDto(tag.getName(), tag.getSlug()), videos.main(), videos.shorts());
    }
}
