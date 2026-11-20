package com.yt.backend.dto.payment;

import java.util.List;

/** Trang Thanh toan cua admin: 1 trang giao dich + tong doanh thu */
public record AdminPaymentsResponse(List<OrderDto> items, long total, int page, int limit, Summary summary,
                                    List<ProviderDto> providers) {

    public record Summary(long paidOrders, long gross, long platformFee, long pending, long failed) {}
}
