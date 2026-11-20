package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/** userId khong muon thay video cua blockedId (collection blocked_users). */
@Document("blocked_users")
@Getter
@Setter
@NoArgsConstructor
public class BlockedUser extends BaseDocument {

    @Field("user_id")
    private String userId;
    @Field("blocked_id")
    private String blockedId;
    @Field("created_at")
    private Date createdAt;
}
