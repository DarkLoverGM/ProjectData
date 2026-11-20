package com.yt.backend.dto.membership;

import java.util.List;

public record TiersResponse(List<TierDto> tiers, Limits limits) {

    public record Limits(int maxTiers, long priceMin, long priceMax) {}
}
