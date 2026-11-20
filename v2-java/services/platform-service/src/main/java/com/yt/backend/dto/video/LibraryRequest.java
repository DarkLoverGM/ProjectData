package com.yt.backend.dto.video;

import jakarta.validation.constraints.NotBlank;

public record LibraryRequest(@NotBlank(message = "Thiếu videoId") String videoId) {}
