package com.yt.backend.controller;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.common.ApiException;
import com.yt.backend.dto.membership.*;
import com.yt.backend.dto.payment.RevenueResponse;
import com.yt.backend.service.MembershipService;
import com.yt.backend.service.PaymentService;
import com.yt.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Hoi vien kenh:
 *   GET /api/channels/{id}/membership           cac goi cua kenh + goi cua nguoi xem (trang xem video, profile)
 *   /api/studio/membership/tiers[/{tierId}]     chu kenh them / sua / an / xoa goi
 *   GET /api/studio/membership/members          hoi vien dang con han
 *   GET /api/studio/revenue                     doanh thu (hoi vien + Super Thanks)
 */
@RestController
@RequestMapping("/api")
public class MembershipController {

    private final MembershipService membershipService;
    private final PaymentService paymentService;
    private final UserService userService;

    public MembershipController(MembershipService membershipService, PaymentService paymentService, UserService userService) {
        this.membershipService = membershipService;
        this.paymentService = paymentService;
        this.userService = userService;
    }

    @GetMapping("/channels/{id}/membership")
    public ChannelMembershipResponse channel(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        if (userService.findUserById(id) == null) throw ApiException.notFound("Không tìm thấy kênh");
        return membershipService.channelInfo(id, user == null ? null : user.id());
    }

    // ================= Studio =================

    @GetMapping("/studio/membership/tiers")
    @PreAuthorize("isAuthenticated()")
    public TiersResponse tiers(@AuthenticationPrincipal AuthUser me) {
        return new TiersResponse(membershipService.listTiers(me.id(), true), new TiersResponse.Limits(
                MembershipService.MAX_TIERS, MembershipService.PRICE_MIN, MembershipService.PRICE_MAX));
    }

    @PostMapping("/studio/membership/tiers")
    @PreAuthorize("isAuthenticated()")
    public TierResponse createTier(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody TierRequest body) {
        return membershipService.createTier(me.id(), body);
    }

    /** Field khong gui thi giu nguyen */
    @PatchMapping("/studio/membership/tiers/{tierId}")
    @PreAuthorize("isAuthenticated()")
    public TierResponse updateTier(@AuthenticationPrincipal AuthUser me, @PathVariable String tierId,
                                   @Valid @RequestBody TierUpdateRequest body) {
        return membershipService.updateTier(me.id(), tierId, body);
    }

    @DeleteMapping("/studio/membership/tiers/{tierId}")
    @PreAuthorize("isAuthenticated()")
    public DeleteTierResponse deleteTier(@AuthenticationPrincipal AuthUser me, @PathVariable String tierId) {
        return membershipService.deleteTier(me.id(), tierId);
    }

    @GetMapping("/studio/membership/members")
    @PreAuthorize("isAuthenticated()")
    public MembersResponse members(@AuthenticationPrincipal AuthUser me) {
        return new MembersResponse(membershipService.listMembers(me.id()));
    }

    @GetMapping("/studio/revenue")
    @PreAuthorize("isAuthenticated()")
    public RevenueResponse revenue(@AuthenticationPrincipal AuthUser me) {
        return paymentService.creatorRevenue(me.id());
    }
}
