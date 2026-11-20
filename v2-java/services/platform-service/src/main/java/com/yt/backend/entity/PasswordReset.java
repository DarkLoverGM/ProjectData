package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/** OTP quen mat khau (collection user_resetpass). Chi luu hash cua OTP. attempts luu o field 'attemps' (ten cu). */
@Document("user_resetpass")
@Getter
@Setter
@NoArgsConstructor
public class PasswordReset extends BaseDocument {

    @Field("id")
    private String id;
    @Field("user_id")
    private String userId;
    @Field("otp_hash")
    private String otpHash;
    @Field("attemps")
    private Integer attempts;
    private Integer verified;
    @Field("createAt")
    private Date createdAt;
    private Date expiresAt;
}
