package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.Locale;

/**
 * 1 don thanh toan (collection orders). Tien tinh bang VND, so nguyen.
 * Don chi doi trang thai qua PaymentService: PENDING -> PAID | FAILED, qua han ma chua tra thi coi la EXPIRED.
 * type / status luu chuoi chu thuong ("membership", "paid"...), doc ra enum qua typeEnum() / statusEnum().
 */
@Document("orders")
@Getter
@Setter
@NoArgsConstructor
public class Order extends BaseDocument {

    /** Don chua thanh toan sau chung nay phut thi het han (link thanh toan cua cong cung het han) */
    public static final int EXPIRES_MINUTES = 15;

    public enum Type {
        MEMBERSHIP, SUPER_THANKS;

        public String code() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** "membership" / "super_thanks" -> enum, sai thi null */
        public static Type of(String code) {
            for (Type t : values()) if (t.code().equals(code)) return t;
            return null;
        }
    }

    public enum Status {
        PENDING, PAID, FAILED, EXPIRED;

        public String code() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Status of(String code) {
            for (Status s : values()) if (s.code().equals(code)) return s;
            return null;
        }
    }

    @Field("id")
    private String id;
    @Field("user_id")
    private String userId;
    private String type;
    /** Nguoi nhan tien (chu kenh) */
    @Field("channel_id")
    private String channelId;
    /** Chi co voi Super Thanks */
    @Field("video_id")
    private String videoId;
    /** Chi co voi hoi vien (months = so thang mua) */
    @Field("tier_id")
    private String tierId;
    private Integer months;
    private Long amount;
    private String message;
    /** momo | vnpay */
    private String provider;
    private String status;
    @Field("provider_txn")
    private String providerTxn;
    private String note;
    /** Binh luan Super Thanks tao ra sau khi thanh toan */
    @Field("comment_id")
    private String commentId;
    @Field("created_at")
    private Date createdAt;
    @Field("paid_at")
    private Date paidAt;

    /** Don moi tao, chua thanh toan */
    public static Order create(String id, String userId, Type type, String channelId, String videoId, String tierId,
                               int months, long amount, String message, String provider) {
        Order o = new Order();
        o.id = id;
        o.userId = userId;
        o.type = type.code();
        o.channelId = channelId;
        o.videoId = videoId;
        o.tierId = tierId;
        o.months = months;
        o.amount = amount;
        o.message = message;
        o.provider = provider;
        o.status = Status.PENDING.code();
        o.createdAt = new Date();
        return o;
    }

    public Type typeEnum() {
        return Type.of(type);
    }

    public Status statusEnum() {
        return Status.of(status);
    }

    public long amountValue() {
        return amount == null ? 0 : amount;
    }

    /** Trang thai nguoi dung thay: don cho qua EXPIRES_MINUTES coi nhu het han */
    public Status effectiveStatus() {
        Status s = statusEnum();
        if (s == Status.PENDING && createdAt != null
                && createdAt.getTime() + EXPIRES_MINUTES * 60_000L < System.currentTimeMillis()) {
            return Status.EXPIRED;
        }
        return s;
    }
}
