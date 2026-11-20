package com.yt.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yt.backend.auth.AuthUser;
import com.yt.backend.common.ApiException;
import com.yt.backend.common.FileStorage;
import com.yt.backend.dto.ErrorResponse;
import com.yt.backend.dto.MessageResponse;
import com.yt.backend.dto.comment.*;
import com.yt.backend.dto.video.*;
import com.yt.backend.entity.Comment;
import com.yt.backend.service.*;
import com.yt.backend.websocket.AdminEvent;
import com.yt.backend.websocket.AdminRealtime;
import com.yt.backend.websocket.Realtime;
import com.yt.backend.websocket.RealtimeEvent;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * /api/videos/* va /api/video/* (controllers/video/videoCoreController.js,
 * videoReactionController.js, videoCommentController.js, videoLibraryController.js).
 * Phu de nam o SubtitleController.
 */
@RestController
@RequestMapping({"/api/videos", "/api/video"})
public class VideoController {

    private static final Logger log = LoggerFactory.getLogger(VideoController.class);

    // Gioi han dung luong theo dinh dang (FE cung kiem tra giong vay trong AddVideo.js, Studio.js)
    private static final long MB = 1024L * 1024;
    private static final long MAX_SHORT_SIZE = 200 * MB;
    private static final long MAX_MAIN_SIZE = 2048 * MB;
    private static final long MAX_THUMB_SIZE = 5 * MB;

    /**
     * Kiem tra dinh dang + dung luong file da nhan (checkSizes trong utils/videoUpload.js).
     * @throws ApiException 400 sai dinh dang, 413 qua lon
     */
    public static void checkFiles(MultipartFile video, MultipartFile thumb, String group) {
        FileStorage.checkType(video, "video");
        FileStorage.checkType(thumb, "thumb");
        if (video != null && !video.isEmpty() && video.getSize() > ("short".equals(group) ? MAX_SHORT_SIZE : MAX_MAIN_SIZE)) {
            throw new ApiException(413, "short".equals(group) ? "Short tối đa 200MB" : "Video dài tối đa 2GB");
        }
        if (thumb != null && !thumb.isEmpty() && thumb.getSize() > MAX_THUMB_SIZE) {
            throw new ApiException(413, "Thumbnail tối đa 5MB");
        }
    }

    private final VideoService videoService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final CommentService commentService;
    private final SubtitleService subtitleService;
    private final MembershipService membershipService;
    private final Realtime realtime;
    private final AdminRealtime adminRealtime;
    private final FileStorage fileStorage;
    private final ObjectMapper mapper;

    public VideoController(VideoService videoService, UserService userService, NotificationService notificationService,
                           CommentService commentService, SubtitleService subtitleService,
                           MembershipService membershipService, Realtime realtime, AdminRealtime adminRealtime,
                           FileStorage fileStorage,
                           ObjectMapper mapper) {
        this.videoService = videoService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.commentService = commentService;
        this.subtitleService = subtitleService;
        this.membershipService = membershipService;
        this.realtime = realtime;
        this.adminRealtime = adminRealtime;
        this.fileStorage = fileStorage;
        this.mapper = mapper;
    }

    /** Video rieng tu: chi nguoi dang va admin thay, nguoi khac coi nhu khong ton tai */
    private VideoDto requireVideo(AuthUser user, String slug, String id) {
        VideoDto video = videoService.getVideo(slug, id);
        if (video == null) throw ApiException.notFound("Video không tồn tại");
        if (video.isPrivate() && !(user != null && (user.id().equals(video.uploaderId()) || "admin".equals(user.role())))) {
            throw ApiException.notFound("Video không tồn tại");
        }
        return video;
    }

    // ================= CORE =================

    /** multipart: title, description, group (main | short), tags, subtitles + file `video`, `thumb` */
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public UploadVideoResponse upload(@AuthenticationPrincipal AuthUser user, @Valid @ModelAttribute UploadVideoRequest body,
                                      @RequestParam(required = false) MultipartFile video,
                                      @RequestParam(required = false) MultipartFile thumb) {
        String group = "short".equals(body.group()) ? "short" : "main";
        if (video == null || video.isEmpty() || thumb == null || thumb.isEmpty()) {
            throw ApiException.badRequest("Thiếu file upload (video hoặc thumbnail)");
        }
        checkFiles(video, thumb, group);
        // Kiem tra trung tieu de (so theo slug vi trang xem tim theo slug)
        if (videoService.slugTaken(body.title())) throw ApiException.badRequest("Tiêu đề video đã tồn tại");

        // Phu de (khong bat buoc): chuoi JSON { vi: [cues], en: [cues] }, FE da doc file .srt/.vtt thanh cues
        JsonNode subtitleMap;
        try {
            subtitleMap = body.subtitles() == null || body.subtitles().isEmpty() ? null : mapper.readTree(body.subtitles());
        } catch (Exception e) {
            throw ApiException.badRequest("Dữ liệu phụ đề không hợp lệ");
        }

        // Local -> "/video/123.mp4", co Firebase -> URL Firebase Storage (xem common/FileStorage)
        List<String> stored = new ArrayList<>();
        try {
            String src = fileStorage.store(video, "video");
            stored.add(src);
            String thumbSrc = fileStorage.store(thumb, "thumb");
            stored.add(thumbSrc);
            VideoDto created = videoService.uploadVideo(body.title().trim(), body.description(), src, thumbSrc, user, group, body.tags());

            // Video da dang xong: phu de loi thi chi bao lai, nguoi dang sua sau o trang soan phu de
            List<String> subtitleErrors = new ArrayList<>();
            if (subtitleMap != null && subtitleMap.isObject()) {
                for (Iterator<Map.Entry<String, JsonNode>> it = subtitleMap.fields(); it.hasNext(); ) {
                    Map.Entry<String, JsonNode> e = it.next();
                    try {
                        subtitleService.saveSubtitles(created.id(), e.getKey(), e.getValue(), user.id());
                    } catch (ApiException err) {
                        subtitleErrors.add(e.getKey() + ": " + err.getMessage());
                    }
                }
            }
            notificationService.sendNotificationVideo(body.title(), user.id());
            adminRealtime.publish(new AdminEvent.VideoNew(created.id(), created.info().title(), user.username()));
            return new UploadVideoResponse("Tải video thành công", created, subtitleErrors);
        } catch (ApiException e) {
            // Loi co ly do cho nguoi dung (vd. thumbnail hong / qua lon khi doi sang WebP): giu nguyen ma + thong bao
            stored.forEach(fileStorage::removeStored);
            throw e;
        } catch (Exception e) {
            log.error("Upload error", e);
            stored.forEach(fileStorage::removeStored);
            throw new ApiException(500, "Lỗi server");
        }
    }

    /**
     * Video chi danh cho hoi vien: chi chu kenh, admin va hoi vien con han moi nhan duoc link file,
     * nguoi khac nhan `locked: true` de frontend hien nut tham gia hoi vien.
     */
    @GetMapping("/watch/{slug}/{id}")
    public WatchResponse watch(@AuthenticationPrincipal AuthUser user, @PathVariable String slug, @PathVariable String id) {
        VideoDto video = requireVideo(user, slug, id);
        if (!video.membersOnly()) return new WatchResponse(video, false);

        String channelId = video.uploaderId();
        boolean allowed = user != null && (user.id().equals(channelId) || "admin".equals(user.role())
                || membershipService.isMember(user.id(), channelId));
        return allowed ? new WatchResponse(video.withSrc(videoService.getVideoSrc(video.id())), false)
                : new WatchResponse(video, true);
    }

    @GetMapping("/user/{id}")
    public VideoListResponse userVideos(@PathVariable String id) {
        return new VideoListResponse(videoService.getAllVideosByUser(id));
    }

    /** Tat ca video, bo video cua nguoi dang bi user chan. /more giu lai cho code cu. */
    @GetMapping({"/all", "/more"})
    public VideosResponse all(@AuthenticationPrincipal AuthUser user) {
        GroupedVideos data = videoService.getAllVideos();
        if (user != null) data = data.withoutUploaders(userService.getBlockedUsers(user.id()));
        return new VideosResponse(data);
    }

    /** ?exclude=id1,id2 : video vua xem, de next khong quay lai */
    private static List<String> parseExclude(String q) {
        if (q == null) return List.of();
        return Arrays.stream(q.split(",")).map(String::trim).filter(x -> !x.isEmpty()).limit(50).toList();
    }

    @GetMapping("/next/{videoId}")
    public VideoDto next(@PathVariable String videoId, @RequestParam(required = false) String exclude) {
        VideoDto video = videoService.getNextVideo(videoId, parseExclude(exclude));
        if (video == null) throw ApiException.notFound("Không tìm thấy video tiếp theo");
        return video;
    }

    @GetMapping("/prev/{videoId}")
    public VideoDto prev(@PathVariable String videoId, @RequestParam(required = false) String exclude) {
        VideoDto video = videoService.getPrevVideo(videoId, parseExclude(exclude));
        if (video == null) throw ApiException.notFound("Không tìm thấy video trước đó");
        return video;
    }

    // ================= REACTIONS =================

    /** action: view (khach cung duoc) | like | unlike | dislike | undislike (can dang nhap) */
    @PostMapping("/viewlike/{slug}/{id}/{action}")
    public ResponseEntity<?> react(@AuthenticationPrincipal AuthUser user, @PathVariable String slug,
                                   @PathVariable String id, @PathVariable String action) {
        VideoDto video = requireVideo(user, slug, id);

        if ("view".equals(action)) {
            VideoDto updated = videoService.incrementViewCount(video.id());
            realtime.broadcastVideoStats(updated);
            return ResponseEntity.ok(new ViewResponse(updated.stats().view(), updated.stats().like(), updated.stats().dislike()));
        }
        if (user == null) throw new ApiException(401, "Vui lòng đăng nhập để thả cảm xúc");

        VideoDto updated = videoService.applyReaction(video.id(), user.id(), action);
        if (updated == null) throw ApiException.badRequest("Hành động không hợp lệ");
        // So dem cho moi nguoi dang xem, userReaction chi tra rieng cho nguoi bam
        realtime.broadcastVideoStats(updated);
        return ResponseEntity.ok(ReactionResponse.of(updated.stats(), videoService.getUserReaction(video.id(), user.id())));
    }

    @GetMapping("/viewlike/{slug}/{id}/stat")
    public ReactionResponse stat(@AuthenticationPrincipal AuthUser user, @PathVariable String slug, @PathVariable String id) {
        VideoDto video = requireVideo(user, slug, id);
        String reaction = user == null ? null : videoService.getUserReaction(video.id(), user.id());
        return ReactionResponse.of(video.stats(), reaction);
    }

    // ================= COMMENTS =================

    @GetMapping("/comments/{videoId}")
    public CommentsResponse comments(@AuthenticationPrincipal AuthUser user, @PathVariable String videoId) {
        return new CommentsResponse(commentService.getComments(videoId, user == null ? null : user.id()));
    }

    /** like / unlike / dislike / undislike tren 1 binh luan */
    @PostMapping("/comments/{commentId}/reaction/{action}")
    @PreAuthorize("isAuthenticated()")
    public CommentReactionResponse reactComment(@AuthenticationPrincipal AuthUser user, @PathVariable String commentId,
                                                @PathVariable String action) {
        Comment comment = commentService.getCommentById(commentId);
        if (comment == null) throw ApiException.notFound("Bình luận không tồn tại");
        CommentReactionResponse stats = commentService.reactComment(commentId, user.id(), action);
        if (stats == null) throw ApiException.badRequest("Hành động không hợp lệ");

        // Chi gui so dem: userReaction la cua rieng tung nguoi
        realtime.broadcast(comment.getVideoId(),
                new RealtimeEvent.CommentReaction(comment.getVideoId(), commentId, stats.like(), stats.dislike()));
        return stats;
    }

    @PostMapping("/comments/{videoId}")
    @PreAuthorize("isAuthenticated()")
    public CommentDto postComment(@AuthenticationPrincipal AuthUser user, @PathVariable String videoId,
                                  @Valid @RequestBody PostCommentRequest body) {
        CommentDto comment = commentService.postComment(videoId, user.id(), body.text(), body.parentId());
        // Day toi nguoi khac dang xem video (nguoi gui cung nhan, FE tu bo trung theo id)
        realtime.broadcast(videoId, new RealtimeEvent.CommentNew(videoId, comment));
        return comment;
    }

    @PostMapping("/blockComment/{commentId}/{videoId}")
    @PreAuthorize("isAuthenticated()")
    public MessageResponse reportComment(@AuthenticationPrincipal AuthUser user, @PathVariable String commentId,
                                         @PathVariable String videoId, @Valid @RequestBody ReportCommentRequest body) {
        if (commentService.getCommentById(commentId) == null) throw ApiException.notFound("Bình luận không tồn tại");
        CommentReportDto report = commentService.reportComment(commentId, user.id(), videoId, body.reason(), body.detail());
        adminRealtime.publish(new AdminEvent.CommentReported(report.id(), commentId, videoId, report.reason()));
        return MessageResponse.ok("Báo cáo bình luận thành công");
    }

    // ================= LIBRARY =================

    @GetMapping("/library")
    @PreAuthorize("isAuthenticated()")
    public GroupedVideos library(@AuthenticationPrincipal AuthUser user) {
        return videoService.getLibraryVideos(user);
    }

    @GetMapping("/library/{videoId}")
    @PreAuthorize("isAuthenticated()")
    public LibraryStatusResponse inLibrary(@AuthenticationPrincipal AuthUser user, @PathVariable String videoId) {
        return new LibraryStatusResponse(null, videoService.isVideoInLibrary(user, videoId));
    }

    @PostMapping("/library")
    @PreAuthorize("isAuthenticated()")
    public LibraryStatusResponse addToLibrary(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody LibraryRequest body) {
        String reason = videoService.addVideoToLibrary(user, body.videoId());
        if (reason != null) {
            throw new ApiException("VIDEO_NOT_FOUND".equals(reason) ? 404 : 400,
                    ErrorResponse.of(reason, "Không thể thêm vào thư viện"));
        }
        return new LibraryStatusResponse("Đã thêm vào thư viện", true);
    }

    @DeleteMapping("/library/{videoId}")
    @PreAuthorize("isAuthenticated()")
    public LibraryStatusResponse removeFromLibrary(@AuthenticationPrincipal AuthUser user, @PathVariable String videoId) {
        videoService.removeVideoFromLibrary(user, videoId);
        return new LibraryStatusResponse("Đã xoá khỏi thư viện", false);
    }
}
