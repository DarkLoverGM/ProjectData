package com.yt.backend.dto.studio;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yt.backend.entity.VideoMessage;

import java.util.Date;

/** 1 tin trao doi ve video. role: creator | admin | system */
public record VideoMessageDto(String id, @JsonProperty("video_id") String videoId, String role,
                              @JsonProperty("sender_id") String senderId,
                              @JsonProperty("sender_name") String senderName,
                              String content, @JsonProperty("created_at") Date createdAt) {

    public static VideoMessageDto from(VideoMessage m) {
        return new VideoMessageDto(m.getId(), m.getVideoId(), m.getRole(), m.getSenderId(), m.getSenderName(),
                m.getContent(), m.getCreatedAt());
    }

    /** Ban cho nguoi dang xem: an id / ten admin (admin tra loi duoi ten "Quan tri vien") */
    public VideoMessageDto forCreator() {
        return new VideoMessageDto(id, videoId, role, null, "creator".equals(role) ? senderName : null, content, createdAt);
    }
}
