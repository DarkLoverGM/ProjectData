package com.yt.backend.dto.studio;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yt.backend.common.VideoRules;
import com.yt.backend.entity.Video;

import java.util.Date;
import java.util.List;

/** Ket qua duyet cho nguoi dang xem: chi gom nhung gi ho can, khong lo id admin */
public record PublicModeration(String state, List<RuleLabel> rules, String note, @JsonProperty("at_time") Long atTime, Date at) {

    public record RuleLabel(String id, String label) {}

    public static PublicModeration from(Video.Moderation m) {
        if (m == null) return null;
        List<RuleLabel> rules = m.getRules() == null ? List.of()
                : m.getRules().stream().map(id -> new RuleLabel(id, VideoRules.ruleLabel(id))).toList();
        return new PublicModeration(m.getState(), rules, m.getNote() == null ? "" : m.getNote(), m.getAtTime(), m.getAt());
    }
}
