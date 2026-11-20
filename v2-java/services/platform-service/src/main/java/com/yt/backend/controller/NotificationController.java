package com.yt.backend.controller;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.common.ApiException;
import com.yt.backend.dto.SuccessResponse;
import com.yt.backend.dto.notification.CountResponse;
import com.yt.backend.dto.notification.NotificationDto;
import com.yt.backend.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * /api/user/notification* (controllers/user/notificationController.js).
 * Moi thao tac chi tren thong bao cua chinh nguoi dang dang nhap (khong nhan userId tu client).
 */
@RestController
@RequestMapping("/api/user/notification")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationDto> mine(@AuthenticationPrincipal AuthUser me) {
        return notificationService.getNotifications(me.id());
    }

    /** Duong dan cu (Header.js goi kem id cua minh). Xem thong bao nguoi khac: chi admin */
    @GetMapping("/{id}")
    public List<NotificationDto> byUser(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        if (!id.equals(me.id()) && !"admin".equals(me.role())) throw ApiException.forbidden("Không thể xem thông báo của người khác");
        return notificationService.getNotifications(id);
    }

    @GetMapping("/unread/count")
    public CountResponse unreadCount(@AuthenticationPrincipal AuthUser me) {
        return new CountResponse(true, notificationService.getUnreadCount(me.id()));
    }

    @PostMapping("/read/{id}")
    public SuccessResponse markAsRead(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        notificationService.markAsRead(id, me.id());
        return SuccessResponse.OK;
    }

    @PostMapping("/read-all")
    public SuccessResponse markAllAsRead(@AuthenticationPrincipal AuthUser me) {
        notificationService.markAllAsRead(me.id());
        return SuccessResponse.OK;
    }

    @DeleteMapping("/{id}")
    public SuccessResponse delete(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        notificationService.deleteNotification(id, me.id());
        return SuccessResponse.OK;
    }
}
