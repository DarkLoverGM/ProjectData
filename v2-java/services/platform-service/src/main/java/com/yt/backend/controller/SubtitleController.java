package com.yt.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.yt.backend.auth.AuthUser;
import com.yt.backend.dto.MessageResponse;
import com.yt.backend.dto.subtitle.SaveSubtitleResponse;
import com.yt.backend.dto.subtitle.SubtitlesResponse;
import com.yt.backend.service.SubtitleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Phu de (CC) cua video (controllers/video/videoSubtitleController.js):
 *   GET    /subtitles/:videoId        -> moi nguoi xem duoc
 *   PUT    /subtitles/:videoId/:lang  -> nguoi dang video / admin, body { cues: [{ start, end, text }] }
 *   DELETE /subtitles/:videoId/:lang  -> nguoi dang video / admin
 */
@RestController
@RequestMapping({"/api/videos", "/api/video"})
public class SubtitleController {

    private final SubtitleService subtitleService;

    public SubtitleController(SubtitleService subtitleService) {
        this.subtitleService = subtitleService;
    }

    @GetMapping("/subtitles/{videoId}")
    public SubtitlesResponse get(@PathVariable String videoId) {
        return new SubtitlesResponse(subtitleService.getSubtitles(videoId));
    }

    /** cues de dang JsonNode vi SubtitleService tu kiem tra tung dong (chap nhan so dang chuoi nhu Number() cua JS) */
    @PutMapping("/subtitles/{videoId}/{lang}")
    @PreAuthorize("isAuthenticated()")
    public SaveSubtitleResponse save(@AuthenticationPrincipal AuthUser user, @PathVariable String videoId,
                                     @PathVariable String lang, @RequestBody(required = false) JsonNode body) {
        subtitleService.checkEditPermission(videoId, user);
        int count = subtitleService.saveSubtitles(videoId, lang, body == null ? null : body.get("cues"), user.id());
        return new SaveSubtitleResponse(count > 0 ? "Đã lưu phụ đề" : "Đã xoá phụ đề", count);
    }

    @DeleteMapping("/subtitles/{videoId}/{lang}")
    @PreAuthorize("isAuthenticated()")
    public MessageResponse delete(@AuthenticationPrincipal AuthUser user, @PathVariable String videoId, @PathVariable String lang) {
        subtitleService.checkEditPermission(videoId, user);
        subtitleService.deleteSubtitles(videoId, lang);
        return MessageResponse.ok("Đã xoá phụ đề");
    }
}
