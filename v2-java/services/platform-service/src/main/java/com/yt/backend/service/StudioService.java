package com.yt.backend.service;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.common.ApiException;
import com.yt.backend.common.FileStorage;
import com.yt.backend.dto.studio.*;
import com.yt.backend.dto.video.TagDto;
import com.yt.backend.entity.Video;
import com.yt.backend.entity.VideoMessage;
import com.yt.backend.repository.CommentRepository;
import com.yt.backend.repository.VideoReactionRepository;
import com.yt.backend.repository.VideoRepository;
import com.yt.backend.websocket.UserEvent;
import com.yt.backend.websocket.UserRealtime;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * Trang Studio: nguoi dang quan ly video cua chinh minh, ke ca video dang an / bi chan
 * (services/StudioServices.js).
 */
@Service
public class StudioService {

    private static final int MAX_DESCRIPTION = 5000;

    private final VideoRepository videos;
    private final VideoReactionRepository reactions;
    private final CommentRepository comments;
    private final TagService tagService;
    private final VideoMessageService videoMessages;
    private final FileStorage fileStorage;
    private final NotificationService notifications;
    private final MembershipService memberships;
    private final MongoTemplate mongo;
    private final UserRealtime userRealtime;

    public StudioService(VideoRepository videos, VideoReactionRepository reactions, CommentRepository comments,
                         TagService tagService, VideoMessageService videoMessages, FileStorage fileStorage,
                         NotificationService notifications, MembershipService memberships, MongoTemplate mongo, UserRealtime userRealtime) {
        this.videos = videos;
        this.reactions = reactions;
        this.comments = comments;
        this.tagService = tagService;
        this.videoMessages = videoMessages;
        this.fileStorage = fileStorage;
        this.notifications = notifications;
        this.memberships = memberships;
        this.mongo = mongo;
        this.userRealtime = userRealtime;
    }

    /**
     * Trang thai hien thi cho nguoi dang, gop tu status + moderation + visibility:
     * - public:      dang hien
     * - unlisted:    nguoi dang an khoi danh sach, ai co link van xem duoc
     * - private:     nguoi dang de rieng tu, chi minh ho (va admin) xem duoc
     * - blocked:     bi admin chan, xem moderation de biet ly do
     * - resubmitted: da sua/thay video sau khi bi chan, cho admin duyet lai
     * - hidden:      admin an (khong qua man duyet)
     */
    private static String stateOf(Video v) {
        String state = v.moderationState();
        if ("blocked".equals(state)) return "blocked";
        if ("resubmitted".equals(state)) return "resubmitted";
        return v.isVisible() ? v.visibilityOrDefault() : "hidden";
    }

    private static boolean appealPending(Video v) {
        return v.getAppeal() != null && Boolean.TRUE.equals(v.getAppeal().getPending());
    }

    private static String text(String s) {
        return s == null ? "" : s;
    }

    private static Query byId(String videoId) {
        return Query.query(Criteria.where("id").is(videoId));
    }

    /** Video cua chinh nguoi dang, khong phai thi 404 */
    private Video ownVideo(String userId, String videoId) {
        return videos.findOneByIdAndUserId(videoId, userId).orElseThrow(() -> ApiException.notFound("Không tìm thấy video"));
    }

    /** Danh sach video cua toi + so lieu tong */
    public StudioListResponse listVideos(String userId) {
        List<Video> rows = videos.findByUserId(userId, Sort.by(Sort.Direction.DESC, "uploadAt"));
        List<String> ids = rows.stream().map(Video::getId).toList();
        Map<String, Long> messageCounts = videoMessages.countByVideo(ids);

        List<StudioVideoItem> items = new ArrayList<>();
        long views = 0, likeSum = 0, commentSum = 0, blocked = 0;
        for (Video v : rows) {
            long view = v.getView() == null ? 0 : v.getView();
            long like = reactions.countByVideoIdAndAction(v.getId(), "like");
            long comment = comments.countByVideoId(v.getId());
            String state = stateOf(v);
            items.add(new StudioVideoItem(v.getId(), text(v.getTitle()), text(v.getSlug()), text(v.getThumbnail()),
                    v.getVideoGroup() == null ? Video.GROUP_MAIN : v.getVideoGroup(), v.getUploadAt(), v.getMediaUpdatedAt(),
                    state, PublicModeration.from(v.getModeration()), appealPending(v),
                    messageCounts.getOrDefault(v.getId(), 0L), v.isMembersOnly(), v.visibilityOrDefault(),
                    new StudioVideoItem.Stats(view, like, comment)));
            views += view;
            likeSum += like;
            commentSum += comment;
            if ("blocked".equals(state)) blocked++;
        }
        return new StudioListResponse(items, new StudioListResponse.Summary(items.size(), views, likeSum, commentSum, blocked));
    }

    /** Chi tiet 1 video de sua + lich su trao doi voi admin */
    public StudioVideoDetail getVideo(String userId, String videoId) {
        Video v = ownVideo(userId, videoId);
        String id = v.getId();
        List<String> tags = tagService.getTagsByVideoIds(List.of(id)).getOrDefault(id, List.of())
                .stream().map(TagDto::name).toList();
        List<VideoMessageDto> messages = videoMessages.list(id).stream()
                .map(m -> VideoMessageDto.from(m).forCreator()).toList();
        return new StudioVideoDetail(id, text(v.getTitle()), text(v.getSlug()), text(v.getDescription()), text(v.getSrc()),
                text(v.getThumbnail()), v.getVideoGroup() == null ? Video.GROUP_MAIN : v.getVideoGroup(), v.isMembersOnly(),
                v.visibilityOrDefault(), v.getUploadAt(), v.getMediaUpdatedAt(), stateOf(v), PublicModeration.from(v.getModeration()),
                appealPending(v), tags,
                new StudioVideoItem.Stats(v.getView() == null ? 0 : v.getView(),
                        reactions.countByVideoIdAndAction(id, "like"), comments.countByVideoId(id)),
                messages);
    }

    /**
     * Sua tieu de / mo ta / tag / thumbnail / che do hoi vien / che do hien thi. Khong doi id nen luot xem, like, binh luan giu nguyen.
     * Field null nghia la khong gui len.
     */
    public StudioVideoResponse updateDetails(String userId, String videoId, UpdateVideoRequest req, MultipartFile thumbFile)
            throws IOException {
        Video v = ownVideo(userId, videoId);

        Update update = new Update();
        if (req.title() != null) {
            String title = req.title().trim();
            String slug = VideoService.slugOf(title);
            if (!slug.equals(v.getSlug()) && videos.existsBySlugAndIdNot(slug, v.getId())) {
                throw ApiException.badRequest("Tiêu đề video đã tồn tại");
            }
            update.set("title", title).set("slug", slug);
        }
        if (req.description() != null) {
            String d = req.description().trim();
            if (d.length() > MAX_DESCRIPTION) d = d.substring(0, MAX_DESCRIPTION);
            update.set("description", d.isEmpty() ? "No description" : d);
        }
        if (req.membersOnly() != null) {
            boolean on = "true".equals(req.membersOnly());
            if (on && v.isShort()) throw ApiException.badRequest("Short không thể đặt chỉ dành cho hội viên");
            if (on && !memberships.hasActiveTier(userId)) {
                throw ApiException.badRequest("Hãy tạo ít nhất 1 gói hội viên (Studio → Kiếm tiền) trước khi khoá video");
            }
            update.set("members_only", on);
        }
        // Khong dung toi status: video admin dang an / chan van an, doi visibility chi co tac dung khi duoc hien lai
        if (req.visibility() != null) update.set("visibility", req.visibility());
        // Da qua moi buoc kiem tra moi luu file (co the la day len Firebase)
        String newThumb = null;
        if (thumbFile != null && !thumbFile.isEmpty()) {
            newThumb = fileStorage.store(thumbFile, "thumb");
            update.set("thumbnail", newThumb);
        }

        if (!update.getUpdateObject().isEmpty()) mongo.updateFirst(byId(v.getId()), update, Video.class);
        // tags gui len dang chuoi JSON (FormData), TagService tu chuan hoa va bo trung
        if (req.tags() != null) tagService.setVideoTags(v.getId(), req.tags());
        if (newThumb != null && !newThumb.equals(v.getThumbnail())) fileStorage.removeStored(v.getThumbnail());

        return new StudioVideoResponse(true, getVideo(userId, v.getId()));
    }

    /**
     * Thay file video (va thumbnail neu co) nhung giu nguyen id -> luot xem, like, binh luan, thu vien van con.
     * Noi dung moi phai duoc duyet lai:
     * - video dang bi chan -> 'resubmitted', van an toi khi admin duyet
     * - video khac -> van hien, quay lai hang doi "Cho duyet"
     * Moc OP/ED (speciallist) theo file cu nen bi xoa; phu de giu lai, nguoi dang tu chinh neu lech.
     */
    public StudioVideoResponse replaceMedia(AuthUser user, String videoId, MultipartFile videoFile,
                                           MultipartFile thumbFile) throws IOException {
        Video v = ownVideo(user.id(), videoId);
        if (videoFile == null || videoFile.isEmpty()) throw ApiException.badRequest("Chưa chọn file video mới");
        boolean hasThumb = thumbFile != null && !thumbFile.isEmpty();

        Date now = new Date();
        String src = fileStorage.store(videoFile, "video");
        Update update = new Update().set("src", src).set("media_updated_at", now).unset("speciallist");
        String thumb = null;
        if (hasThumb) {
            try {
                thumb = fileStorage.store(thumbFile, "thumb");
                update.set("thumbnail", thumb);
            } catch (IOException | RuntimeException e) {
                fileStorage.removeStored(src);
                throw e;
            }
        }

        String state = v.moderationState();
        boolean wasBlocked = "blocked".equals(state) || "resubmitted".equals(state);
        if (wasBlocked) {
            // Giu rule/ghi chu cu de admin biet lan truoc sai o dau
            update.set("moderation.state", "resubmitted").set("appeal", new Video.Appeal(true, now));
        } else {
            update.unset("moderation");
        }
        mongo.updateFirst(byId(v.getId()), update, Video.class);

        fileStorage.removeStored(v.getSrc());
        if (thumb != null && !thumb.equals(v.getThumbnail())) fileStorage.removeStored(v.getThumbnail());

        videoMessages.add(v.getId(), "system", user, wasBlocked
                ? "Người đăng đã thay file video mới và gửi yêu cầu duyệt lại."
                : "Người đăng đã thay file video mới.");
        return new StudioVideoResponse(true, getVideo(user.id(), v.getId()));
    }

    /**
     * Nguoi dang nhan cho admin. resubmit = true: video dang bi chan duoc danh dau "cho duyet lai"
     * (dung sau khi da sua tieu de/thumbnail ma khong can thay file).
     */
    public MessageItemResponse sendMessage(AuthUser user, String videoId, String content, boolean resubmit) {
        Video v = ownVideo(user.id(), videoId);
        VideoMessage item = videoMessages.add(v.getId(), "creator", user, content);

        Update update = Update.update("appeal", new Video.Appeal(true, new Date()));
        if (resubmit && "blocked".equals(v.moderationState())) update.set("moderation.state", "resubmitted");
        mongo.updateFirst(byId(v.getId()), update, Video.class);
        return new MessageItemResponse(true, VideoMessageDto.from(item));
    }

    /**
     * Thong bao cho nguoi dang, bam vao se mo dung video trong Studio.
     * Neu ho dang mo Studio thi video do tu tai lai ngay (admin tra loi / chan / hien lai) qua /ws/user.
     */
    public void notifyCreator(String userId, String videoId, String title, String content) {
        String link = "/studio?video=" + FileStorage.encodeURIComponent(String.valueOf(videoId)) + "&tab=messages";
        notifications.send(userId, title, content, link);
        userRealtime.sendTo(userId, new UserEvent.StudioVideo(videoId, title, content));
    }
}
