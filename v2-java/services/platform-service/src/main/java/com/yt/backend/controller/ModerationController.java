package com.yt.backend.controller;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.common.ApiException;
import com.yt.backend.dto.MessageResponse;
import com.yt.backend.dto.comment.CommentReportDto;
import com.yt.backend.dto.comment.ReportCommentRequest;
import com.yt.backend.dto.user.ReportUserRequest;
import com.yt.backend.entity.UserReport;
import com.yt.backend.service.CommentService;
import com.yt.backend.service.UserService;
import com.yt.backend.websocket.AdminEvent;
import com.yt.backend.websocket.AdminRealtime;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Duong dan cu ban Node da bo, giu lai cho code cu (frontend hien tai khong goi):
 * /api/blockUser/:id, /api/report/user/:id, /api/blockComment/:commentId/:videoId
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("isAuthenticated()")
public class ModerationController {

    private final UserService userService;
    private final CommentService commentService;
    private final AdminRealtime adminRealtime;

    public ModerationController(UserService userService, CommentService commentService, AdminRealtime adminRealtime) {
        this.userService = userService;
        this.commentService = commentService;
        this.adminRealtime = adminRealtime;
    }

    /** Frontend moi goi /api/user/block/:id (UserBlockController) */
    @PostMapping("/blockUser/{id}")
    public MessageResponse block(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return userService.blockUser(me.id(), id);
    }

    @DeleteMapping("/blockUser/{id}")
    public MessageResponse unblock(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return userService.unblockUser(me.id(), id);
    }

    @PostMapping("/report/user/{id}")
    public ResponseEntity<MessageResponse> reportUser(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                                      @Valid @RequestBody ReportUserRequest body) {
        UserReport report = userService.reportUser(me.id(), id, body.reason(), body.detail());
        adminRealtime.publish(new AdminEvent.UserReported(report.getId(), id, report.getReason()));
        return ResponseEntity.status(201).body(MessageResponse.ok("Report submitted"));
    }

    /** body { commentId, reason, detail } */
    @PostMapping("/blockComment/{commentId}/{videoId}")
    public ResponseEntity<CommentReportDto> reportComment(@AuthenticationPrincipal AuthUser me, @PathVariable String videoId,
                                                          @Valid @RequestBody ReportCommentRequest body) {
        if (body.commentId() == null || body.commentId().isEmpty()) throw ApiException.badRequest("commentId is required");
        if (body.reason() == null || body.reason().isEmpty()) throw ApiException.badRequest("Reason is required");
        if (commentService.getCommentById(body.commentId()) == null) throw ApiException.notFound("Bình luận không tồn tại");
        CommentReportDto report = commentService.reportComment(body.commentId(), me.id(), videoId, body.reason(), body.detail());
        adminRealtime.publish(new AdminEvent.CommentReported(report.id(), body.commentId(), videoId, report.reason()));
        return ResponseEntity.status(201).body(report);
    }
}
