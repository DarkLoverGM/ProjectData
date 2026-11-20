package com.yt.backend.controller;

import com.mongodb.ConnectionString;
import com.yt.backend.auth.AuthCookies;
import com.yt.backend.auth.AuthUser;
import com.yt.backend.auth.JwtService;
import com.yt.backend.common.ApiException;
import com.yt.backend.common.FileStorage;
import com.yt.backend.common.VideoRules;
import com.yt.backend.dto.PageResponse;
import com.yt.backend.dto.Paging;
import com.yt.backend.dto.SuccessResponse;
import com.yt.backend.dto.admin.*;
import com.yt.backend.entity.Comment;
import com.yt.backend.service.AdminService;
import com.yt.backend.service.TrackService;
import com.yt.backend.websocket.AdminEvent;
import com.yt.backend.websocket.AdminRealtime;
import com.yt.backend.websocket.Realtime;
import com.yt.backend.websocket.UserRealtime;
import com.yt.backend.websocket.RealtimeEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * Moi route /api/admin/* (controllers/AdminController.js). Chi role admin: chan 2 lop,
 * SecurityConfig (/api/admin/**) va @PreAuthorize tren class nay.
 * Moi thao tac thay doi du lieu deu ghi nhat ky (GET /audit).
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final TrackService trackService;
    private final JwtService jwt;
    private final AuthCookies cookies;
    private final Realtime realtime;
    private final AdminRealtime adminRealtime;
    private final UserRealtime userRealtime;
    private final int port;
    private final String clientUrl;
    private final ConnectionString mongoUri;

    public AdminController(AdminService adminService, TrackService trackService, JwtService jwt, AuthCookies cookies,
                           Realtime realtime, AdminRealtime adminRealtime, UserRealtime userRealtime,
                           @Value("${server.port}") int port,
                           @Value("${app.client-url}") String clientUrl,
                           @Value("${spring.data.mongodb.uri}") String mongoUri) {
        this.adminService = adminService;
        this.trackService = trackService;
        this.jwt = jwt;
        this.cookies = cookies;
        this.realtime = realtime;
        this.adminRealtime = adminRealtime;
        this.userRealtime = userRealtime;
        this.port = port;
        this.clientUrl = clientUrl;
        this.mongoUri = new ConnectionString(mongoUri);
    }

    /** Ghi audit log cho thao tac cua admin dang dang nhap, dong thoi bao cho admin khac dang mo trang quan tri */
    private void audit(HttpServletRequest req, AuthUser admin, String action, String targetType, String targetId,
                       String targetLabel, Map<String, Object> detail) {
        String forwarded = req.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isEmpty() ? forwarded : req.getRemoteAddr();
        adminService.audit(admin, action, targetType, targetId, targetLabel, detail, ip);
        adminRealtime.publish(new AdminEvent.AdminAction(action, targetType, targetId, targetLabel, admin.id(), admin.username()));
    }

    /** Chi tiet nhat ky (du lieu tu do, khong co dinh dang chung nen dung Map) */
    private static Map<String, Object> detail(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    // Bao cho nguoi dang xem video rang binh luan da bi go
    private void broadcastCommentDeleted(String videoId, String commentId) {
        if (videoId != null) realtime.broadcast(videoId, new RealtimeEvent.CommentDeleted(videoId, commentId));
    }

    private static void notSelf(AuthUser admin, String id, String message) {
        if (id.equals(admin.id())) throw ApiException.badRequest(message);
    }

    // ================= DASHBOARD =================

    @GetMapping("/stats")
    public StatsResponse stats() {
        return adminService.getStats();
    }

    /** So lieu theo ngay + bang xep hang. ?days=7|30|90 */
    @GetMapping("/analytics")
    public AnalyticsResponse analytics(@RequestParam(required = false) String days) {
        return adminService.getAnalytics(days);
    }

    @GetMapping("/system")
    public SystemResponse system() {
        Runtime rt = Runtime.getRuntime();
        return new SystemResponse(
                "Java " + System.getProperty("java.version"),
                System.getProperty("os.name").toLowerCase(),
                ProcessHandle.current().pid(),
                Math.round(ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0),
                new SystemResponse.Memory(rt.totalMemory(), rt.totalMemory() - rt.freeMemory(), rt.maxMemory()),
                new SystemResponse.Config(port, String.join(",", mongoUri.getHosts()), mongoUri.getDatabase(),
                        jwt.accessExpires(), jwt.refreshExpires(), cookies.isSecure()));
    }

    // ================= USERS =================

    @GetMapping("/users")
    public PageResponse<AdminUserRow> users(@RequestParam(defaultValue = "") String search,
                                            @RequestParam(required = false) String state,
                                            @RequestParam(required = false) String page,
                                            @RequestParam(required = false) String limit) {
        return adminService.listUsers(search, state, Paging.of(page, limit));
    }

    @PatchMapping("/users/{id}/role")
    public SuccessResponse setRole(@AuthenticationPrincipal AuthUser admin, @PathVariable String id,
                                   @Valid @RequestBody RoleRequest body, HttpServletRequest req) {
        // Khong cho tu ha quyen chinh minh, tranh khoa luon trang admin
        notSelf(admin, id, "Không thể đổi role của chính bạn");
        adminService.setUserRole(id, body.role());
        // Bi ha quyen thi ngat ngay khoi /ws/admin, khong doi access token het han
        if (!"admin".equals(body.role())) adminRealtime.closeUser(id);
        audit(req, admin, "user.role", "user", id, null, detail("role", body.role()));
        return SuccessResponse.OK;
    }

    @GetMapping("/users/{id}")
    public UserDetailResponse userDetail(@PathVariable String id) {
        return adminService.getUserDetail(id);
    }

    /** body { days: 0 (vinh vien) | so ngay, reason } */
    @PostMapping("/users/{id}/ban")
    public BanResponse ban(@AuthenticationPrincipal AuthUser admin, @PathVariable String id,
                           @Valid @RequestBody BanRequest body, HttpServletRequest req) {
        notSelf(admin, id, "Không thể tự khoá chính bạn");
        BanResponse result = adminService.banUser(id, body);
        userRealtime.closeUser(id); // khong doi access token het han moi mat ket noi
        double days = body.days();
        audit(req, admin, "user.ban", "user", id, result.username(), detail(
                "days", days == Math.rint(days) ? (Object) (long) days : (Object) days,
                "reason", body.reason() == null ? "" : body.reason(),
                "resolvedReports", result.resolvedReports()));
        return result;
    }

    @DeleteMapping("/users/{id}/ban")
    public SuccessResponse unban(@AuthenticationPrincipal AuthUser admin, @PathVariable String id, HttpServletRequest req) {
        adminService.unbanUser(id);
        audit(req, admin, "user.unban", "user", id, null, null);
        return SuccessResponse.OK;
    }

    /** Xoa han tai khoan cung video, binh luan, follow... cua no (AdminService.deleteUser) */
    @DeleteMapping("/users/{id}")
    public DeleteUserResponse deleteUser(@AuthenticationPrincipal AuthUser admin, @PathVariable String id,
                                         HttpServletRequest req) {
        notSelf(admin, id, "Không thể tự xoá chính bạn");
        AdminService.DeletedUser result = adminService.deleteUser(id);
        userRealtime.closeUser(id);
        result.comments().removed().forEach(c -> broadcastCommentDeleted(c.getVideoId(), c.getId()));
        audit(req, admin, "user.delete", "user", id, result.username(),
                detail("videos", result.videos(), "comments", result.comments().deleted()));
        return new DeleteUserResponse(true, result.username(), result.videos(), result.comments().deleted());
    }

    /** Don spam: xoa moi binh luan cua user */
    @DeleteMapping("/users/{id}/comments")
    public DeletedResponse purgeComments(@AuthenticationPrincipal AuthUser admin, @PathVariable String id,
                                         HttpServletRequest req) {
        AdminService.PurgedComments result = adminService.purgeUserComments(id);
        for (Comment c : result.removed()) broadcastCommentDeleted(c.getVideoId(), c.getId());
        audit(req, admin, "user.purge_comments", "user", id, null, detail("deleted", result.deleted()));
        return new DeletedResponse(true, result.deleted());
    }

    /** An / hien toan bo video cua user. body { status: 0 | 1 } */
    @PatchMapping("/users/{id}/videos/status")
    public ModifiedResponse setUserVideosStatus(@AuthenticationPrincipal AuthUser admin, @PathVariable String id,
                                                @Valid @RequestBody StatusRequest body, HttpServletRequest req) {
        ModifiedResponse result = adminService.setUserVideosStatus(id, body.status());
        audit(req, admin, "user.videos_status", "user", id, null, detail("status", body.status(), "modified", result.modified()));
        return result;
    }

    // ================= VIDEOS =================

    @GetMapping("/videos")
    public PageResponse<AdminVideoRow> videos(@RequestParam(defaultValue = "") String search,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String moderation,
                                              @RequestParam(required = false) String page,
                                              @RequestParam(required = false) String limit) {
        return adminService.listVideos(search, status, moderation, Paging.of(page, limit));
    }

    /** Tieu chuan cong dong de chon khi chan video */
    @GetMapping("/video-rules")
    public List<VideoRules.Rule> videoRules() {
        return adminService.getVideoRules();
    }

    /** Du lieu cho man duyet video. reviewUrl: trang player cua frontend (src/pages/admin/VideoReview.js) */
    @GetMapping("/videos/{id}")
    public VideoReviewResponse videoForReview(@PathVariable String id) {
        return adminService.getVideoForReview(id, clientUrl + "/review/" + FileStorage.encodeURIComponent(id));
    }

    /** body { decision: 'block' | 'approve', rules: [...], note, atTime (giay) } */
    @PostMapping("/videos/{id}/moderate")
    public ModerateResponse moderate(@AuthenticationPrincipal AuthUser admin, @PathVariable String id,
                                     @Valid @RequestBody ModerateRequest body, HttpServletRequest req) {
        ModerateResponse result = adminService.moderateVideo(id, body, admin);
        audit(req, admin, "video." + body.decision(), "video", id, result.title(), detail(
                "rules", result.moderation().rules(), "note", result.moderation().note(),
                "atTime", result.moderation().atTime()));
        return result;
    }

    /** Tra loi nguoi dang (ho nhan tu Studio). body { content } */
    @PostMapping("/videos/{id}/messages")
    public ReplyResponse replyMessage(@AuthenticationPrincipal AuthUser admin, @PathVariable String id,
                                      @Valid @RequestBody ContentRequest body, HttpServletRequest req) {
        ReplyResponse result = adminService.replyVideoMessage(id, body.content(), admin);
        adminRealtime.publish(new AdminEvent.VideoMessage(result.item()));
        audit(req, admin, "video.reply", "video", id, result.title(), detail("content", result.item().content()));
        return result;
    }

    /** Hang loat: body { ids: [...], status }. Khai bao truoc /videos/:id/status */
    @PatchMapping("/videos/status")
    public ModifiedResponse setVideosStatus(@AuthenticationPrincipal AuthUser admin, @Valid @RequestBody BulkStatusRequest body,
                                            HttpServletRequest req) {
        ModifiedResponse result = adminService.setVideosStatus(body.ids(), body.status());
        audit(req, admin, "video.bulk_status", "video", null, null, detail("ids", body.ids(), "status", body.status()));
        return result;
    }

    @PatchMapping("/videos/{id}/status")
    public SuccessResponse setVideoStatus(@AuthenticationPrincipal AuthUser admin, @PathVariable String id,
                                          @Valid @RequestBody StatusRequest body, HttpServletRequest req) {
        adminService.setVideoStatus(id, body.status());
        audit(req, admin, "video.status", "video", id, null, detail("status", body.status()));
        return SuccessResponse.OK;
    }

    // ================= COMMENT REPORTS =================

    @GetMapping("/reports/comments")
    public PageResponse<CommentReportRow> commentReports(@RequestParam(required = false) String status,
                                                         @RequestParam(required = false) String page,
                                                         @RequestParam(required = false) String limit) {
        return adminService.listCommentReports(status, Paging.of(page, limit));
    }

    @PatchMapping("/reports/comments/{id}/status")
    public SuccessResponse setReportStatus(@AuthenticationPrincipal AuthUser admin, @PathVariable String id,
                                           @Valid @RequestBody StatusRequest body, HttpServletRequest req) {
        adminService.setCommentReportStatus(id, body.status());
        audit(req, admin, "report_comment.status", "report", id, null, detail("status", body.status()));
        return SuccessResponse.OK;
    }

    /** { success, videoId }: video chua binh luan vua xoa */
    public record DeleteCommentResponse(boolean success, String videoId) {}

    @DeleteMapping("/comments/{id}")
    public DeleteCommentResponse deleteComment(@AuthenticationPrincipal AuthUser admin, @PathVariable String id,
                                               HttpServletRequest req) {
        String videoId = adminService.deleteComment(id);
        // Go binh luan khoi man hinh nguoi dang xem video
        broadcastCommentDeleted(videoId, id);
        audit(req, admin, "comment.delete", "comment", id, null, detail("videoId", videoId));
        return new DeleteCommentResponse(true, videoId);
    }

    // ================= USER REPORTS =================

    @GetMapping("/reports/users")
    public PageResponse<UserReportRow> userReports(@RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String page,
                                                   @RequestParam(required = false) String limit) {
        return adminService.listUserReports(status, Paging.of(page, limit));
    }

    /** body { status: 'pending' | 'resolved' | 'dismissed' } */
    @PatchMapping("/reports/users/{id}/status")
    public SuccessResponse setUserReportStatus(@AuthenticationPrincipal AuthUser admin, @PathVariable String id,
                                               @Valid @RequestBody UserReportStatusRequest body, HttpServletRequest req) {
        adminService.setUserReportStatus(id, body.status());
        audit(req, admin, "report_user.status", "report", id, null, detail("status", body.status()));
        return SuccessResponse.OK;
    }

    // ================= BROADCAST =================

    /** body { title, content, audience: 'all' | 'admins' | 'user', userId? } */
    @PostMapping("/notifications/broadcast")
    public SentResponse broadcast(@AuthenticationPrincipal AuthUser admin, @Valid @RequestBody BroadcastRequest body,
                                  HttpServletRequest req) {
        SentResponse result = adminService.broadcast(body);
        audit(req, admin, "notification.broadcast", "user", body.userId(), null, detail(
                "audience", body.audience(), "title", body.title(), "sent", result.sent()));
        return result;
    }

    // ================= AUDIT =================

    @GetMapping("/audit")
    public AuditPageResponse auditLog(@RequestParam(required = false) String action,
                                      @RequestParam(required = false) String admin,
                                      @RequestParam(required = false) String target,
                                      @RequestParam(required = false) String page,
                                      @RequestParam(required = false) String limit) {
        return adminService.listAudit(action, admin, target, Paging.of(page, limit));
    }

    // ================= LOGS =================

    /** Log truy cap ghi boi POST /api/track, moi nhat truoc */
    @GetMapping("/logs")
    public LogsResponse logs(@RequestParam(required = false) String limit) {
        int max = Math.max(Math.min(com.yt.backend.common.Json.parseIntLoose(limit, 200), 1000), 0);
        try {
            List<Map<String, Object>> logs = trackService.readLogs();
            List<Map<String, Object>> items = new ArrayList<>(logs.subList(Math.max(0, logs.size() - max), logs.size()));
            Collections.reverse(items);
            return new LogsResponse(items, logs.size());
        } catch (IOException e) {
            throw new ApiException(500, "Không đọc được file log");
        }
    }

    @DeleteMapping("/logs")
    public SuccessResponse clearLogs(@AuthenticationPrincipal AuthUser admin, HttpServletRequest req) throws IOException {
        trackService.clear();
        audit(req, admin, "logs.clear", null, null, null, null);
        return SuccessResponse.OK;
    }
}
