package com.yt.backend.repository;

import com.yt.backend.entity.EmailVerification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/** Ma xac minh email. */
public interface EmailVerificationRepository extends MongoRepository<EmailVerification, String> {

    Optional<EmailVerification> findOneByEmail(String email);

    long deleteByEmail(String email);
}
