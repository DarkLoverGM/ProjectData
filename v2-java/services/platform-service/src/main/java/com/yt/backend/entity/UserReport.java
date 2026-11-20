package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/** Bao cao 1 nguoi dung (collection reports). status: pending | resolved | dismissed */
@Document("reports")
@Getter
@Setter
@NoArgsConstructor
public class UserReport extends BaseDocument {

    @Field("id")
    private String id;
    @Field("comment_id")
    private String commentId;
    @Field("reported_user")
    private String reportedUser;
    private String reporter;
    private String reason;
    private String detail;
    private String status;
    @Field("created_at")
    private Date createdAt;
    @Field("resolved_at")
    private Date resolvedAt;
}
