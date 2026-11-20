package com.yt.backend.auth;

import com.yt.backend.entity.User;
import com.yt.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Danh sach tai khoan dang bi khoa, giu trong RAM de JwtAuthenticationFilter kiem tra moi request
 * ma khong phai query DB (utils/banList.js). Nguon goc van la users.banned_until trong DB:
 * - load() khi khoi dong server (sau khi tao index)
 * - set()/clear() khi admin khoa/mo khoa (AdminService)
 * Nho vay access token con han cua nguoi bi khoa cung bi chan ngay, khong phai doi het 15 phut.
 */
@Component
public class BanList {

    private static final Logger log = LoggerFactory.getLogger(BanList.class);

    // Khoa vinh vien = banned_until rat xa, de moi query chi can so sanh ngay
    public static final Date PERMANENT = Date.from(ZonedDateTime.of(9999, 12, 31, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant());

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    // Giong toLocaleString('vi-VN'): "20:30:00 20/10/2026"
    private static final DateTimeFormatter VI_DATE_TIME = DateTimeFormatter.ofPattern("HH:mm:ss d/M/yyyy").withZone(VN);

    /** userId -> thoi diem het khoa (ms) */
    private final Map<String, Long> banned = new ConcurrentHashMap<>();
    private final UserRepository users;

    public BanList(UserRepository users) {
        this.users = users;
    }

    /** Thong tin khoa hien tai (cung la DTO tra ve trong JSON: { until, permanent, reason }) */
    public record Ban(Date until, boolean permanent, String reason) {}

    @EventListener(ApplicationReadyEvent.class)
    @Order(10)
    public void loadOnStartup() {
        try {
            load();
        } catch (RuntimeException err) {
            log.error("Khong nap duoc danh sach tai khoan bi khoa: {}", err.getMessage());
        }
    }

    public int load() {
        Map<String, Long> fresh = new ConcurrentHashMap<>();
        for (User u : users.findByBannedUntilAfter(new Date())) {
            if (u.getId() != null) fresh.put(u.getId(), u.getBannedUntil().getTime());
        }
        banned.clear();
        banned.putAll(fresh);
        log.info("Da nap {} tai khoan dang bi khoa", banned.size());
        return banned.size();
    }

    public void set(String userId, Date until) {
        banned.put(String.valueOf(userId), until.getTime());
    }

    public void clear(String userId) {
        banned.remove(String.valueOf(userId));
    }

    public boolean isBanned(String userId) {
        if (userId == null) return false;
        Long until = banned.get(userId);
        if (until == null) return false;
        if (until > System.currentTimeMillis()) return true;
        banned.remove(userId); // het han khoa thi tu go
        return false;
    }

    /** Thong tin khoa cua user, null neu khong bi khoa */
    public static Ban activeBan(User user) {
        if (user == null || user.getBannedUntil() == null || !user.getBannedUntil().after(new Date())) return null;
        Date until = user.getBannedUntil();
        return new Ban(until, !until.before(PERMANENT), user.getBanReason() == null ? "" : user.getBanReason());
    }

    /** "Tai khoan da bi khoa den 20:30:00 20/10/2026. Ly do: spam" */
    public static String banMessage(Ban ban) {
        String when = ban.permanent() ? "vĩnh viễn" : "đến " + VI_DATE_TIME.format(ban.until().toInstant());
        return "Tài khoản đã bị khoá " + when + (ban.reason().isEmpty() ? "" : ". Lý do: " + ban.reason());
    }
}
