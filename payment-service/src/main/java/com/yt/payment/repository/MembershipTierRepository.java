package com.yt.payment.repository;

import com.yt.payment.entity.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipTierRepository extends JpaRepository<MembershipTier, String> {
    List<MembershipTier> findByChannelId(String channelId);
    List<MembershipTier> findByActiveTrue();
}
