package com.yt.backend.dto.payment;

import java.util.List;

/** Bang gia chung: giam gia theo so thang, muc Super Thanks, phi nen tang, cac cong */
public record PricingResponse(List<MonthOption> months, SuperThanks superThanks, int platformFeePercent,
                              List<ProviderDto> providers) {

    public record MonthOption(int months, int discountPercent) {}

    public record SuperThanks(long min, long max, List<Long> presets, int messageMax) {}
}
