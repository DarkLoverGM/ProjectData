package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/** Thong bao (collection notifications). isRead: 0 | 1 (giu kieu so nhu ban Node). link: trang mo khi bam. */
@Document("notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification extends BaseDocument {

    @Field("id")
    private String id;
    private String title;
    private String content;
    private String link;
    @Field("user_id")
    private String userId;
    @Field("is_read")
    private Integer isRead;
    @Field("created_at")
    private Date createdAt;

    public static Notification create(String id, String userId, String title, String content, String link) {
        Notification n = new Notification();
        n.id = id;
        n.userId = userId;
        n.title = title == null ? "" : title;
        n.content = content == null ? "" : content;
        n.link = link;
        n.isRead = 0;
        n.createdAt = new Date();
        return n;
    }
}
