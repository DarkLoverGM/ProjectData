package com.yt.backend.dto.video;

import java.util.List;

public record UploadVideoResponse(String message, VideoDto video, List<String> subtitleErrors) {}
