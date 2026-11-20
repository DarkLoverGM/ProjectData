package com.yt.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yt.backend.entity.AdminAudit;

import java.util.Date;
import java.util.Map;

/** 1 dong nhat ky admin. detail: du lieu tuy thao tac (vd. { status: 0 }, { rules, note }) */
public record AuditDto(String id, @JsonProperty("admin_id") String adminId, @JsonProperty("admin_name") String adminName,
                       String action, @JsonProperty("target_type") String targetType,
                       @JsonProperty("target_id") String targetId, @JsonProperty("target_label") String targetLabel,
                       Map<String, Object> detail, String ip, @JsonProperty("created_at") Date createdAt) {

    public static AuditDto from(AdminAudit a) {
        return new AuditDto(a.getId(), a.getAdminId(), a.getAdminName(), a.getAction(), a.getTargetType(), a.getTargetId(),
                a.getTargetLabel(), a.getDetail(), a.getIp(), a.getCreatedAt());
    }
}
