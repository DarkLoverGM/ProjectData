package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/** Like / dislike video (collection action_video). Moi user chi 1 cam xuc tren 1 video. action: like | dislike */
@Document("action_video")
@Getter
@Setter
@NoArgsConstructor
public class VideoReaction extends BaseDocument {

    @Field("id")
    private String id;
    @Field("video_id")
    private String videoId;
    @Field("user_id")
    private String userId;
    private String action;
}
