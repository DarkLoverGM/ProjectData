package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/** Like / dislike binh luan (collection action_comment). action: like | dislike */
@Document("action_comment")
@Getter
@Setter
@NoArgsConstructor
public class CommentReaction extends BaseDocument {

    @Field("id")
    private String id;
    @Field("comment_id")
    private String commentId;
    @Field("user_id")
    private String userId;
    private String action;
}
