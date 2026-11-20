package com.yt.backend.repository;

import com.yt.backend.entity.PushToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** FCM token. */
public interface PushTokenRepository extends MongoRepository<PushToken, String> {

    Optional<PushToken> findOneByToken(String token);

    List<PushToken> findByUserIdIn(Collection<String> userIds);

    long deleteOneByToken(String token);

    long deleteByTokenIn(Collection<String> tokens);
}
