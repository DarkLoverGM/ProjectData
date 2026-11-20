package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/** Bao cao 1 binh luan (collection report_comment). status: 0 = cho xu ly, 1 = da xu ly */
@Document("report_comment")
@Getter
@Setter
@NoArgsConstructor
public class CommentReport extends BaseDocument {

    @Field("id")
    private String id;
    @Field("user_id")
    private String userId;
    @Field("comment_id")
    private String commentId;
    @Field("video_id")
    private String videoId;
    private String reason;
    private String detail;
    private Integer status;
    @Field("created_at")
    private Date createdAt;
}
