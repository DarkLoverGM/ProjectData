package com.yt.backend.service;

import com.yt.backend.common.ApiException;
import com.yt.backend.common.MongoUtils;
import com.yt.backend.dto.comment.CommentDto;
import com.yt.backend.dto.comment.CommentReactionResponse;
import com.yt.backend.dto.comment.CommentReportDto;
import com.yt.backend.entity.Comment;
import com.yt.backend.entity.CommentReaction;
import com.yt.backend.entity.CommentReport;
import com.yt.backend.entity.User;
import com.yt.backend.entity.Video;
import com.yt.backend.repository.CommentReactionRepository;
import com.yt.backend.repository.CommentReportRepository;
import com.yt.backend.repository.CommentRepository;
import com.yt.backend.repository.UserRepository;
import com.yt.backend.repository.VideoRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Binh luan (cay 2 tang), like / dislike binh luan, bao cao binh luan, binh luan Super Thanks */
@Service
public class CommentService {

    private final CommentRepository comments;
    private final CommentReactionRepository reactions;
    private final CommentReportRepository reports;
    private final UserRepository users;
    private final VideoRepository videos;
    private final MembershipService memberships;
    private final NotificationService notifications;
    private final MongoTemplate mongo;

    public CommentService(CommentRepository comments, CommentReactionRepository reactions, CommentReportRepository reports,
                          UserRepository users, VideoRepository videos, MembershipService memberships,
                          NotificationService notifications, MongoTemplate mongo) {
        this.comments = comments;
        this.reactions = reactions;
        this.reports = reports;
        this.users = users;
        this.videos = videos;
        this.memberships = memberships;
        this.notifications = notifications;
        this.mongo = mongo;
    }

    /** Ket qua dem theo nhom cua aggregation: { _id: commentId, n } */
    private record CountRow(String id, long n) {}

    /** commentId -> so luot like (hoac dislike), dem 1 lan cho ca danh sach thay vi tung binh luan */
    private Map<String, Long> countReactions(Collection<String> commentIds, String action) {
        if (commentIds.isEmpty()) return Map.of();
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("comment_id").in(commentIds).and("action").is(action)),
                Aggregation.group("comment_id").count().as("n"));
        return mongo.aggregate(agg, CommentReaction.class, CountRow.class).getMappedResults().stream()
                .collect(Collectors.toMap(CountRow::id, CountRow::n));
    }

    /** videoId -> (userId -> ten goi hoi vien o kenh cua video do) */
    private Map<String, Map<String, String>> memberBadges(List<Comment> list) {
        Map<String, Set<String>> usersByVideo = new HashMap<>();
        for (Comment c : list) usersByVideo.computeIfAbsent(c.getVideoId(), k -> new HashSet<>()).add(c.getUserId());
        Map<String, String> owners = videos.findByIdIn(usersByVideo.keySet()).stream()
                .collect(Collectors.toMap(Video::getId, v -> String.valueOf(v.getUserId()), (a, b) -> a));
        Map<String, Map<String, String>> out = new HashMap<>();
        usersByVideo.forEach((videoId, userIds) -> out.put(videoId,
                owners.containsKey(videoId) ? memberships.activeTierNames(owners.get(videoId), userIds) : Map.of()));
        return out;
    }

    /** Entity -> DTO kem ten / avatar nguoi viet, so like, huy hieu hoi vien (thay cho JOIN + subquery) */
    private List<CommentDto> toDtos(List<Comment> list) {
        List<String> ids = list.stream().map(Comment::getId).toList();
        Map<String, User> authors = users.findByIdIn(list.stream().map(Comment::getUserId).filter(Objects::nonNull)
                .collect(Collectors.toSet())).stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        Map<String, Long> likes = countReactions(ids, "like");
        Map<String, Long> dislikes = countReactions(ids, "dislike");
        Map<String, Map<String, String>> badges = memberBadges(list);

        List<CommentDto> out = new ArrayList<>();
        for (Comment c : list) {
            User u = authors.get(c.getUserId()); // LEFT JOIN: user da bi xoa thi null
            out.add(new CommentDto(c.getId(), c.getVideoId(), c.getParentId(), c.getUserId(),
                    u == null ? null : u.getUsername(), u == null ? null : u.getAvatar(),
                    c.getContent(), c.getCreatedAt(), likes.getOrDefault(c.getId(), 0L), dislikes.getOrDefault(c.getId(), 0L),
                    c.getSuperThanks() == null ? null : c.getSuperThanks().getAmount(),
                    badges.getOrDefault(c.getVideoId(), Map.of()).get(c.getUserId()),
                    null, null));
        }
        return out;
    }

    /**
     * Danh sach binh luan GOC, moi cai kem mang `replies`. Gom nhom trong Java.
     * userId (null = khach) dung de gan `userReaction` cho tung binh luan.
     */
    public List<CommentDto> getComments(String videoId, String userId) {
        List<CommentDto> rows = toDtos(comments.findByVideoIdOrderByCreatedAtAsc(videoId));

        Map<String, String> mine = new HashMap<>();
        if (userId != null && !rows.isEmpty()) {
            for (CommentReaction r : reactions.findByUserIdAndCommentIdIn(userId, rows.stream().map(CommentDto::id).toList())) {
                mine.put(r.getCommentId(), r.getAction());
            }
        }

        // Luot 1: binh luan goc. Luot 2: gan phan hoi vao goc tuong ung
        Map<String, List<CommentDto>> repliesByRoot = new LinkedHashMap<>();
        List<CommentDto> roots = new ArrayList<>();
        for (CommentDto row : rows) {
            if (row.isReply()) continue;
            List<CommentDto> replies = new ArrayList<>();
            repliesByRoot.put(row.id(), replies);
            roots.add(row.withUserReaction(mine.get(row.id())).withReplies(replies));
        }
        for (CommentDto row : rows) {
            if (!row.isReply()) continue;
            CommentDto withReaction = row.withUserReaction(mine.get(row.id()));
            List<CommentDto> replies = repliesByRoot.get(row.parentId());
            // Goc da bi xoa / khac video: van hien thi thay vi lam mat du lieu
            if (replies != null) replies.add(withReaction);
            else roots.add(withReaction.withReplies(new ArrayList<>()));
        }
        return roots;
    }

    public Comment getCommentById(String commentId) {
        return commentId == null ? null : comments.findOneById(commentId).orElse(null);
    }

    /** Binh luan vua tao, cung dang voi getComments (binh luan goc kem replies rong) */
    private CommentDto detail(Comment c) {
        CommentDto dto = toDtos(List.of(c)).get(0);
        return dto.isReply() ? dto : dto.withReplies(List.of());
    }

    /**
     * parentId la id binh luan dang tra loi. Neu no von da la mot phan hoi,
     * he thong tu ghim ve binh luan GOC => toan bo cay chi co dung 2 tang.
     */
    public CommentDto postComment(String videoId, String userId, String text, String parentId) {
        Video video = videos.findOneById(videoId).orElseThrow(() -> ApiException.notFound("Video không tồn tại"));

        String rootId = null;
        Comment parent = null;
        if (parentId != null && !parentId.isEmpty()) {
            parent = getCommentById(parentId);
            if (parent == null || !videoId.equals(parent.getVideoId())) throw ApiException.badRequest("Không gửi được bình luận");
            rootId = parent.getParentId() != null && !parent.getParentId().isEmpty() ? parent.getParentId() : parent.getId();
        }

        Comment c = new Comment();
        c.setId(MongoUtils.uuid());
        c.setUserId(userId);
        c.setVideoId(videoId);
        c.setParentId(rootId);
        c.setContent(text.trim());
        c.setCreatedAt(new Date());
        comments.insert(c);
        notifyComment(video, c, parent);
        return detail(c);
    }

    /**
     * Bao chu video co binh luan moi, bao nguoi viet binh luan duoc tra loi.
     * Khong bao cho chinh nguoi viet, va khong bao 2 lan neu chu video cung la nguoi duoc tra loi.
     */
    private void notifyComment(Video video, Comment c, Comment parent) {
        String author = users.findOneById(c.getUserId()).map(User::getUsername).orElse("Ai đó");
        String link = video.isShort() ? "/shorts/" + video.getId()
                : "/watch/" + (video.getSlug() == null ? "video" : video.getSlug()) + "/" + video.getId();
        String preview = c.getContent().length() > 100 ? c.getContent().substring(0, 100) + "…" : c.getContent();
        String owner = video.getUserId() == null ? null : String.valueOf(video.getUserId());

        String repliedTo = parent == null ? null : parent.getUserId();
        if (repliedTo != null && !repliedTo.equals(c.getUserId())) {
            notifications.send(repliedTo, "Phản hồi mới", author + " đã trả lời bình luận của bạn: " + preview, link);
        }
        if (owner != null && !owner.equals(c.getUserId()) && !owner.equals(repliedTo)) {
            notifications.send(owner, "Bình luận mới",
                    author + " đã bình luận về video \"" + video.getTitle() + "\": " + preview, link);
        }
    }

    /**
     * Binh luan Super Thanks, tao sau khi thanh toan thanh cong (PaymentService).
     * Luon la binh luan goc, luu kem so tien + don hang de doi soat.
     */
    public CommentDto postSuperThanks(String videoId, String userId, String text, long amount, String orderId) {
        Comment c = new Comment();
        c.setId(MongoUtils.uuid());
        c.setUserId(userId);
        c.setVideoId(videoId);
        c.setContent(text != null && !text.isEmpty() ? text : "Cảm ơn bạn vì video này!");
        c.setSuperThanks(new Comment.SuperThanks(amount, orderId));
        c.setCreatedAt(new Date());
        comments.insert(c);
        return detail(c);
    }

    // ------------------------------------------------------------------
    // REACTIONS (like / dislike): moi user chi 1 cam xuc tren 1 binh luan
    // ------------------------------------------------------------------

    /** action: like | unlike | dislike | undislike. Sai action thi null */
    public CommentReactionResponse reactComment(String commentId, String userId, String action) {
        switch (action) {
            case "like", "dislike" -> {
                Query mine = Query.query(Criteria.where("comment_id").is(commentId).and("user_id").is(userId));
                // Thay cho INSERT ... ON DUPLICATE KEY UPDATE (unique index comment_id + user_id).
                // 2 request cung luc cung upsert -> 1 cai trung key, chi can cap nhat lai
                Object res = MongoUtils.ignoreDuplicate(() -> mongo.upsert(mine,
                        new Update().set("action", action).setOnInsert("id", MongoUtils.uuid()), CommentReaction.class));
                if (res == null) mongo.updateFirst(mine, Update.update("action", action), CommentReaction.class);
            }
            case "unlike", "undislike" -> reactions.deleteByCommentIdAndUserIdAndAction(commentId, userId, action.substring(2));
            default -> {
                return null;
            }
        }
        return getReactionStats(commentId, userId);
    }

    public CommentReactionResponse getReactionStats(String commentId, String userId) {
        String mine = userId == null ? null
                : reactions.findOneByCommentIdAndUserId(commentId, userId).map(CommentReaction::getAction).orElse(null);
        return new CommentReactionResponse(commentId, reactions.countByCommentIdAndAction(commentId, "like"),
                reactions.countByCommentIdAndAction(commentId, "dislike"), mine);
    }

    /** Luu bao cao binh luan (admin xu ly o trang Bao cao binh luan) */
    public CommentReportDto reportComment(String commentId, String userId, String videoId, String reason, String detail) {
        CommentReport r = new CommentReport();
        r.setId(MongoUtils.uuid());
        r.setUserId(userId);
        r.setCommentId(commentId);
        r.setVideoId(videoId);
        r.setReason(reason);
        r.setDetail(detail);
        r.setCreatedAt(new Date());
        r.setStatus(0);
        reports.insert(r);
        return new CommentReportDto(r.getId(), commentId, userId, videoId, reason, detail, r.getCreatedAt(), 0);
    }
}
