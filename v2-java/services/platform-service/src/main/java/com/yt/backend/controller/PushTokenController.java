package com.yt.backend.controller;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.dto.SuccessResponse;
import com.yt.backend.dto.notification.PushTokenRequest;
import com.yt.backend.service.PushService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** /api/user/push-token: dang ky trinh duyet nhan push (Firebase Cloud Messaging), giong notificationController.js */
@RestController
@RequestMapping("/api/user/push-token")
public class PushTokenController {

    private final PushService pushService;

    public PushTokenController(PushService pushService) {
        this.pushService = pushService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public SuccessResponse save(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody PushTokenRequest body) {
        pushService.saveToken(me.id(), body.token());
        return SuccessResponse.OK;
    }

    /**
     * Huy dang ky (dang xuat / tat thong bao). Khong can dang nhap: luc goi co the token dang nhap da het han,
     * ma biet duoc FCM token thi cung chinh la trinh duyet do.
     */
    @DeleteMapping
    public SuccessResponse remove(@Valid @RequestBody PushTokenRequest body) {
        pushService.removeToken(body.token());
        return SuccessResponse.OK;
    }
}
