package com.yt.backend.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/** Ma xac minh email khi dang ky (collection email_verification), moi email 1 ma. */
@Document("email_verification")
@Getter
@Setter
@NoArgsConstructor
public class EmailVerification extends BaseDocument {

    private String email;
    @Field("code_hash")
    private String codeHash;
    private Integer attempts;
    @Field("createAt")
    private Date createdAt;
    private Date expiresAt;
}
