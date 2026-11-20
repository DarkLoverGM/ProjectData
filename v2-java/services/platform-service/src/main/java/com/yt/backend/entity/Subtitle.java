package com.yt.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.List;

/** Phu de 1 ngon ngu cua 1 video (collection subtitles), unique (video_id, lang). Cac dong luu thang trong mang cues. */
@Document("subtitles")
@Getter
@Setter
@NoArgsConstructor
public class Subtitle extends BaseDocument {

    @Field("video_id")
    private String videoId;
    /** vi | en (xem SubtitleService.SUBTITLE_LANGS) */
    private String lang;
    private List<Cue> cues;
    @Field("updated_at")
    private Date updatedAt;
    @Field("updated_by")
    private String updatedBy;

    /** 1 dong phu de, thoi gian tinh bang giay */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cue {
        private Double start;
        private Double end;
        private String text;
    }
}
