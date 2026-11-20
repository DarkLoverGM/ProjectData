package com.yt.backend.controller;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.dto.MessageResponse;
import com.yt.backend.dto.user.FollowStatusResponse;
import com.yt.backend.service.FollowService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** /api/follow/:id (controllers/user/followController.js) */
@RestController
@RequestMapping("/api/follow")
@PreAuthorize("isAuthenticated()")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @GetMapping("/{id}")
    public FollowStatusResponse status(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return new FollowStatusResponse(true, followService.isFollowing(me.id(), id), followService.countFollowers(id));
    }

    @PostMapping("/{id}")
    public MessageResponse follow(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return followService.follow(me.id(), id);
    }

    @DeleteMapping("/{id}")
    public MessageResponse unfollow(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return followService.unfollow(me.id(), id);
    }
}
