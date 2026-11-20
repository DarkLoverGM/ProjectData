package com.yt.backend.dto.subtitle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yt.backend.entity.Subtitle;

import java.util.Date;
import java.util.List;

/** Phu de 1 ngon ngu: { lang, label, cues: [{ start, end, text }], updated_at } */
public record SubtitleDto(String lang, String label, List<Subtitle.Cue> cues, @JsonProperty("updated_at") Date updatedAt) {}
