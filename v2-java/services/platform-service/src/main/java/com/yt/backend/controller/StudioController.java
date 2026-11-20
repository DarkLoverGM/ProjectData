package com.yt.backend.controller;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.common.ApiException;
import com.yt.backend.dto.studio.*;
import com.yt.backend.service.StudioService;
import com.yt.backend.websocket.AdminEvent;
import com.yt.backend.websocket.AdminRealtime;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Trang Studio cua nguoi dang (src/pages/Studio.js), controllers/StudioController.js.
 * Chi thao tac tren video cua chinh minh (service kiem tra user_id).
 */
@RestController
@RequestMapping("/api/studio")
@PreAuthorize("isAuthenticated()")
public class StudioController {

    private final StudioService studioService;
    private final AdminRealtime adminRealtime;

    public StudioController(StudioService studioService, AdminRealtime adminRealtime) {
        this.studioService = studioService;
        this.adminRealtime = adminRealtime;
    }

    @GetMapping("/videos")
    public StudioListResponse list(@AuthenticationPrincipal AuthUser me) {
        return studioService.listVideos(me.id());
    }

    @GetMapping("/videos/{id}")
    public StudioVideoDetail get(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return studioService.getVideo(me.id(), id);
    }

    /** multipart: title, description, tags (chuoi JSON), membersOnly ("true" / "false"), thumb (tuy chon) */
    @PatchMapping("/videos/{id}")
    public StudioVideoResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                      @Valid @ModelAttribute UpdateVideoRequest body,
                                      @RequestParam(required = false) MultipartFile thumb,
                                      @RequestParam(required = false) MultipartFile video) throws IOException {
        if (video != null && !video.isEmpty()) throw ApiException.badRequest("Thay file video dùng /videos/:id/media");
        VideoController.checkFiles(null, thumb, "main");
        return studioService.updateDetails(me.id(), id, body, thumb);
    }

    /** multipart: video (bat buoc), thumb (tuy chon). Giu nguyen id -> giu luot xem, like, binh luan */
    @PostMapping("/videos/{id}/media")
    public StudioVideoResponse replaceMedia(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                           @RequestParam(required = false) MultipartFile video,
                                           @RequestParam(required = false) MultipartFile thumb) throws IOException {
        StudioVideoDetail current = studioService.getVideo(me.id(), id);
        VideoController.checkFiles(video, thumb, current.videoGroup());
        StudioVideoResponse result = studioService.replaceMedia(me, id, video, thumb);
        adminRealtime.publish(new AdminEvent.VideoMedia(id, me.username()));
        return result;
    }

    /** body { content, resubmit? } */
    @PostMapping("/videos/{id}/messages")
    public MessageItemResponse sendMessage(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                           @Valid @RequestBody MessageRequest body) {
        MessageItemResponse result = studioService.sendMessage(me, id, body.content(), Boolean.TRUE.equals(body.resubmit()));
        // Admin dang mo man duyet video nay thay tin ngay (chat realtime), admin khac thay so "can phan hoi" tang
        adminRealtime.publish(new AdminEvent.VideoMessage(result.item()));
        return result;
    }
}
