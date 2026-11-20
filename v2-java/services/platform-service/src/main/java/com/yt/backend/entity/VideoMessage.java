package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/** Trao doi giua nguoi dang va admin ve 1 video (collection video_messages). role: creator | admin | system */
@Document("video_messages")
@Getter
@Setter
@NoArgsConstructor
public class VideoMessage extends BaseDocument {

    @Field("id")
    private String id;
    @Field("video_id")
    private String videoId;
    private String role;
    @Field("sender_id")
    private String senderId;
    @Field("sender_name")
    private String senderName;
    private String content;
    @Field("created_at")
    private Date createdAt;
}
