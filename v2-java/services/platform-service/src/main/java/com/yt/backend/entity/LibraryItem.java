package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/** 1 video trong thu vien (collection value_in_library). */
@Document("value_in_library")
@Getter
@Setter
@NoArgsConstructor
public class LibraryItem extends BaseDocument {

    @Field("id")
    private String id;
    @Field("library_id")
    private String libraryId;
    @Field("video_id")
    private String videoId;
    private Date createdAt;
}
