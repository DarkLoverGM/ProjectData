package com.yt.backend.repository;

import com.yt.backend.entity.PasswordReset;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/** OTP quen mat khau. */
public interface PasswordResetRepository extends MongoRepository<PasswordReset, String> {

    Optional<PasswordReset> findFirstByUserIdOrderByCreatedAtDesc(String userId);

    long deleteByUserId(String userId);
}
