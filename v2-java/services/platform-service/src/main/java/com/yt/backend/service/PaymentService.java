package com.yt.backend.service;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.common.ApiException;
import com.yt.backend.common.MongoUtils;
import com.yt.backend.dto.Paging;
import com.yt.backend.dto.comment.CommentDto;
import com.yt.backend.dto.payment.*;
import com.yt.backend.entity.*;
import com.yt.backend.payment.PaymentProvider;
import com.yt.backend.repository.*;
import com.yt.backend.websocket.Realtime;
import com.yt.backend.websocket.RealtimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Don hang + thanh toan cho 2 nghiep vu thuong mai:
 * - MEMBERSHIP: mua goi hoi vien kenh 1 / 3 / 6 / 12 thang (mua dai duoc giam gia)
 * - SUPER_THANKS: tang tien cho 1 video, kem binh luan duoc to noi bat
 * Tien di qua cong online (PaymentProvider: MoMo, VNPay). Nen tang giu PLATFORM_FEE_PERCENT, phan con lai la cua chu kenh.
 *
 * Mot don co the duoc bao ket qua 2 lan (trinh duyet quay ve return URL + cong goi IPN), nen:
 * - chi doi PENDING -> PAID bang 1 lenh findAndModify co dieu kien -> chi 1 request kich hoat quyen loi
 * - so tien cong bao ve phai khop don, chu ky sai thi bo qua
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public static final int PLATFORM_FEE_PERCENT = 30;
    public static final long SUPER_THANKS_MIN = 10_000;
    public static final long SUPER_THANKS_MAX = 5_000_000;
    public static final List<Long> SUPER_THANKS_PRESETS = List.of(10_000L, 20_000L, 50_000L, 100_000L, 200_000L, 500_000L);
    private static final int MESSAGE_MAX = 200;
    /** So giao dich chua xong toi da cua 1 nguoi trong EXPIRES_MINUTES (chong spam tao don) */
    private static final int MAX_PENDING = 5;
    /** So thang duoc mua -> % giam gia */
    private static final Map<Integer, Integer> MONTH_DISCOUNTS = new LinkedHashMap<>();

    static {
        MONTH_DISCOUNTS.put(1, 0);
        MONTH_DISCOUNTS.put(3, 0);
        MONTH_DISCOUNTS.put(6, 5);
        MONTH_DISCOUNTS.put(12, 10);
    }

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(VN);

    private final OrderRepository orders;
    private final UserRepository users;
    private final VideoRepository videos;
    private final MembershipTierRepository tiers;
    private final MembershipService memberships;
    private final CommentService comments;
    private final NotificationService notifications;
    private final Realtime realtime;
    private final MongoTemplate mongo;
    /** id cong -> cai dat, giu thu tu Spring tiem vao */
    private final Map<String, PaymentProvider> providers = new LinkedHashMap<>();

    public PaymentService(OrderRepository orders, UserRepository users, VideoRepository videos,
                          MembershipTierRepository tiers, MembershipService memberships, CommentService comments,
                          NotificationService notifications, Realtime realtime, MongoTemplate mongo,
                          List<PaymentProvider> providers) {
        this.orders = orders;
        this.users = users;
        this.videos = videos;
        this.tiers = tiers;
        this.memberships = memberships;
        this.comments = comments;
        this.notifications = notifications;
        this.realtime = realtime;
        this.mongo = mongo;
        providers.forEach(p -> this.providers.put(p.id(), p));
    }

    /** Ket qua xu ly 1 lan cong bao ve (VNPay IPN can phan biet tung truong hop) */
    public enum CallbackOutcome { CONFIRMED, ALREADY_CONFIRMED, INVALID_SIGNATURE, ORDER_NOT_FOUND, AMOUNT_MISMATCH }

    public record CallbackResult(CallbackOutcome outcome, String orderId) {}

    static String money(long amount) {
        return String.format(Locale.forLanguageTag("vi-VN"), "%,d", amount) + "đ";
    }

    // ================= Thong tin cho giao dien =================

    /** Cac cong + co dung duoc khong (thieu key thi enabled = false) */
    public List<ProviderDto> providers() {
        return providers.values().stream().map(p -> new ProviderDto(p.id(), p.label(), p.isConfigured())).toList();
    }

    /** Bang gia chung: giam gia theo so thang, muc Super Thanks, phi nen tang */
    public PricingResponse pricing() {
        List<PricingResponse.MonthOption> months = MONTH_DISCOUNTS.entrySet().stream()
                .map(e -> new PricingResponse.MonthOption(e.getKey(), e.getValue())).toList();
        return new PricingResponse(months,
                new PricingResponse.SuperThanks(SUPER_THANKS_MIN, SUPER_THANKS_MAX, SUPER_THANKS_PRESETS, MESSAGE_MAX),
                PLATFORM_FEE_PERCENT, providers());
    }

    /** Gia goi `months` thang sau giam gia, lam tron xuong 1.000d */
    static long membershipAmount(long monthlyPrice, int months) {
        long gross = monthlyPrice * months;
        long discounted = gross * (100 - MONTH_DISCOUNTS.getOrDefault(months, 0)) / 100;
        return discounted / 1000 * 1000;
    }

    // ================= Tao don =================

    /** Tao don PENDING roi xin link thanh toan tu cong */
    public CheckoutResponse checkout(AuthUser user, CheckoutRequest req, String clientIp) {
        PaymentProvider provider = providers.get(req.provider());
        if (provider == null || !provider.isConfigured()) throw ApiException.badRequest("Cổng thanh toán không khả dụng");

        Date cutoff = new Date(System.currentTimeMillis() - Order.EXPIRES_MINUTES * 60_000L);
        if (orders.countByUserIdAndStatusAndCreatedAtAfter(user.id(), Order.Status.PENDING.code(), cutoff) >= MAX_PENDING) {
            throw new ApiException(429, "Bạn có quá nhiều giao dịch chưa hoàn tất, vui lòng thử lại sau "
                    + Order.EXPIRES_MINUTES + " phút");
        }

        Order order;
        String description;
        if (Order.Type.MEMBERSHIP.code().equals(req.type())) {
            MembershipTier tier = memberships.findTier(req.tierId());
            if (tier == null || !tier.isOnSale()) throw ApiException.notFound("Gói hội viên không tồn tại hoặc đã ngừng bán");
            String channelId = tier.getChannelId();
            if (channelId.equals(user.id())) throw ApiException.badRequest("Không thể tự tham gia hội viên kênh của mình");
            if (req.months() == null || !MONTH_DISCOUNTS.containsKey(req.months())) throw ApiException.badRequest("Số tháng không hợp lệ");
            User channel = users.findOneById(channelId).orElseThrow(() -> ApiException.notFound("Kênh không tồn tại"));

            long total = membershipAmount(tier.getPrice(), req.months());
            order = Order.create(MongoUtils.uuid(), user.id(), Order.Type.MEMBERSHIP, channelId, null, tier.getId(),
                    req.months(), total, null, provider.id());
            description = "Hội viên " + tier.getName() + " kênh " + channel.getUsername() + " - " + req.months() + " tháng";
        } else {
            Video video = req.videoId() == null ? null
                    : videos.findOneByIdAndStatus(req.videoId(), Video.VISIBLE).orElse(null);
            if (video == null) throw ApiException.notFound("Video không tồn tại");
            String channelId = video.getUserId();
            if (user.id().equals(channelId)) throw ApiException.badRequest("Không thể tự gửi Super Thanks cho video của mình");
            Long amount = req.amount();
            if (amount == null || amount < SUPER_THANKS_MIN || amount > SUPER_THANKS_MAX) {
                throw ApiException.badRequest("Số tiền từ " + money(SUPER_THANKS_MIN) + " đến " + money(SUPER_THANKS_MAX));
            }
            if (amount % 1000 != 0) throw ApiException.badRequest("Số tiền phải là bội số của 1.000đ");

            order = Order.create(MongoUtils.uuid(), user.id(), Order.Type.SUPER_THANKS, channelId, video.getId(), null, 0,
                    amount, req.message() == null ? "" : req.message().trim(), provider.id());
            description = "Super Thanks cho video " + video.getTitle();
        }

        orders.insert(order);
        try {
            String payUrl = provider.createPayUrl(order, description, clientIp);
            return new CheckoutResponse(true, order.getId(), payUrl, order.amountValue());
        } catch (IOException | RuntimeException e) {
            log.error("[payment] Không tạo được giao dịch {} qua {}: {}", order.getId(), provider.id(), e.getMessage());
            mongo.updateFirst(byId(order.getId()), new Update().set("status", Order.Status.FAILED.code())
                    .set("note", "Không tạo được giao dịch: " + e.getMessage()), Order.class);
            throw new ApiException(502, "Không kết nối được tới " + provider.label() + ", vui lòng thử lại hoặc chọn cổng khác");
        }
    }

    private static Query byId(String orderId) {
        return Query.query(Criteria.where("id").is(orderId));
    }

    // ================= Cong bao ket qua =================

    /** Xu ly tham so cong gui ve (return URL hoac IPN). Goi nhieu lan cho cung 1 don van an toan. */
    public CallbackResult handleCallback(String providerId, Map<String, String> params) {
        PaymentProvider provider = providers.get(providerId);
        if (provider == null || !provider.isConfigured()) return new CallbackResult(CallbackOutcome.INVALID_SIGNATURE, null);

        PaymentProvider.PaymentResult result = provider.verify(params);
        if (!result.validSignature()) {
            log.warn("[payment] Sai chữ ký từ {}: {}", providerId, params);
            return new CallbackResult(CallbackOutcome.INVALID_SIGNATURE, null);
        }

        Order order = result.orderId() == null ? null : orders.findOneById(result.orderId()).orElse(null);
        if (order == null || !providerId.equals(order.getProvider())) {
            return new CallbackResult(CallbackOutcome.ORDER_NOT_FOUND, result.orderId());
        }
        if (order.statusEnum() == Order.Status.PAID) return new CallbackResult(CallbackOutcome.ALREADY_CONFIRMED, order.getId());
        if (result.amount() != order.amountValue()) {
            markFailed(order.getId(), "Số tiền cổng báo về (" + money(result.amount()) + ") không khớp đơn hàng");
            return new CallbackResult(CallbackOutcome.AMOUNT_MISMATCH, order.getId());
        }
        if (!result.success()) {
            markFailed(order.getId(), result.message());
            return new CallbackResult(CallbackOutcome.CONFIRMED, order.getId());
        }

        // Tien da bi tru nen don FAILED (vd. bao huy truoc roi moi bao thanh cong) cung phai chuyen sang PAID
        Order paid = mongo.findAndModify(
                Query.query(Criteria.where("id").is(order.getId())
                        .and("status").in(Order.Status.PENDING.code(), Order.Status.FAILED.code())),
                new Update().set("status", Order.Status.PAID.code()).set("paid_at", new Date())
                        .set("provider_txn", result.transactionId()).set("note", null),
                FindAndModifyOptions.options().returnNew(true), Order.class);
        if (paid == null) return new CallbackResult(CallbackOutcome.ALREADY_CONFIRMED, order.getId());

        try {
            fulfill(paid);
        } catch (RuntimeException e) {
            // Don van la PAID (da nhan tien), ghi chu de admin xu ly tay
            log.error("[payment] Đã nhận tiền nhưng kích hoạt quyền lợi lỗi, đơn {}", order.getId(), e);
            mongo.updateFirst(byId(order.getId()), Update.update("note", "Lỗi kích hoạt quyền lợi: " + e.getMessage()), Order.class);
        }
        return new CallbackResult(CallbackOutcome.CONFIRMED, order.getId());
    }

    private void markFailed(String orderId, String note) {
        mongo.updateFirst(Query.query(Criteria.where("id").is(orderId).and("status").is(Order.Status.PENDING.code())),
                new Update().set("status", Order.Status.FAILED.code()).set("note", note), Order.class);
    }

    /** Kich hoat quyen loi cua don vua thanh toan (chi chay 1 lan moi don) */
    private void fulfill(Order order) {
        String buyerName = users.findOneById(order.getUserId()).map(User::getUsername).orElse("Một người xem");

        switch (order.typeEnum()) {
            case MEMBERSHIP -> {
                MembershipTier tier = memberships.findTier(order.getTierId());
                String tierName = tier == null ? "Hội viên" : tier.getName();
                Date expires = memberships.extend(order.getUserId(), order.getChannelId(), order.getTierId(), order.getMonths());
                String channelName = users.findOneById(order.getChannelId()).map(User::getUsername).orElse("");
                notifications.send(order.getUserId(), "Tham gia hội viên thành công",
                        "Bạn là hội viên " + tierName + " của kênh " + channelName + " đến " + VI_DATE.format(expires.toInstant()));
                notifications.send(order.getChannelId(), "Hội viên mới",
                        buyerName + " đã tham gia gói " + tierName + " (" + order.getMonths() + " tháng, " + money(order.amountValue()) + ")");
            }
            case SUPER_THANKS -> {
                CommentDto comment = comments.postSuperThanks(order.getVideoId(), order.getUserId(), order.getMessage(),
                        order.amountValue(), order.getId());
                mongo.updateFirst(byId(order.getId()), Update.update("comment_id", comment.id()), Order.class);
                realtime.broadcast(order.getVideoId(), new RealtimeEvent.CommentNew(order.getVideoId(), comment));

                String title = videos.findOneById(order.getVideoId()).map(Video::getTitle).orElse("");
                notifications.send(order.getChannelId(), "Super Thanks",
                        buyerName + " đã gửi " + money(order.amountValue()) + " cho video \"" + title + "\"");
                notifications.send(order.getUserId(), "Gửi Super Thanks thành công",
                        "Cảm ơn bạn đã ủng hộ video \"" + title + "\" " + money(order.amountValue()));
            }
        }
    }

    // ================= Xem don =================

    /** Gan ten kenh, ten goi, tieu de video... cho danh sach don (1 query moi loai, thay cho JOIN) */
    private List<OrderDto> toDtos(List<Order> rows) {
        Set<String> userIds = new HashSet<>();
        rows.forEach(o -> {
            userIds.add(o.getUserId());
            userIds.add(o.getChannelId());
        });
        userIds.remove(null);
        Map<String, User> userMap = users.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        Map<String, MembershipTier> tierMap = tiers.findByIdIn(rows.stream().map(Order::getTierId).filter(Objects::nonNull)
                .collect(Collectors.toSet())).stream().collect(Collectors.toMap(MembershipTier::getId, Function.identity(), (a, b) -> a));
        Map<String, Video> videoMap = videos.findByIdIn(rows.stream().map(Order::getVideoId).filter(Objects::nonNull)
                .collect(Collectors.toSet())).stream().collect(Collectors.toMap(Video::getId, Function.identity(), (a, b) -> a));

        return rows.stream().map(o -> {
            User buyer = userMap.get(o.getUserId());
            User channel = userMap.get(o.getChannelId());
            MembershipTier tier = o.getTierId() == null ? null : tierMap.get(o.getTierId());
            Video video = o.getVideoId() == null ? null : videoMap.get(o.getVideoId());
            return new OrderDto(o.getId(), o.getType(), o.getChannelId(), o.getVideoId(), o.getTierId(),
                    o.getMonths() == null ? 0 : o.getMonths(), o.amountValue(), o.getMessage(), o.getProvider(),
                    o.effectiveStatus().code(), o.getNote(), o.getCreatedAt(), o.getPaidAt(), o.getUserId(),
                    buyer == null ? null : buyer.getUsername(), channel == null ? null : channel.getUsername(),
                    channel == null ? null : channel.getAvatar(), tier == null ? null : tier.getName(),
                    video == null ? null : video.getTitle(), video == null ? null : video.getSlug(), o.getProviderTxn(), null);
        }).toList();
    }

    /** Chi tiet 1 don: chi nguoi mua hoac admin xem duoc (nguoi khac nhan 404 nhu khong ton tai) */
    public OrderDto getOrder(AuthUser user, String orderId) {
        Order order = orders.findOneById(orderId)
                .filter(o -> user.id().equals(o.getUserId()) || "admin".equals(user.role()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch"));
        OrderDto dto = toDtos(List.of(order)).get(0);
        if (order.typeEnum() == Order.Type.MEMBERSHIP && order.statusEnum() == Order.Status.PAID) {
            Membership m = memberships.activeMembership(order.getUserId(), order.getChannelId());
            dto = dto.withMembershipExpiresAt(m == null ? null : m.getExpiresAt());
        }
        return dto;
    }

    /** Lich su giao dich cua toi + cac kenh dang la hoi vien */
    public MyOrdersResponse myOrders(String userId, Paging p) {
        Page<Order> page = orders.findByUserId(userId, p.pageable(Sort.by(Sort.Direction.DESC, "createdAt")));
        return new MyOrdersResponse(toDtos(page.getContent()), page.getTotalElements(), p.page(), p.size(),
                memberships.myMemberships(userId));
    }

    /** Filter trang thai: "expired" = pending qua han, "pending" = pending con han */
    private static Criteria statusFilter(Order.Status s) {
        Date cutoff = new Date(System.currentTimeMillis() - Order.EXPIRES_MINUTES * 60_000L);
        return switch (s) {
            case PENDING -> Criteria.where("status").is("pending").and("created_at").gte(cutoff);
            case EXPIRED -> Criteria.where("status").is("pending").and("created_at").lt(cutoff);
            default -> Criteria.where("status").is(s.code());
        };
    }

    // ================= Doanh thu (Studio) =================

    /** Doanh thu cua chu kenh: tong, theo loai, 6 thang gan nhat, giao dich moi nhat */
    public RevenueResponse creatorRevenue(String channelId) {
        List<Order> paid = orders.findByChannelIdAndStatus(channelId, Order.Status.PAID.code(),
                Sort.by(Sort.Direction.DESC, "paidAt"));

        long gross = 0, membershipGross = 0, superThanksGross = 0, superThanksCount = 0;
        Map<YearMonth, long[]> monthly = new TreeMap<>();
        YearMonth now = YearMonth.now(VN);
        for (int i = 5; i >= 0; i--) monthly.put(now.minusMonths(i), new long[2]); // [hoi vien, super thanks]

        for (Order o : paid) {
            long amount = o.amountValue();
            boolean isMembership = o.typeEnum() == Order.Type.MEMBERSHIP;
            gross += amount;
            if (isMembership) membershipGross += amount;
            else {
                superThanksGross += amount;
                superThanksCount++;
            }
            if (o.getPaidAt() != null) {
                long[] bucket = monthly.get(YearMonth.from(o.getPaidAt().toInstant().atZone(VN)));
                if (bucket != null) bucket[isMembership ? 0 : 1] += amount;
            }
        }

        List<RevenueResponse.Month> series = monthly.entrySet().stream().map(e -> new RevenueResponse.Month(
                e.getKey().toString(), e.getValue()[0], e.getValue()[1], e.getValue()[0] + e.getValue()[1])).toList();
        return new RevenueResponse(
                new RevenueResponse.Summary(gross, gross * (100 - PLATFORM_FEE_PERCENT) / 100, membershipGross,
                        superThanksGross, superThanksCount, paid.size(), memberships.listMembers(channelId).size()),
                PLATFORM_FEE_PERCENT, series, toDtos(paid.subList(0, Math.min(20, paid.size()))));
    }

    // ================= Admin =================

    /**
     * Moi giao dich, loc theo trang thai / cong / loai, tim theo ma don, ma giao dich hoac ten nguoi mua / kenh.
     * Kem tong doanh thu va phi nen tang cua cac don da thanh toan.
     */
    public AdminPaymentsResponse adminList(String search, String status, String provider, String type, Paging p) {
        List<Criteria> filters = new ArrayList<>();
        Order.Status s = Order.Status.of(status);
        if (s != null) filters.add(statusFilter(s));
        if (provider != null && !provider.isEmpty()) filters.add(Criteria.where("provider").is(provider));
        if (Order.Type.of(type) != null) filters.add(Criteria.where("type").is(type));
        if (search != null && !search.isBlank()) {
            String q = search.trim();
            List<String> userIds = mongo.find(Query.query(MongoUtils.contains("username", q)), User.class)
                    .stream().map(User::getId).toList();
            filters.add(new Criteria().orOperator(Criteria.where("id").is(q), Criteria.where("provider_txn").is(q),
                    Criteria.where("user_id").in(userIds), Criteria.where("channel_id").in(userIds)));
        }
        Query query = filters.isEmpty() ? new Query() : Query.query(new Criteria().andOperator(filters));
        long total = mongo.count(query, Order.class);
        List<Order> rows = mongo.find(query.with(p.pageable(Sort.by(Sort.Direction.DESC, "createdAt"))), Order.class);

        List<Order> paid = orders.findByStatus(Order.Status.PAID.code());
        long gross = paid.stream().mapToLong(Order::amountValue).sum();
        return new AdminPaymentsResponse(toDtos(rows), total, p.page(), p.size(),
                new AdminPaymentsResponse.Summary(paid.size(), gross, gross * PLATFORM_FEE_PERCENT / 100,
                        mongo.count(Query.query(statusFilter(Order.Status.PENDING)), Order.class),
                        orders.countByStatus(Order.Status.FAILED.code())),
                providers());
    }
}
