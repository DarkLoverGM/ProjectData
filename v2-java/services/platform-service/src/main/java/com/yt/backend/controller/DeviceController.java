package com.yt.backend.controller;

import com.yt.backend.auth.DeviceIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** POST /api/device: cap device id cho frontend (issueDeviceId trong middleware/deviceId.js) */
@RestController
public class DeviceController {

    public record DeviceResponse(String deviceId) {}

    private final DeviceIds deviceIds;

    public DeviceController(DeviceIds deviceIds) {
        this.deviceIds = deviceIds;
    }

    /** Tra id dang co, chua co thi cap moi (so lan cap moi / IP bi gioi han o RateLimitFilter) */
    @PostMapping("/api/device")
    public DeviceResponse issue(HttpServletRequest req, HttpServletResponse res) {
        String token = DeviceIds.of(req);
        if (token == null) {
            token = deviceIds.issue();
            deviceIds.setCookie(res, token);
        }
        return new DeviceResponse(token);
    }
}
