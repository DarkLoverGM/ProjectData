package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/** FCM token cua 1 trinh duyet da bat thong bao day (collection push_tokens). */
@Document("push_tokens")
@Getter
@Setter
@NoArgsConstructor
public class PushToken extends BaseDocument {

    private String token;
    @Field("user_id")
    private String userId;
    @Field("updated_at")
    private Date updatedAt;
}
