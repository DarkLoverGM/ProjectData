package com.yt.backend.dto.payment;

import com.yt.backend.dto.membership.MyMembershipDto;

import java.util.List;

/** Lich su giao dich (phan trang) + cac kenh dang la hoi vien */
public record MyOrdersResponse(List<OrderDto> items, long total, int page, int limit, List<MyMembershipDto> memberships) {}
