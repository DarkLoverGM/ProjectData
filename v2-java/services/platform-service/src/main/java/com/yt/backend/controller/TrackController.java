package com.yt.backend.controller;

import com.yt.backend.common.ApiException;
import com.yt.backend.common.ClientIp;
import com.yt.backend.dto.MessageResponse;
import com.yt.backend.service.TrackService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** GET / (chao admin), POST /api/track (ghi log truy cap), GET /api/log (chi admin) */
@RestController
public class TrackController {

    private final TrackService trackService;

    public TrackController(TrackService trackService) {
        this.trackService = trackService;
    }

    @GetMapping("/")
    public MessageResponse root() {
        return MessageResponse.ok("Chào admin!");
    }

    @PostMapping("/api/track")
    public MessageResponse track(HttpServletRequest req) throws IOException {
        String referrer = req.getHeader("Referer");
        // log.json la file tu do (dung chung voi ban Node), moi dong 1 object
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("ip", ClientIp.of(req));
        entry.put("userAgent", req.getHeader("User-Agent"));
        entry.put("referrer", referrer == null || referrer.isEmpty() ? "Direct" : referrer);
        entry.put("timestamp", Instant.now().toString());
        trackService.saveLog(entry);
        return MessageResponse.ok("Logged");
    }

    /** Log chua IP + trinh duyet cua moi nguoi xem -> chi admin duoc doc (truoc day ai cung doc duoc) */
    @GetMapping("/api/log")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> logs() {
        try {
            return trackService.readLogs();
        } catch (IOException e) {
            throw new ApiException(500, "Failed to read logs");
        }
    }
}
