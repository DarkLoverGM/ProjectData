package com.yt.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yt.backend.dto.user.BanInfo;

import java.util.Date;

/** 1 bao cao nguoi dung kem thong tin nguoi bi bao cao. pending_against: so bao cao dang cho ve nguoi nay */
public record UserReportRow(String id, @JsonProperty("reported_user") String reportedUser, String reporter,
                            String reason, String detail, String status, @JsonProperty("created_at") Date createdAt,
                            @JsonProperty("resolved_at") Date resolvedAt,
                            @JsonProperty("reporter_name") String reporterName,
                            @JsonProperty("reported_name") String reportedName,
                            @JsonProperty("reported_avatar") String reportedAvatar,
                            @JsonProperty("reported_role") String reportedRole,
                            @JsonProperty("reported_ban") BanInfo reportedBan,
                            @JsonProperty("pending_against") long pendingAgainst) {}
