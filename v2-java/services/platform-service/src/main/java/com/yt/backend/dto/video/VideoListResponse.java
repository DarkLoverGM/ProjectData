package com.yt.backend.dto.video;

import java.util.List;

/** { videos: [...] } - danh sach phang (video cua 1 nguoi dang) */
public record VideoListResponse(List<VideoDto> videos) {}
