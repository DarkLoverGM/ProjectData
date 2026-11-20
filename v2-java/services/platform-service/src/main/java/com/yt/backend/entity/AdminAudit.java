package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.Map;

/** Nhat ky thao tac cua admin (collection admin_audit). detail: du lieu tuy thao tac. */
@Document("admin_audit")
@Getter
@Setter
@NoArgsConstructor
public class AdminAudit extends BaseDocument {

    @Field("id")
    private String id;
    @Field("admin_id")
    private String adminId;
    @Field("admin_name")
    private String adminName;
    private String action;
    @Field("target_type")
    private String targetType;
    @Field("target_id")
    private String targetId;
    @Field("target_label")
    private String targetLabel;
    private Map<String, Object> detail;
    private String ip;
    @Field("created_at")
    private Date createdAt;
}
