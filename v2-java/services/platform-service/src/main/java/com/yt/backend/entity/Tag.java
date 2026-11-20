package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/** Tag gan cho video (collection tags). slug la khoa so trung + URL (/tag/hanh-dong). */
@Document("tags")
@Getter
@Setter
@NoArgsConstructor
public class Tag extends BaseDocument {

    @Field("id")
    private String id;
    private String name;
    private String slug;
    @Field("created_at")
    private Date createdAt;
}
