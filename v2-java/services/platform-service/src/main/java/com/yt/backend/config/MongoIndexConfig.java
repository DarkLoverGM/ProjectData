package com.yt.backend.config;

import com.yt.backend.common.MongoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Index cua cac collection, tao luc server khoi dong (chay lai nhieu lan khong sao).
 * Thay cho PRIMARY KEY / UNIQUE KEY / INDEX cua MySQL, giong INDEXES trong server/models/connectDB.js.
 * Unique index dam bao khong co ban ghi trung (vd. 1 nguoi chi like 1 video 1 lan) ngay ca khi 2 request cung luc.
 * Khai bao bang code thay vi @Indexed tren entity de giu dung ten / tuy chon index ma ban Node da tao.
 */
@Component
public class MongoIndexConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexConfig.class);

    private final MongoTemplate mongo;

    public MongoIndexConfig(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    private record Def(String collection, Index index) {}

    private static Index on(String... fields) {
        Index index = new Index();
        for (String f : fields) {
            boolean desc = f.startsWith("-");
            index.on(desc ? f.substring(1) : f, desc ? Sort.Direction.DESC : Sort.Direction.ASC);
        }
        return index;
    }

    private static Def idx(String collection, String... fields) {
        return new Def(collection, on(fields));
    }

    private static Def unique(String collection, String... fields) {
        return new Def(collection, on(fields).unique());
    }

    private static final List<Def> INDEXES = List.of(
            unique("users", "id"),
            new Def("users", on("banned_until").sparse()),
            new Def("users", on("email").collation(MongoUtils.CASE_INSENSITIVE)),
            new Def("users", on("username").collation(MongoUtils.CASE_INSENSITIVE)),
            // Chi user dang nhap Google moi co google_id -> partial de user thuong khong bi coi la trung
            new Def("users", on("google_id").unique()
                    .partial(PartialIndexFilter.of(Criteria.where("google_id").type(2)))),

            unique("videos", "id"),
            idx("videos", "slug"),
            idx("videos", "user_id"),
            idx("videos", "status", "video_group"),
            // Hang doi "Can phan hoi" cua admin
            new Def("videos", on("appeal.pending").sparse()),

            unique("tags", "id"),
            unique("tags", "slug"),
            unique("video_tags", "video_id", "tag_id"),
            idx("video_tags", "tag_id"),

            unique("action_video", "id"),
            unique("action_video", "video_id", "user_id"),
            unique("action_comment", "id"),
            unique("action_comment", "comment_id", "user_id"),
            idx("action_comment", "user_id"),

            unique("comments", "id"),
            idx("comments", "video_id", "created_at"),
            idx("comments", "parent_id"),

            unique("followers", "id"),
            unique("followers", "follower_id", "following_id"),
            idx("followers", "following_id"),

            unique("blocked_users", "user_id", "blocked_id"),
            idx("blocked_users", "blocked_id"),

            unique("library", "id"),
            idx("library", "user_id"),
            unique("value_in_library", "id"),
            unique("value_in_library", "library_id", "video_id"),

            unique("notifications", "id"),
            idx("notifications", "user_id", "-created_at"),
            unique("push_tokens", "token"),
            idx("push_tokens", "user_id"),

            unique("reports", "id"),
            idx("reports", "reporter", "reported_user", "status"),
            idx("reports", "status", "-created_at"),
            unique("report_comment", "id"),
            idx("report_comment", "status", "-created_at"),
            idx("report_comment", "comment_id"),

            unique("user_resetpass", "id"),
            idx("user_resetpass", "user_id", "-createAt"),
            unique("subtitles", "video_id", "lang"),
            unique("email_verification", "email"),
            idx("video_messages", "video_id", "created_at"),
            idx("admin_audit", "-created_at"),
            idx("admin_audit", "target_id", "-created_at"),

            // Hoi vien kenh + don thanh toan (MembershipService, PaymentService)
            unique("membership_tiers", "id"),
            idx("membership_tiers", "channel_id", "price"),
            unique("memberships", "id"),
            unique("memberships", "user_id", "channel_id"),
            idx("memberships", "channel_id", "expires_at"),
            idx("memberships", "tier_id", "expires_at"),
            unique("orders", "id"),
            idx("orders", "user_id", "-created_at"),
            idx("orders", "channel_id", "status", "-paid_at"),
            idx("orders", "status", "-created_at"));

    @EventListener(ApplicationReadyEvent.class)
    @Order(0)
    public void ensureIndexes() {
        for (Def d : INDEXES) {
            try {
                mongo.indexOps(d.collection()).ensureIndex(d.index());
            } catch (RuntimeException err) {
                // Khong lam sap server: thuong la do du lieu cu dang bi trung, xem server/MONGODB.md
                log.error("[mongo] Khong tao duoc index {} {}: {}", d.collection(), d.index().getIndexKeys().toJson(), err.getMessage());
            }
        }
        log.info("MongoDB da ket noi: {}", mongo.getDb().getName());
    }
}
