package com.yt.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yt.backend.entity.Video;

import java.util.Date;
import java.util.List;

/** Ket qua duyet day du (cho admin): ai duyet, luc nao, rule nao, giay thu bao nhieu */
public record ModerationDto(String state, List<String> rules, String note, @JsonProperty("at_time") Long atTime,
                            String by, @JsonProperty("by_name") String byName, Date at) {

    public static ModerationDto from(Video.Moderation m) {
        return m == null ? null : new ModerationDto(m.getState(), m.getRules(), m.getNote(), m.getAtTime(), m.getBy(),
                m.getByName(), m.getAt());
    }
}
