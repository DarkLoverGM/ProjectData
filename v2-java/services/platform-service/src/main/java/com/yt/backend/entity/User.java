package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/** Tai khoan (collection users). password null = tai khoan tao bang Google, chua dat mat khau. */
@Document("users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseDocument {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";

    @Field("id")
    private String id;
    private String username;
    private String email;
    private String password;
    private String role;
    private String avatar;
    private String banner;
    @Field("google_id")
    private String googleId;
    @Field("created_at")
    private Date createdAt;
    /** Khoa den ngay nay (BanList.PERMANENT = vinh vien), null = khong bi khoa */
    @Field("banned_until")
    private Date bannedUntil;
    @Field("ban_reason")
    private String banReason;

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }
}
