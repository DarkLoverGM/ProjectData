package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/** Binh luan (collection comments). parentId = binh luan goc dang tra loi (cay chi 2 tang). */
@Document("comments")
@Getter
@Setter
@NoArgsConstructor
public class Comment extends BaseDocument {

    @Field("id")
    private String id;
    @Field("user_id")
    private String userId;
    @Field("video_id")
    private String videoId;
    @Field("parent_id")
    private String parentId;
    private String content;
    @Field("report_id")
    private String reportId;
    @Field("created_at")
    private Date createdAt;
    /** Chi co voi binh luan Super Thanks (tao sau khi thanh toan) */
    @Field("super_thanks")
    private SuperThanks superThanks;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SuperThanks {
        private Long amount;
        @Field("order_id")
        private String orderId;

        public SuperThanks(long amount, String orderId) {
            this.amount = amount;
            this.orderId = orderId;
        }
    }
}
