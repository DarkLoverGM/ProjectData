package com.yt.backend.repository;

import com.yt.backend.entity.Membership;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/** Hoi vien. */
public interface MembershipRepository extends MongoRepository<Membership, String> {

    Optional<Membership> findOneByUserIdAndChannelId(String userId, String channelId);

    Optional<Membership> findOneByUserIdAndChannelIdAndExpiresAtAfter(String userId, String channelId, Date now);

    List<Membership> findByChannelIdAndExpiresAtAfterOrderByExpiresAtAsc(String channelId, Date now);

    List<Membership> findByUserIdAndExpiresAtAfterOrderByExpiresAtAsc(String userId, Date now);

    List<Membership> findByChannelIdAndUserIdInAndExpiresAtAfter(String channelId, Collection<String> userIds, Date now);

    long countByChannelIdAndExpiresAtAfter(String channelId, Date now);

    long countByTierIdAndExpiresAtAfter(String tierId, Date now);

    long deleteByUserIdOrChannelId(String userId, String channelId);
}
