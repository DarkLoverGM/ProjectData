package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Video (collection videos). status: 1 = dang hien, 0 = an (admin dat).
 * visibility: nguoi dang tu chon trong Studio (public | unlisted | private), null = public.
 * Thong tin nguoi dang lay tu users qua userId, like/dislike nam o action_video (VideoReaction).
 */
@Document("videos")
@Getter
@Setter
@NoArgsConstructor
public class Video extends BaseDocument {

    public static final int VISIBLE = 1;
    public static final int HIDDEN = 0;
    public static final String GROUP_MAIN = "main";
    public static final String GROUP_SHORT = "short";
    /** Hien o trang chu, tim kiem, kenh */
    public static final String PUBLIC = "public";
    /** Khong hien o danh sach nao, ai co link van xem duoc */
    public static final String UNLISTED = "unlisted";
    /** Chi nguoi dang va admin xem duoc */
    public static final String PRIVATE = "private";
    public static final List<String> VISIBILITIES = List.of(PUBLIC, UNLISTED, PRIVATE);

    @Field("id")
    private String id;
    private String title;
    private String slug;
    private String description;
    /** Duong dan file (/video/123.mp4) hoac URL Firebase Storage */
    private String src;
    private String thumbnail;
    private String subtitle;
    @Field("user_id")
    private String userId;
    private Long view;
    private Integer status;
    @Field("video_group")
    private String videoGroup;
    @Field("uploadAt")
    private Date uploadAt;
    @Field("media_updated_at")
    private Date mediaUpdatedAt;
    /** Moc OP/ED de hien nut bo qua: { op?: {start, end}, ep?: {start, end} }, chi vai video co */
    private Map<String, Object> speciallist;
    /** Ket qua duyet cua admin, null = chua ai duyet */
    private Moderation moderation;
    /** Nguoi dang dang cho admin phan hoi (nhan tu Studio / thay file sau khi bi chan) */
    private Appeal appeal;
    /** Chi hoi vien kenh moi xem duoc (MembershipService) */
    @Field("members_only")
    private Boolean membersOnly;
    private String visibility;

    public boolean isVisible() {
        return status != null && status == VISIBLE;
    }

    public boolean isShort() {
        return GROUP_SHORT.equals(videoGroup);
    }

    public String visibilityOrDefault() {
        // List.of().contains(null) nem NPE, video cu chua co field nay
        return visibility != null && VISIBILITIES.contains(visibility) ? visibility : PUBLIC;
    }

    public boolean isPrivate() {
        return PRIVATE.equals(visibility);
    }

    /** Dieu kien video duoc hien o danh sach (trang chu, tim kiem, kenh, tag, next/prev) */
    public static Criteria listed() {
        return Criteria.where("status").is(VISIBLE).and("visibility").nin(UNLISTED, PRIVATE);
    }

    public boolean isMembersOnly() {
        return Boolean.TRUE.equals(membersOnly);
    }

    public String moderationState() {
        return moderation == null ? null : moderation.getState();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Moderation {
        /** approved | blocked | resubmitted */
        private String state;
        private List<String> rules;
        private String note;
        /** Giay trong video noi vi pham */
        @Field("at_time")
        private Long atTime;
        private String by;
        @Field("by_name")
        private String byName;
        private Date at;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Appeal {
        private Boolean pending;
        private Date at;

        public Appeal(boolean pending, Date at) {
            this.pending = pending;
            this.at = at;
        }
    }
}
