package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/** Goi hoi vien cua 1 kenh (collection membership_tiers). active = false: an, khong ban nua. */
@Document("membership_tiers")
@Getter
@Setter
@NoArgsConstructor
public class MembershipTier extends BaseDocument {

    @Field("id")
    private String id;
    @Field("channel_id")
    private String channelId;
    private String name;
    private Long price;
    private String perks;
    private Boolean active;
    @Field("created_at")
    private Date createdAt;
    @Field("updated_at")
    private Date updatedAt;

    /** Goi dang ban (active null = du lieu cu, coi nhu dang ban) */
    public boolean isOnSale() {
        return !Boolean.FALSE.equals(active);
    }
}
