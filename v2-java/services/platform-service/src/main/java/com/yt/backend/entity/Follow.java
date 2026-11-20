package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/** followerId dang theo doi kenh followingId (collection followers). */
@Document("followers")
@Getter
@Setter
@NoArgsConstructor
public class Follow extends BaseDocument {

    @Field("id")
    private String id;
    @Field("follower_id")
    private String followerId;
    @Field("following_id")
    private String followingId;
}
