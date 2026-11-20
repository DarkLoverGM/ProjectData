package com.yt.backend.dto.payment;

import java.util.List;

/** Doanh thu cua chu kenh (Studio > Kiem tien) */
public record RevenueResponse(Summary summary, int platformFeePercent, List<Month> monthly, List<OrderDto> recent) {

    /** gross: tong tien nguoi xem tra; net: phan chu kenh nhan sau phi nen tang */
    public record Summary(long gross, long net, long membership, long superThanks, long superThanksCount, long orders,
                          long activeMembers) {}

    /** month: "2026-10" */
    public record Month(String month, long membership, long superThanks, long total) {}
}
