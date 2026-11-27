package com.yt.payment.service;

import com.yt.payment.client.UserClient;
import com.yt.payment.dto.MembershipTierRequest;
import com.yt.payment.dto.MembershipTierResponse;
import com.yt.payment.entity.MembershipTier;
import com.yt.payment.exception.ResourceNotFoundException;
import com.yt.payment.repository.MembershipTierRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MembershipTierService {

    private final MembershipTierRepository repository;
    private final UserClient userClient;

    public MembershipTierService(MembershipTierRepository repository, UserClient userClient) {
        this.repository = repository;
        this.userClient = userClient;
    }

    private MembershipTierResponse toResponse(MembershipTier tier) {
        return new MembershipTierResponse(
                tier.getId(),
                tier.getChannelId(),
                tier.getName(),
                tier.getPrice(),
                tier.getPerks(),
                tier.isActive(),
                tier.getCreatedAt()
        );
    }

    public List<MembershipTierResponse> getAll(String channelId, Boolean activeOnly) {
        List<MembershipTier> tiers;
        if (channelId != null && !channelId.isBlank()) {
            tiers = repository.findByChannelId(channelId.trim());
        } else if (Boolean.TRUE.equals(activeOnly)) {
            tiers = repository.findByActiveTrue();
        } else {
            tiers = repository.findAll();
        }
        return tiers.stream().map(this::toResponse).toList();
    }

    public MembershipTierResponse getById(String id) {
        MembershipTier tier = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói hội viên: " + id));
        return toResponse(tier);
    }

    public MembershipTierResponse create(MembershipTierRequest request) {
        // Kiểm tra kênh tồn tại ở User Service qua RestClient
        userClient.getById(request.channelId().trim());

        MembershipTier tier = new MembershipTier(
                "tier-" + UUID.randomUUID().toString().substring(0, 8),
                request.channelId().trim(),
                request.name().trim(),
                request.price(),
                request.perks() == null ? "" : request.perks().trim(),
                request.active() == null || request.active(),
                Instant.now()
        );
        return toResponse(repository.save(tier));
    }

    public MembershipTierResponse update(String id, MembershipTierRequest request) {
        MembershipTier tier = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói hội viên: " + id));

        userClient.getById(request.channelId().trim());

        tier.setChannelId(request.channelId().trim());
        tier.setName(request.name().trim());
        tier.setPrice(request.price());
        tier.setPerks(request.perks() == null ? "" : request.perks().trim());
        if (request.active() != null) {
            tier.setActive(request.active());
        }
        return toResponse(repository.save(tier));
    }

    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy gói hội viên: " + id);
        }
        repository.deleteById(id);
    }
}
