package com.yt.backend.dto.admin;

import com.yt.backend.dto.studio.VideoMessageDto;

public record ReplyResponse(boolean success, String title, VideoMessageDto item) {}
