package com.yt.backend.dto.studio;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

/**
 * 1 dong trong danh sach video cua Studio.
 * state: public | unlisted | private | blocked | resubmitted | hidden (gop tu status + ket qua duyet + visibility)
 * visibility: che do nguoi dang chon, giu rieng de Studio hien dung lua chon ca khi admin dang an video
 */
public record StudioVideoItem(String id, String title, String slug, String thumbnail,
                              @JsonProperty("video_group") String videoGroup, Date uploadAt,
                              @JsonProperty("media_updated_at") Date mediaUpdatedAt, String state,
                              PublicModeration moderation, boolean appealPending, long messages,
                              @JsonProperty("members_only") boolean membersOnly, String visibility, Stats stats) {

    public record Stats(long view, long likes, long comments) {}
}
