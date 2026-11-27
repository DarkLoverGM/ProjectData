package com.yt.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "membership_tiers")
public class MembershipTier {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "channel_id", nullable = false, length = 64)
    private String channelId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(length = 500)
    private String perks;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public MembershipTier() {
    }

    public MembershipTier(String id, String channelId, String name, BigDecimal price, String perks, boolean active, Instant createdAt) {
        this.id = id;
        this.channelId = channelId;
        this.name = name;
        this.price = price;
        this.perks = perks;
        this.active = active;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getPerks() { return perks; }
    public void setPerks(String perks) { this.perks = perks; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
