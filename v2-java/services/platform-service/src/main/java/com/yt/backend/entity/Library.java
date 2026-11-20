package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/** Thu vien video da luu cua user (collection library). Moi user co 1 thu vien mac dinh. */
@Document("library")
@Getter
@Setter
@NoArgsConstructor
public class Library extends BaseDocument {

    @Field("id")
    private String id;
    @Field("user_id")
    private String userId;
    @Field("library_name")
    private String libraryName;
}
