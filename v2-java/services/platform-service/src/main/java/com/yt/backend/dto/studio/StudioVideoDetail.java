package com.yt.backend.dto.studio;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

/** Chi tiet 1 video de sua trong Studio + lich su trao doi voi admin */
public record StudioVideoDetail(String id, String title, String slug, String description, String src, String thumbnail,
                                @JsonProperty("video_group") String videoGroup,
                                @JsonProperty("members_only") boolean membersOnly, String visibility, Date uploadAt,
                                @JsonProperty("media_updated_at") Date mediaUpdatedAt, String state,
                                PublicModeration moderation, boolean appealPending, List<String> tags,
                                StudioVideoItem.Stats stats, List<VideoMessageDto> messages) {}
