package com.yt.backend.controller;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.dto.MessageResponse;
import com.yt.backend.dto.user.ReportUserRequest;
import com.yt.backend.entity.UserReport;
import com.yt.backend.service.UserService;
import com.yt.backend.websocket.AdminEvent;
import com.yt.backend.websocket.AdminRealtime;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Chan / bo chan / bao cao user tu trang profile (controllers/user/blockController.js).
 * "Khong de xuat video cua nguoi nay": video cua nguoi bi chan bi loc khoi /api/videos/all va /more.
 */
@RestController
@RequestMapping("/api/user")
@PreAuthorize("isAuthenticated()")
public class UserBlockController {

    private final UserService userService;
    private final AdminRealtime adminRealtime;

    public UserBlockController(UserService userService, AdminRealtime adminRealtime) {
        this.userService = userService;
        this.adminRealtime = adminRealtime;
    }

    @PostMapping("/block/{id}")
    public MessageResponse block(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return userService.blockUser(me.id(), id);
    }

    @DeleteMapping("/block/{id}")
    public MessageResponse unblock(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return userService.unblockUser(me.id(), id);
    }

    @PostMapping("/report/{id}")
    public ResponseEntity<MessageResponse> report(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                                  @Valid @RequestBody ReportUserRequest body) {
        UserReport report = userService.reportUser(me.id(), id, body.reason(), body.detail());
        adminRealtime.publish(new AdminEvent.UserReported(report.getId(), id, report.getReason()));
        return ResponseEntity.status(201).body(MessageResponse.ok("Đã gửi báo cáo, chúng tôi sẽ xem xét sớm nhất có thể"));
    }
}
