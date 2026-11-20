package com.yt.backend.dto.video;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.List;

/** Danh sach video tach 2 nhom: { Main: [...], Short: [...] } */
public record GroupedVideos(@JsonProperty("Main") List<VideoDto> main, @JsonProperty("Short") List<VideoDto> shorts) {

    public static final GroupedVideos EMPTY = new GroupedVideos(List.of(), List.of());

    /**
     * @param mainIsNotShort true: moi video khong phai short deu vao Main; false: Main chi gom video_group = main
     */
    public static GroupedVideos split(List<VideoDto> videos, boolean mainIsNotShort) {
        return new GroupedVideos(
                videos.stream().filter(v -> mainIsNotShort ? !"short".equals(v.videoGroup()) : "main".equals(v.videoGroup())).toList(),
                videos.stream().filter(v -> "short".equals(v.videoGroup())).toList());
    }

    /** Bo video cua nhung nguoi dang nay (nguoi xem da chan ho) */
    public GroupedVideos withoutUploaders(Collection<String> uploaderIds) {
        if (uploaderIds.isEmpty()) return this;
        return new GroupedVideos(
                main.stream().filter(v -> !uploaderIds.contains(v.uploaderId())).toList(),
                shorts.stream().filter(v -> !uploaderIds.contains(v.uploaderId())).toList());
    }
}
