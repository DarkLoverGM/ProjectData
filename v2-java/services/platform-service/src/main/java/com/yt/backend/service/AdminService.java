package com.yt.backend.service;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.auth.BanList;
import com.yt.backend.common.ApiException;
import com.yt.backend.common.FileStorage;
import com.yt.backend.common.MongoUtils;
import com.yt.backend.common.VideoRules;
import com.yt.backend.dto.PageResponse;
import com.yt.backend.dto.Paging;
import com.yt.backend.dto.admin.*;
import com.yt.backend.dto.studio.VideoMessageDto;
import com.yt.backend.dto.user.BanInfo;
import com.yt.backend.entity.*;
import com.yt.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Nghiep vu trang quan tri: thong ke, nguoi dung, duyet video, bao cao, thong bao hang loat, nhat ky */
@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private static final String TZ = "Asia/Ho_Chi_Minh";

    private final MongoTemplate mongo;
    private final UserRepository users;
    private final VideoRepository videos;
    private final CommentRepository comments;
    private final CommentReactionRepository commentReactions;
    private final CommentReportRepository commentReports;
    private final UserReportRepository userReports;
    private final VideoReactionRepository videoReactions;
    private final VideoTagRepository videoTags;
    private final TagRepository tags;
    private final FollowRepository follows;
    private final BlockedUserRepository blocked;
    private final LibraryRepository libraries;
    private final LibraryItemRepository libraryItems;
    private final NotificationRepository notifications;
    private final PasswordResetRepository resets;
    private final EmailVerificationRepository verifications;
    private final SubtitleRepository subtitles;
    private final VideoMessageRepository messages;
    private final AdminAuditRepository audits;
    private final BanList banList;
    private final VideoMessageService videoMessages;
    private final StudioService studioService;
    private final NotificationService notificationService;
    private final MembershipService memberships;
    private final FileStorage storage;

    public AdminService(MongoTemplate mongo, UserRepository users, VideoRepository videos, CommentRepository comments,
                        CommentReactionRepository commentReactions, CommentReportRepository commentReports,
                        UserReportRepository userReports, VideoReactionRepository videoReactions,
                        VideoTagRepository videoTags, TagRepository tags, FollowRepository follows,
                        BlockedUserRepository blocked, LibraryRepository libraries, LibraryItemRepository libraryItems,
                        NotificationRepository notifications, PasswordResetRepository resets,
                        EmailVerificationRepository verifications, SubtitleRepository subtitles,
                        VideoMessageRepository messages, AdminAuditRepository audits, BanList banList,
                        VideoMessageService videoMessages, StudioService studioService,
                        NotificationService notificationService, MembershipService memberships, FileStorage storage) {
        this.mongo = mongo;
        this.users = users;
        this.videos = videos;
        this.comments = comments;
        this.commentReactions = commentReactions;
        this.commentReports = commentReports;
        this.userReports = userReports;
        this.videoReactions = videoReactions;
        this.videoTags = videoTags;
        this.tags = tags;
        this.follows = follows;
        this.blocked = blocked;
        this.libraries = libraries;
        this.libraryItems = libraryItems;
        this.notifications = notifications;
        this.resets = resets;
        this.verifications = verifications;
        this.subtitles = subtitles;
        this.messages = messages;
        this.audits = audits;
        this.banList = banList;
        this.videoMessages = videoMessages;
        this.studioService = studioService;
        this.notificationService = notificationService;
        this.memberships = memberships;
        this.storage = storage;
    }

    // ==================== HELPER ====================

    /** Ket qua dem theo nhom cua aggregation: { _id, n } */
    private record CountRow(String id, long n) {}

    /** Ket qua cong don: { _id, total } */
    private record SumRow(String id, long total) {}

    /**
     * Thay cho subquery (SELECT COUNT(*) ... WHERE field = x): dem 1 lan cho nhieu gia tri.
     * @return value -> so ban ghi (khong co trong map nghia la 0)
     */
    private Map<String, Long> countBy(Class<?> entity, String field, Collection<String> values, Criteria extra) {
        if (values.isEmpty()) return Map.of();
        Criteria match = Criteria.where(field).in(values);
        if (extra != null) match = new Criteria().andOperator(match, extra);
        Aggregation agg = Aggregation.newAggregation(Aggregation.match(match), Aggregation.group(field).count().as("n"));
        return mongo.aggregate(agg, entity, CountRow.class).getMappedResults().stream()
                .collect(Collectors.toMap(CountRow::id, CountRow::n));
    }

    /** Tong luot xem cua cac video khop dieu kien */
    private long sumViews(Criteria match) {
        Aggregation agg = Aggregation.newAggregation(Aggregation.match(match), Aggregation.group().sum("view").as("total"));
        SumRow row = mongo.aggregate(agg, Video.class, SumRow.class).getUniqueMappedResult();
        return row == null ? 0 : row.total();
    }

    private Map<String, User> usersById(Collection<String> ids) {
        return users.findByIdIn(ids.stream().filter(Objects::nonNull).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }

    private Map<String, Video> videosById(Collection<String> ids) {
        return videos.findByIdIn(ids.stream().filter(Objects::nonNull).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Video::getId, Function.identity(), (a, b) -> a));
    }

    private static String username(Map<String, User> map, String id) {
        User u = id == null ? null : map.get(id);
        return u == null ? null : u.getUsername();
    }

    private static Query byId(String id) {
        return Query.query(Criteria.where("id").is(id));
    }

    private User requireUser(String userId) {
        return users.findOneById(userId).orElseThrow(() -> ApiException.badRequest("Không tìm thấy user"));
    }

    private Video requireVideo(String videoId) {
        return videos.findOneById(videoId).orElseThrow(() -> ApiException.badRequest("Không tìm thấy video"));
    }

    // ==================== DASHBOARD ====================

    public StatsResponse getStats() {
        Date now = new Date();
        List<Video> recent = mongo.find(new Query().with(Sort.by(Sort.Direction.DESC, "uploadAt")).limit(5), Video.class);
        Map<String, User> names = usersById(recent.stream().map(Video::getUserId).toList());

        return new StatsResponse(
                new StatsResponse.Users(users.count(), users.countByRole(User.ROLE_ADMIN), users.countByBannedUntilAfter(now)),
                new StatsResponse.Videos(videos.count(), videos.countByStatus(Video.VISIBLE), sumViews(new Criteria())),
                new StatsResponse.Total(comments.count()),
                new StatsResponse.Reports(commentReports.count(), commentReports.countByStatus(0),
                        userReports.countByStatus("pending"), videos.countAppealsPending()),
                new StatsResponse.Total(tags.count()),
                recent.stream().map(v -> new StatsResponse.RecentVideo(v.getId(), v.getTitle(), v.getSlug(), v.getView(),
                        v.getStatus(), v.getUploadAt(), username(names, v.getUserId()))).toList());
    }

    // ==================== USERS ====================

    public PageResponse<AdminUserRow> listUsers(String search, String state, Paging p) {
        List<Criteria> conditions = new ArrayList<>();
        if (search != null && !search.isEmpty()) {
            conditions.add(new Criteria().orOperator(MongoUtils.contains("username", search),
                    MongoUtils.contains("email", search), Criteria.where("id").is(search)));
        }
        if ("banned".equals(state)) conditions.add(Criteria.where("banned_until").gt(new Date()));
        if ("admin".equals(state)) conditions.add(Criteria.where("role").is(User.ROLE_ADMIN));
        Query query = conditions.isEmpty() ? new Query() : Query.query(new Criteria().andOperator(conditions));

        long total = mongo.count(query, User.class);
        List<User> rows = mongo.find(query.with(p.pageable(Sort.by("username"))), User.class);

        List<String> ids = rows.stream().map(User::getId).toList();
        Map<String, Long> videoCounts = countBy(Video.class, "user_id", ids, null);
        Map<String, Long> followerCounts = countBy(Follow.class, "following_id", ids, null);
        Map<String, Long> reports = countBy(UserReport.class, "reported_user", ids, Criteria.where("status").is("pending"));
        List<AdminUserRow> items = rows.stream().map(u -> new AdminUserRow(u.getId(), u.getUsername(), u.getEmail(),
                u.getAvatar(), u.getRole(), BanInfo.from(BanList.activeBan(u)), videoCounts.getOrDefault(u.getId(), 0L),
                followerCounts.getOrDefault(u.getId(), 0L), reports.getOrDefault(u.getId(), 0L))).toList();
        return PageResponse.of(items, total, p);
    }

    public void setUserRole(String userId, String role) {
        if (mongo.updateFirst(byId(userId), Update.update("role", role), User.class).getMatchedCount() == 0) {
            throw ApiException.badRequest("Không tìm thấy user");
        }
    }

    // ==================== VIDEOS ====================

    public PageResponse<AdminVideoRow> listVideos(String search, String status, String moderation, Paging p) {
        List<Criteria> conditions = new ArrayList<>();
        if (search != null && !search.isEmpty()) {
            // Thay cho LEFT JOIN USERS ... WHERE USERS.username LIKE ?: tim user truoc roi loc theo user_id
            List<String> userIds = mongo.find(Query.query(MongoUtils.contains("username", search)), User.class)
                    .stream().map(User::getId).toList();
            conditions.add(new Criteria().orOperator(MongoUtils.contains("title", search),
                    Criteria.where("user_id").in(userIds), Criteria.where("id").is(search)));
        }
        if ("0".equals(status) || "1".equals(status)) conditions.add(Criteria.where("status").is(Integer.parseInt(status)));
        // Hang doi duyet: video chua ai duyet hoac nguoi dang da sua sau khi bi chan ($nin khop ca khi khong co field)
        if ("pending".equals(moderation)) {
            conditions.add(Criteria.where("moderation.state").nin("approved", "blocked"));
        } else if (moderation != null && Set.of("approved", "blocked", "resubmitted").contains(moderation)) {
            conditions.add(Criteria.where("moderation.state").is(moderation));
        } else if ("appeal".equals(moderation)) {
            // Nguoi dang nhan tu Studio, chua co admin tra loi
            conditions.add(Criteria.where("appeal.pending").is(true));
        }
        Query query = conditions.isEmpty() ? new Query() : Query.query(new Criteria().andOperator(conditions));

        long total = mongo.count(query, Video.class);
        List<Video> rows = mongo.find(query.with(p.pageable(Sort.by(Sort.Direction.DESC, "uploadAt"))), Video.class);
        Map<String, User> names = usersById(rows.stream().map(Video::getUserId).toList());
        Map<String, Long> commentCounts = countBy(Comment.class, "video_id", rows.stream().map(Video::getId).toList(), null);

        List<AdminVideoRow> items = rows.stream().map(v -> new AdminVideoRow(v.getId(), v.getTitle(), v.getSlug(),
                v.getThumbnail(), v.getView(), v.getStatus(), v.getUploadAt(), v.getVideoGroup(), v.getUserId(),
                v.getModeration() == null ? null : new AdminVideoRow.Moderation(v.getModeration().getState(), v.getModeration().getRules()),
                v.getAppeal() == null ? null : new AdminVideoRow.Appeal(v.getAppeal().getPending()),
                username(names, v.getUserId()), commentCounts.getOrDefault(v.getId(), 0L))).toList();
        return PageResponse.of(items, total, p);
    }

    /** status: 1 = hien thi, 0 = an */
    public void setVideoStatus(String videoId, int status) {
        if (mongo.updateFirst(byId(videoId), Update.update("status", status), Video.class).getMatchedCount() == 0) {
            throw ApiException.badRequest("Không tìm thấy video");
        }
        if (status == 1) clearBlocked(Criteria.where("id").is(videoId));
    }

    // Hien lai video da bi chan bang nut An/Hien nhanh -> bo nhan "Da chan" de quay lai hang doi duyet
    private void clearBlocked(Criteria filter) {
        mongo.updateMulti(Query.query(new Criteria().andOperator(filter,
                        Criteria.where("moderation.state").in("blocked", "resubmitted"))),
                new Update().unset("moderation").unset("appeal"), Video.class);
    }

    public ModifiedResponse setVideosStatus(List<String> ids, int status) {
        long modified = mongo.updateMulti(Query.query(Criteria.where("id").in(ids)), Update.update("status", status), Video.class)
                .getModifiedCount();
        if (status == 1) clearBlocked(Criteria.where("id").in(ids));
        return new ModifiedResponse(true, modified);
    }

    // ==================== COMMENT REPORTS ====================

    public PageResponse<CommentReportRow> listCommentReports(String status, Paging p) {
        Query query = "0".equals(status) || "1".equals(status)
                ? Query.query(Criteria.where("status").is(Integer.parseInt(status)))
                : new Query();
        long total = mongo.count(query, CommentReport.class);
        List<CommentReport> rows = mongo.find(query.with(p.pageable(Sort.by(Sort.Direction.DESC, "createdAt"))), CommentReport.class);

        // Thay cho 4 LEFT JOIN: COMMENT, USERS (author), USERS (reporter), VIDEO
        Map<String, Comment> commentMap = comments.findByIdIn(rows.stream().map(CommentReport::getCommentId)
                        .filter(Objects::nonNull).toList()).stream()
                .collect(Collectors.toMap(Comment::getId, Function.identity(), (a, b) -> a));
        List<String> userIds = new ArrayList<>(rows.stream().map(CommentReport::getUserId).toList());
        commentMap.values().forEach(c -> userIds.add(c.getUserId()));
        Map<String, User> names = usersById(userIds);
        Map<String, Video> videoMap = videosById(rows.stream().map(CommentReport::getVideoId).toList());

        List<CommentReportRow> items = rows.stream().map(r -> {
            Comment c = r.getCommentId() == null ? null : commentMap.get(r.getCommentId());
            Video v = r.getVideoId() == null ? null : videoMap.get(r.getVideoId());
            return new CommentReportRow(r.getId(), r.getReason(), r.getDetail(), r.getCreatedAt(), r.getStatus(),
                    r.getCommentId(), r.getVideoId(), c == null ? null : c.getContent(),
                    c == null ? null : username(names, c.getUserId()), username(names, r.getUserId()),
                    v == null ? null : v.getTitle());
        }).toList();
        return PageResponse.of(items, total, p);
    }

    /** status: 0 = cho xu ly, 1 = da xu ly */
    public void setCommentReportStatus(String reportId, int status) {
        if (mongo.updateFirst(byId(reportId), Update.update("status", status), CommentReport.class).getMatchedCount() == 0) {
            throw ApiException.badRequest("Không tìm thấy báo cáo");
        }
    }

    /**
     * Xoa binh luan + cac reply + like/dislike cua chung, roi dong cac bao cao lien quan.
     * Mongo khong co FK ON DELETE CASCADE nhu MySQL nen phai tu xoa phan lien quan.
     * @return video chua binh luan (de controller bao cho nguoi dang xem), null neu khong ro
     */
    public String deleteComment(String commentId) {
        Comment comment = comments.findOneById(commentId).orElseThrow(() -> ApiException.badRequest("Không tìm thấy bình luận"));
        deleteCommentTrees(List.of(comment.getId()));
        return comment.getVideoId();
    }

    /**
     * Xoa cac binh luan goc + toan bo reply ben duoi (cay hien chi 2 tang nhung cu duyet het cho chac).
     * @return so binh luan da xoa
     */
    private long deleteCommentTrees(List<String> rootIds) {
        Set<String> ids = new LinkedHashSet<>(rootIds);
        List<String> frontier = new ArrayList<>(ids);
        while (!frontier.isEmpty()) {
            frontier = comments.findByParentIdIn(frontier).stream().map(Comment::getId).filter(id -> !ids.contains(id)).toList();
            ids.addAll(frontier);
        }
        long deleted = comments.deleteByIdIn(ids);
        commentReactions.deleteByCommentIdIn(ids);
        mongo.updateMulti(Query.query(Criteria.where("comment_id").in(ids)), Update.update("status", 1), CommentReport.class);
        return deleted;
    }

    // ==================== VIDEO REVIEW ====================

    public List<VideoRules.Rule> getVideoRules() {
        return VideoRules.VIDEO_RULES;
    }

    private static String textOr(String s, String fallback) {
        return s == null || s.isEmpty() ? fallback : s;
    }

    /** Video kem thong tin de admin duyet (ke ca video dang an, trang xem thuong khong tra ve) */
    public VideoReviewResponse getVideoForReview(String videoId, String reviewUrl) {
        Video v = videos.findOneById(videoId).orElseThrow(() -> ApiException.notFound("Không tìm thấy video"));
        String id = v.getId();
        String userId = v.getUserId();

        User uploader = userId == null ? null : users.findOneById(userId).orElse(null);
        List<String> tagIds = videoTags.findByVideoId(id).stream().map(VideoTag::getTagId).distinct().toList();
        Map<String, Tag> tagMap = tags.findByIdIn(tagIds).stream().collect(Collectors.toMap(Tag::getId, Function.identity(), (a, b) -> a));
        List<String> tagNames = tagIds.stream().map(tagMap::get).filter(Objects::nonNull).map(Tag::getName).toList();
        List<AuditDto> history = audits.findByTargetTypeAndTargetIdOrderByCreatedAtDesc("video", id, PageRequest.of(0, 10))
                .stream().map(AuditDto::from).toList();

        VideoReviewResponse.Uploader uploaderDto = uploader == null ? null : new VideoReviewResponse.Uploader(
                uploader.getId(), uploader.getUsername(), uploader.getAvatar(), BanInfo.from(BanList.activeBan(uploader)),
                videos.countByUserId(userId),
                // Nguoi dang tung bi chan bao nhieu video -> dau hieu tai pham
                videos.countByUserIdAndModerationState(userId, "blocked"));

        return new VideoReviewResponse(id, textOr(v.getTitle(), ""), textOr(v.getSlug(), ""), textOr(v.getDescription(), ""),
                textOr(v.getSrc(), ""), textOr(v.getThumbnail(), ""), textOr(v.getVideoGroup(), Video.GROUP_MAIN),
                v.getSpeciallist(), v.getStatus(), v.getUploadAt(), ModerationDto.from(v.getModeration()),
                AppealDto.from(v.getAppeal()), v.getMediaUpdatedAt(),
                videoMessages.list(id).stream().map(VideoMessageDto::from).toList(), tagNames,
                new VideoReviewResponse.Stats(v.getView() == null ? 0 : v.getView(),
                        videoReactions.countByVideoIdAndAction(id, "like"), videoReactions.countByVideoIdAndAction(id, "dislike"),
                        comments.countByVideoId(id)),
                uploaderDto, history, reviewUrl);
    }

    /**
     * Quyet dinh sau khi xem video.
     * - block:   an video (status 0), luu rule vi pham + moc thoi gian, bao cho nguoi dang
     * - approve: hien video (status 1), danh dau da duyet de khong vao hang doi nua
     */
    public ModerateResponse moderateVideo(String videoId, ModerateRequest req, AuthUser admin) {
        boolean block = "block".equals(req.decision());
        Video video = requireVideo(videoId);

        List<String> picked = req.rules() == null ? List.of()
                : req.rules().stream().filter(VideoRules::isRule).distinct().toList();
        String note = req.note() == null ? "" : req.note().trim();
        if (block && picked.isEmpty()) throw ApiException.badRequest("Chọn ít nhất 1 tiêu chuẩn bị vi phạm");
        if (block && picked.contains("other") && note.isEmpty()) throw ApiException.badRequest("Chọn \"Khác\" thì cần ghi chú lý do");
        Double t = req.atTime();

        Video.Moderation moderation = new Video.Moderation();
        moderation.setState(block ? "blocked" : "approved");
        moderation.setRules(block ? picked : List.of());
        moderation.setNote(note);
        moderation.setAtTime(block && t != null && Double.isFinite(t) && t >= 0 ? (long) Math.floor(t) : null);
        moderation.setBy(admin.id());
        moderation.setByName(admin.username());
        moderation.setAt(new Date());
        // Da ra quyet dinh = da phan hoi nguoi dang
        mongo.updateFirst(byId(video.getId()),
                new Update().set("status", block ? 0 : 1).set("moderation", moderation).unset("appeal"), Video.class);

        String labels = String.join(", ", picked.stream().map(VideoRules::ruleLabel).toList());
        String prevState = video.moderationState();
        boolean wasBlocked = "blocked".equals(prevState) || "resubmitted".equals(prevState);
        String title = video.getTitle();
        String noteSuffix = note.isEmpty() ? "" : " Ghi chú: " + note;
        if (block) {
            studioService.notifyCreator(video.getUserId(), video.getId(), "Video của bạn đã bị chặn",
                    "Video \"" + title + "\" vi phạm tiêu chuẩn cộng đồng: " + labels + "." + noteSuffix
                            + " Bấm để xem chi tiết, sửa video hoặc phản hồi admin.");
        } else if (wasBlocked) {
            studioService.notifyCreator(video.getUserId(), video.getId(), "Video của bạn đã được hiện lại",
                    "Video \"" + title + "\" đã được duyệt lại và hiển thị công khai." + noteSuffix);
        }
        // Ghi vao luong trao doi de nguoi dang thay moc quyet dinh ngay trong Studio
        if (block || wasBlocked) {
            videoMessages.add(video.getId(), "system", admin, block
                    ? "Video bị chặn: " + labels + "." + noteSuffix
                    : "Video đã được duyệt và hiện lại." + noteSuffix);
        }
        return new ModerateResponse(true, title, ModerationDto.from(moderation));
    }

    /** Admin tra loi nguoi dang trong man duyet video -> het "can phan hoi", nguoi dang nhan thong bao */
    public ReplyResponse replyVideoMessage(String videoId, String content, AuthUser admin) {
        Video video = requireVideo(videoId);
        VideoMessage item = videoMessages.add(video.getId(), "admin", admin, content);
        mongo.updateFirst(byId(video.getId()), new Update().unset("appeal"), Video.class);

        String text = item.getContent();
        String preview = text.length() > 160 ? text.substring(0, 160) + "…" : text;
        studioService.notifyCreator(video.getUserId(), video.getId(), "Admin đã phản hồi về video của bạn",
                "\"" + video.getTitle() + "\": " + preview);
        return new ReplyResponse(true, video.getTitle(), VideoMessageDto.from(item));
    }

    // ==================== BAN ====================

    /**
     * Khoa tai khoan. days = 0 -> vinh vien. Co hieu luc ngay nho BanList (JwtAuthenticationFilter kiem tra).
     * Cac bao cao dang cho ve user nay duoc coi nhu da xu ly.
     */
    public BanResponse banUser(String userId, BanRequest req) {
        User user = requireUser(userId);
        if (user.isAdmin()) throw ApiException.badRequest("Không thể khoá admin, hãy bỏ quyền admin trước");

        double days = req.days();
        Date until = days == 0 ? BanList.PERMANENT : new Date(System.currentTimeMillis() + (long) (days * 86_400_000L));
        String reason = req.reason() == null ? "" : req.reason().trim();
        mongo.updateFirst(byId(user.getId()), new Update().set("banned_until", until).set("ban_reason", reason), User.class);
        banList.set(user.getId(), until);
        long resolved = mongo.updateMulti(
                Query.query(Criteria.where("reported_user").is(user.getId()).and("status").is("pending")),
                new Update().set("status", "resolved").set("resolved_at", new Date()), UserReport.class).getModifiedCount();
        return new BanResponse(true, user.getUsername(), until, days == 0, resolved);
    }

    public void unbanUser(String userId) {
        if (mongo.updateFirst(byId(userId), new Update().unset("banned_until").unset("ban_reason"), User.class)
                .getMatchedCount() == 0) {
            throw ApiException.badRequest("Không tìm thấy user");
        }
        banList.clear(userId);
    }

    // ==================== USER DETAIL / PURGE ====================

    public UserDetailResponse getUserDetail(String id) {
        User user = users.findOneById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy user"));

        List<String> commentIds = comments.findByUserId(id).stream().map(Comment::getId).toList();
        List<Comment> recent = comments.findByUserIdOrderByCreatedAtDesc(id, PageRequest.of(0, 8));
        Map<String, Video> titles = videosById(recent.stream().map(Comment::getVideoId).toList());
        List<AuditDto> history = audits.findByTargetIdOrderByCreatedAtDesc(id, PageRequest.of(0, 10))
                .stream().map(AuditDto::from).toList();

        List<String> loginMethods = new ArrayList<>();
        if (user.hasPassword()) loginMethods.add("password");
        if (user.getGoogleId() != null && !user.getGoogleId().isEmpty()) loginMethods.add("google");

        return new UserDetailResponse(user.getId(), user.getUsername(), user.getEmail(), user.getAvatar(), user.getRole(),
                user.getCreatedAt(), loginMethods, BanInfo.from(BanList.activeBan(user)),
                new UserDetailResponse.Counts(videos.countByUserId(id), videos.countByUserIdAndStatus(id, Video.VISIBLE),
                        sumViews(Criteria.where("user_id").is(id)), commentIds.size(), follows.countByFollowingId(id),
                        follows.countByFollowerId(id), userReports.countByReportedUserAndStatus(id, "pending"),
                        userReports.countByReportedUser(id),
                        // Binh luan cua user nay tung bi bao cao bao nhieu lan
                        commentIds.isEmpty() ? 0 : commentReports.countByCommentIdIn(commentIds)),
                recent.stream().map(c -> {
                    Video v = titles.get(c.getVideoId());
                    return new UserDetailResponse.RecentComment(c.getId(), c.getContent(), c.getCreatedAt(), c.getVideoId(),
                            v == null ? null : v.getTitle());
                }).toList(),
                history);
    }

    /** Ket qua xoa binh luan cua 1 user: tong so da xoa (ca reply) + binh luan cua user do (de bao realtime) */
    public record PurgedComments(long deleted, List<Comment> removed) {}

    /** Xoa toan bo binh luan cua 1 user (don spam) */
    public PurgedComments purgeUserComments(String userId) {
        List<Comment> roots = comments.findByUserId(userId);
        long deleted = roots.isEmpty() ? 0 : deleteCommentTrees(roots.stream().map(Comment::getId).toList());
        return new PurgedComments(deleted, roots);
    }

    public ModifiedResponse setUserVideosStatus(String userId, int status) {
        long modified = mongo.updateMulti(Query.query(Criteria.where("user_id").is(userId)), Update.update("status", status),
                Video.class).getModifiedCount();
        if (status == 1) clearBlocked(Criteria.where("user_id").is(userId));
        return new ModifiedResponse(true, modified);
    }

    // ==================== XOA USER ====================
    // MongoDB khong co ON DELETE CASCADE nhu MySQL: xoa document trong users thi video, binh luan,
    // follow... tro toi user_id do van nam nguyen. Phai xoa qua day (giong AdminServices.deleteUser ben Node).

    /** Ket qua xoa user: so video / binh luan da xoa + binh luan (bao realtime) */
    public record DeletedUser(String username, long videos, PurgedComments comments) {}

    /** Xoa han 1 tai khoan + toan bo du lieu cua no. Lich su trong admin_audit duoc giu lai. */
    public DeletedUser deleteUser(String userId) {
        User user = requireUser(userId);
        if (user.isAdmin()) throw ApiException.badRequest("Không thể xoá admin, hãy bỏ quyền admin trước");

        long videoCount = deleteVideos(videos.findByUserId(userId, Sort.unsorted()).stream().map(Video::getId).toList());
        PurgedComments removed = purgeUserComments(userId);
        List<String> libraryIds = libraries.findByUserId(userId).stream().map(Library::getId).toList();

        videoReactions.deleteByUserId(userId);
        commentReactions.deleteByUserId(userId);
        follows.deleteByFollowerIdOrFollowingId(userId, userId);
        blocked.deleteByUserIdOrBlockedId(userId, userId);
        libraryItems.deleteByLibraryIdIn(libraryIds);
        libraries.deleteByUserId(userId);
        notifications.deleteByUserId(userId);
        userReports.deleteByReporterOrReportedUser(userId, userId);
        commentReports.deleteByUserId(userId);
        resets.deleteByUserId(userId);
        memberships.removeUser(userId);

        users.deleteOneById(userId);
        if (user.getEmail() != null) verifications.deleteByEmail(user.getEmail());
        banList.clear(userId);
        storage.removeStored(user.getAvatar());
        storage.removeStored(user.getBanner());
        return new DeletedUser(user.getUsername(), videoCount, removed);
    }

    /** Xoa han cac video + binh luan, like, tag, phu de, tin nhan, cho luu trong thu vien va file. */
    public long deleteVideos(List<String> ids) {
        if (ids.isEmpty()) return 0;
        List<Video> list = videos.findByIdIn(ids);
        deleteCommentTrees(comments.findByVideoIdIn(ids).stream().map(Comment::getId).toList());
        videoReactions.deleteByVideoIdIn(ids);
        videoTags.deleteByVideoIdIn(ids);
        subtitles.deleteByVideoIdIn(ids);
        messages.deleteByVideoIdIn(ids);
        libraryItems.deleteByVideoIdIn(ids);
        commentReports.deleteByVideoIdIn(ids);
        videos.deleteAll(list);
        list.forEach(v -> {
            storage.removeStored(v.getSrc());
            storage.removeStored(v.getThumbnail());
        });
        return list.size();
    }

    // ==================== USER REPORTS ====================

    /** Bao cao nguoi dung (gui tu UserService.reportUser). status: pending | resolved | dismissed, mac dinh pending */
    public PageResponse<UserReportRow> listUserReports(String status, Paging p) {
        String s = status == null ? "pending" : status;
        Criteria filter = Criteria.where("comment_id").is(null);
        if (Set.of("pending", "resolved", "dismissed").contains(s)) filter = filter.and("status").is(s);
        Query query = Query.query(filter);

        long total = mongo.count(query, UserReport.class);
        List<UserReport> rows = mongo.find(query.with(p.pageable(Sort.by(Sort.Direction.DESC, "createdAt"))), UserReport.class);
        List<String> reportedIds = rows.stream().map(UserReport::getReportedUser).toList();
        List<String> userIds = new ArrayList<>(reportedIds);
        rows.forEach(r -> userIds.add(r.getReporter()));
        Map<String, User> userMap = usersById(userIds);
        Map<String, Long> pendingAgainst = countBy(UserReport.class, "reported_user", reportedIds.stream()
                .filter(Objects::nonNull).toList(), Criteria.where("status").is("pending"));

        List<UserReportRow> items = rows.stream().map(r -> {
            User target = r.getReportedUser() == null ? null : userMap.get(r.getReportedUser());
            return new UserReportRow(r.getId(), r.getReportedUser(), r.getReporter(), r.getReason(), r.getDetail(),
                    r.getStatus(), r.getCreatedAt(), r.getResolvedAt(), username(userMap, r.getReporter()),
                    target == null ? null : target.getUsername(), target == null ? null : target.getAvatar(),
                    target == null ? null : target.getRole(), BanInfo.from(BanList.activeBan(target)),
                    // Nhieu nguoi cung bao cao 1 user -> dang uu tien xu ly
                    r.getReportedUser() == null ? 0 : pendingAgainst.getOrDefault(r.getReportedUser(), 0L));
        }).toList();
        return PageResponse.of(items, total, p);
    }

    public void setUserReportStatus(String reportId, String status) {
        Update update = new Update().set("status", status).set("resolved_at", "pending".equals(status) ? null : new Date());
        if (mongo.updateFirst(byId(reportId), update, UserReport.class).getMatchedCount() == 0) {
            throw ApiException.badRequest("Không tìm thấy báo cáo");
        }
    }

    // ==================== ANALYTICS ====================

    /** Ngay (theo gio VN) dang 'YYYY-MM-DD' cua n ngay truoc toi hom nay, cu nhat truoc */
    private static List<String> lastDays(int n) {
        ZoneId zone = ZoneId.of(TZ);
        long now = System.currentTimeMillis();
        List<String> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(LocalDate.ofInstant(Instant.ofEpochMilli(now - (long) (n - 1 - i) * 86_400_000L), zone).toString());
        }
        return out;
    }

    /** Ket qua dem theo ngay: { _id: "2026-10-26", n } */
    private record DayCount(String id, long n) {}

    /** Dem so document theo ngay (gio VN) cua 1 field ngay thang */
    private List<Long> perDay(Class<?> entity, String field, List<String> labels, Date since) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where(field).gte(since)),
                Aggregation.project().and(DateOperators.dateOf(field).withTimezone(DateOperators.Timezone.valueOf(TZ))
                        .toString("%Y-%m-%d")).as("day"),
                Aggregation.group("day").count().as("n"));
        Map<String, Long> map = mongo.aggregate(agg, entity, DayCount.class).getMappedResults().stream()
                .collect(Collectors.toMap(DayCount::id, DayCount::n));
        return labels.stream().map(d -> map.getOrDefault(d, 0L)).toList();
    }

    /** Ket qua nhom video theo nguoi dang: { _id: userId, views, videos } */
    private record CreatorRow(String id, long views, long videos) {}

    public AnalyticsResponse getAnalytics(String daysParam) {
        int days = daysParam != null && Set.of("7", "30", "90").contains(daysParam) ? Integer.parseInt(daysParam) : 30;
        List<String> labels = lastDays(days);
        Date since = Date.from(LocalDate.parse(labels.get(0)).atStartOfDay(ZoneId.of("+07:00")).toInstant());

        List<Long> userSeries = perDay(User.class, "created_at", labels, since);
        List<Long> videoSeries = perDay(Video.class, "uploadAt", labels, since);
        List<Long> commentSeries = perDay(Comment.class, "created_at", labels, since);
        List<Long> userReportSeries = perDay(UserReport.class, "created_at", labels, since);
        List<Long> commentReportSeries = perDay(CommentReport.class, "created_at", labels, since);
        List<Long> reports = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) reports.add(userReportSeries.get(i) + commentReportSeries.get(i));

        List<Video> topVideos = mongo.find(new Query().with(Sort.by(Sort.Direction.DESC, "view")).limit(8), Video.class);
        List<CreatorRow> topCreators = mongo.aggregate(Aggregation.newAggregation(
                Aggregation.group("user_id").sum("view").as("views").count().as("videos"),
                Aggregation.sort(Sort.Direction.DESC, "views"),
                Aggregation.limit(8)), Video.class, CreatorRow.class).getMappedResults();
        List<CountRow> topTags = mongo.aggregate(Aggregation.newAggregation(
                Aggregation.group("tag_id").count().as("n"),
                // Bang so video thi xep theo id cho on dinh giua cac lan tai
                Aggregation.sort(Sort.by(Sort.Order.desc("n"), Sort.Order.asc("_id"))),
                Aggregation.limit(10)), VideoTag.class, CountRow.class).getMappedResults();
        Map<String, Long> reactions = mongo.aggregate(Aggregation.newAggregation(Aggregation.group("action").count().as("n")),
                VideoReaction.class, CountRow.class).getMappedResults().stream()
                .filter(r -> r.id() != null).collect(Collectors.toMap(CountRow::id, CountRow::n));

        List<String> videoIds = topVideos.stream().map(Video::getId).toList();
        List<String> nameIds = new ArrayList<>(topVideos.stream().map(Video::getUserId).toList());
        topCreators.forEach(c -> nameIds.add(c.id()));
        Map<String, User> names = usersById(nameIds);
        Map<String, Long> likes = countBy(VideoReaction.class, "video_id", videoIds, Criteria.where("action").is("like"));
        Map<String, Long> commentCounts = countBy(Comment.class, "video_id", videoIds, null);
        Map<String, Tag> tagMap = tags.findByIdIn(topTags.stream().map(CountRow::id).filter(Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(Tag::getId, Function.identity(), (a, b) -> a));
        Map<String, Long> followers = countBy(Follow.class, "following_id",
                topCreators.stream().map(CreatorRow::id).filter(Objects::nonNull).toList(), null);

        return new AnalyticsResponse(days, labels,
                new AnalyticsResponse.Series(userSeries, videoSeries, commentSeries, reports),
                new AnalyticsResponse.Reactions(reactions.getOrDefault("like", 0L), reactions.getOrDefault("dislike", 0L)),
                topVideos.stream().map(v -> new AnalyticsResponse.TopVideo(v.getId(), v.getTitle(), v.getView(), v.getStatus(),
                        username(names, v.getUserId()), likes.getOrDefault(v.getId(), 0L),
                        commentCounts.getOrDefault(v.getId(), 0L))).toList(),
                topCreators.stream().map(c -> new AnalyticsResponse.TopCreator(c.id(), username(names, c.id()), c.views(),
                        c.videos(), c.id() == null ? 0 : followers.getOrDefault(c.id(), 0L))).toList(),
                topTags.stream().map(t -> {
                    Tag tag = t.id() == null ? null : tagMap.get(t.id());
                    return new AnalyticsResponse.TopTag(t.id(), tag == null ? "(đã xoá)" : tag.getName(), t.n());
                }).toList());
    }

    // ==================== BROADCAST ====================

    /** Gui thong bao hang loat. audience: all | admins | user (can userId). */
    public SentResponse broadcast(BroadcastRequest req) {
        Query query = switch (req.audience()) {
            case "all" -> new Query();
            case "admins" -> Query.query(Criteria.where("role").is(User.ROLE_ADMIN));
            default -> {
                if (req.userId() == null || req.userId().isEmpty()) throw ApiException.badRequest("Đối tượng nhận không hợp lệ");
                yield Query.query(Criteria.where("id").is(req.userId()));
            }
        };
        query.fields().include("id");
        List<String> ids = mongo.find(query, User.class).stream().map(User::getId).toList();
        if (ids.isEmpty()) throw ApiException.badRequest("Không có người nhận nào");
        int sent = notificationService.sendMany(ids, req.title().trim(), req.content().trim());
        return new SentResponse(true, sent);
    }

    // ==================== AUDIT LOG ====================

    /** Ghi lai thao tac cua admin. Loi ghi log khong duoc lam hong thao tac chinh. */
    public void audit(AuthUser admin, String action, String targetType, String targetId, String targetLabel,
                      Map<String, Object> detail, String ip) {
        try {
            // Luu kem ten de doc log de hon (va van con ten khi user bi xoa sau nay)
            if ("user".equals(targetType) && targetId != null && targetLabel == null) {
                targetLabel = users.findOneById(targetId).map(User::getUsername).orElse(null);
            }
            AdminAudit a = new AdminAudit();
            a.setId(MongoUtils.uuid());
            a.setAdminId(admin == null ? null : admin.id());
            a.setAdminName(admin == null ? null : admin.username());
            a.setAction(action);
            a.setTargetType(targetType);
            a.setTargetId(targetId);
            a.setTargetLabel(targetLabel);
            a.setDetail(detail);
            a.setIp(ip);
            a.setCreatedAt(new Date());
            audits.insert(a);
        } catch (RuntimeException err) {
            log.error("Admin audit error: {}", err.getMessage());
        }
    }

    public AuditPageResponse listAudit(String action, String admin, String target, Paging p) {
        List<Criteria> conditions = new ArrayList<>();
        if (action != null && !action.isEmpty()) conditions.add(Criteria.where("action").is(action));
        if (admin != null && !admin.isEmpty()) conditions.add(MongoUtils.contains("admin_name", admin));
        if (target != null && !target.isEmpty()) {
            conditions.add(new Criteria().orOperator(Criteria.where("target_id").is(target), MongoUtils.contains("target_label", target)));
        }
        Query query = conditions.isEmpty() ? new Query() : Query.query(new Criteria().andOperator(conditions));
        long total = mongo.count(query, AdminAudit.class);
        List<AuditDto> items = mongo.find(query.with(p.pageable(Sort.by(Sort.Direction.DESC, "createdAt"))), AdminAudit.class)
                .stream().map(AuditDto::from).toList();
        List<String> actions = mongo.findDistinct(new Query(), "action", AdminAudit.class, String.class)
                .stream().filter(Objects::nonNull).sorted().toList();
        return new AuditPageResponse(items, total, p.page(), p.size(), actions);
    }
}
