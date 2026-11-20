package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/** Lien ket video - tag (collection video_tags), unique (video_id, tag_id). */
@Document("video_tags")
@Getter
@Setter
@NoArgsConstructor
public class VideoTag extends BaseDocument {

    @Field("video_id")
    private String videoId;
    @Field("tag_id")
    private String tagId;
}
