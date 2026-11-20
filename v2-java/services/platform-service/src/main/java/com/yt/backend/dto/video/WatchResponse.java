package com.yt.backend.dto.video;

/** locked = true: video chi danh cho hoi vien ma nguoi xem chua tham gia (media.src rong) */
public record WatchResponse(VideoDto video, boolean locked) {}
