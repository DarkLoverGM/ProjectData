package com.yt.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "user_name", length = 120)
    private String userName;

    @Column(nullable = false, length = 30)
    private String type; // MEMBERSHIP | SUPER_THANKS

    @Column(name = "channel_id", nullable = false, length = 64)
    private String channelId;

    @Column(name = "channel_name", length = 120)
    private String channelName;

    @Column(name = "video_id", length = 64)
    private String videoId;

    @Column(name = "video_title", length = 255)
    private String videoTitle;

    @Column(name = "tier_id", length = 64)
    private String tierId;

    @Column(name = "tier_name", length = 120)
    private String tierName;

    private Integer months;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 300)
    private String message;

    @Column(nullable = false, length = 20)
    private String provider; // MOMO | VNPAY | BANK

    @Column(nullable = false, length = 20)
    private String status;   // PENDING | PAID | CANCELLED

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Order() {
    }

    public Order(String id, String userId, String userName, String type, String channelId, String channelName,
                 String videoId, String videoTitle, String tierId, String tierName, Integer months,
                 BigDecimal unitPrice, BigDecimal totalAmount, String message, String provider, String status,
                 Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.type = type;
        this.channelId = channelId;
        this.channelName = channelName;
        this.videoId = videoId;
        this.videoTitle = videoTitle;
        this.tierId = tierId;
        this.tierName = tierName;
        this.months = months;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.message = message;
        this.provider = provider;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }

    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }

    public String getVideoTitle() { return videoTitle; }
    public void setVideoTitle(String videoTitle) { this.videoTitle = videoTitle; }

    public String getTierId() { return tierId; }
    public void setTierId(String tierId) { this.tierId = tierId; }

    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }

    public Integer getMonths() { return months; }
    public void setMonths(Integer months) { this.months = months; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
