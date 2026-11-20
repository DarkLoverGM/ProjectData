package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/** Tu cach hoi vien cua userId o kenh channelId (collection memberships), unique (user_id, channel_id). */
@Document("memberships")
@Getter
@Setter
@NoArgsConstructor
public class Membership extends BaseDocument {

    @Field("id")
    private String id;
    @Field("user_id")
    private String userId;
    @Field("channel_id")
    private String channelId;
    @Field("tier_id")
    private String tierId;
    @Field("started_at")
    private Date startedAt;
    @Field("expires_at")
    private Date expiresAt;
    @Field("updated_at")
    private Date updatedAt;

    /** Con han hoi vien khong */
    public boolean isActive() {
        return expiresAt != null && expiresAt.after(new Date());
    }
}
