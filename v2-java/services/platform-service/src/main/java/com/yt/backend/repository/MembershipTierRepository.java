package com.yt.backend.repository;

import com.yt.backend.entity.MembershipTier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Goi hoi vien. */
public interface MembershipTierRepository extends MongoRepository<MembershipTier, String> {

    Optional<MembershipTier> findOneById(String id);

    List<MembershipTier> findByIdIn(Collection<String> ids);

    List<MembershipTier> findByChannelIdOrderByPriceAsc(String channelId);

    @Query("{ 'channel_id': ?0, 'active': { '$ne': false } }")
    List<MembershipTier> findOnSale(String channelId, Sort sort);

    @Query(value = "{ 'channel_id': ?0, 'active': { '$ne': false } }", count = true)
    long countOnSale(String channelId);

    @Query(value = "{ 'channel_id': ?0, 'name': ?1, 'active': { '$ne': false }, 'id': { '$ne': ?2 } }",
            collation = "{ 'locale': 'en', 'strength': 2 }", exists = true)
    boolean existsNameOnSale(String channelId, String name, String exceptId);

    long deleteOneById(String id);

    long deleteByChannelId(String channelId);
}
