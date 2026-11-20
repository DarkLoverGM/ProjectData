package com.yt.backend.service;

import com.yt.backend.common.ApiException;
import com.yt.backend.common.MongoUtils;
import com.yt.backend.dto.membership.*;
import com.yt.backend.entity.Membership;
import com.yt.backend.entity.MembershipTier;
import com.yt.backend.entity.User;
import com.yt.backend.repository.MembershipRepository;
import com.yt.backend.repository.MembershipTierRepository;
import com.yt.backend.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Hoi vien kenh (giong "Tham gia" tren YouTube):
 * - Chu kenh tao toi da MAX_TIERS goi (membership_tiers): ten, gia / thang, quyen loi
 * - Nguoi xem mua goi qua PaymentService, sau khi thanh toan thi extend() cong them thoi han (memberships)
 * - Hoi vien con han: xem video "chi danh cho hoi vien", co huy hieu canh ten khi binh luan
 * Moi nguoi chi co 1 ban ghi memberships cho moi kenh (unique user_id + channel_id), mua them thi cong don han.
 */
@Service
public class MembershipService {

    public static final int MAX_TIERS = 5;
    public static final long PRICE_MIN = 10_000;
    public static final long PRICE_MAX = 2_000_000;
    /** 1 "thang" hoi vien = 30 ngay */
    private static final long MONTH_MS = 30L * 24 * 60 * 60 * 1000;

    private final MembershipTierRepository tiers;
    private final MembershipRepository memberships;
    private final UserRepository users;
    private final MongoTemplate mongo;

    public MembershipService(MembershipTierRepository tiers, MembershipRepository memberships, UserRepository users,
                             MongoTemplate mongo) {
        this.tiers = tiers;
        this.memberships = memberships;
        this.users = users;
        this.mongo = mongo;
    }

    private Map<String, User> usersById(Collection<String> ids) {
        return users.findByIdIn(new HashSet<>(ids)).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }

    private Map<String, MembershipTier> tiersById(Collection<String> ids) {
        return tiers.findByIdIn(new HashSet<>(ids)).stream()
                .collect(Collectors.toMap(MembershipTier::getId, Function.identity(), (a, b) -> a));
    }

    // ================= Goi hoi vien (chu kenh quan ly trong Studio) =================

    /** Goi cua kenh, re -> dat. includeHidden = true: ca goi da an (cho chu kenh) */
    public List<TierDto> listTiers(String channelId, boolean includeHidden) {
        List<MembershipTier> list = includeHidden
                ? tiers.findByChannelIdOrderByPriceAsc(channelId)
                : tiers.findOnSale(channelId, Sort.by("price"));
        return list.stream().map(TierDto::from).toList();
    }

    public MembershipTier findTier(String tierId) {
        return tierId == null ? null : tiers.findOneById(tierId).orElse(null);
    }

    /** Goi cua chinh kenh minh, khong co thi 404 */
    private MembershipTier ownTier(String channelId, String tierId) {
        MembershipTier tier = findTier(tierId);
        if (tier == null || !channelId.equals(tier.getChannelId())) throw ApiException.notFound("Không tìm thấy gói");
        return tier;
    }

    public TierResponse createTier(String channelId, TierRequest req) {
        if (tiers.countOnSale(channelId) >= MAX_TIERS) {
            throw ApiException.badRequest("Mỗi kênh tối đa " + MAX_TIERS + " gói hội viên");
        }
        String name = req.name().trim();
        if (tiers.existsNameOnSale(channelId, name, "")) throw ApiException.conflict("Kênh đã có gói tên này");

        Date now = new Date();
        MembershipTier tier = new MembershipTier();
        tier.setId(MongoUtils.uuid());
        tier.setChannelId(channelId);
        tier.setName(name);
        tier.setPrice(req.price());
        tier.setPerks(req.perks() == null ? "" : req.perks().trim());
        tier.setActive(true);
        tier.setCreatedAt(now);
        tier.setUpdatedAt(now);
        tiers.insert(tier);
        return new TierResponse(true, "Đã tạo gói hội viên", TierDto.from(tier));
    }

    /**
     * Sua goi. Doi gia chi ap dung cho lan mua sau, hoi vien hien tai giu han da tra.
     * active = false: an goi, khong ai mua them duoc nhung hoi vien cu van con han.
     */
    public TierResponse updateTier(String channelId, String tierId, TierUpdateRequest req) {
        MembershipTier tier = ownTier(channelId, tierId);
        if (req.name() != null && tiers.existsNameOnSale(channelId, req.name().trim(), tierId)) {
            throw ApiException.conflict("Kênh đã có gói tên này");
        }
        boolean reactivating = Boolean.TRUE.equals(req.active()) && !tier.isOnSale();
        if (reactivating && tiers.countOnSale(channelId) >= MAX_TIERS) {
            throw ApiException.badRequest("Mỗi kênh tối đa " + MAX_TIERS + " gói hội viên đang bán");
        }

        if (req.name() != null) tier.setName(req.name().trim());
        if (req.price() != null) tier.setPrice(req.price());
        if (req.perks() != null) tier.setPerks(req.perks().trim());
        if (req.active() != null) tier.setActive(req.active());
        tier.setUpdatedAt(new Date());
        tiers.save(tier);
        return new TierResponse(true, "Đã lưu gói", TierDto.from(tier));
    }

    /** Goi chua ai mua thi xoa han, dang co hoi vien thi chi an (khong lam mat quyen loi nguoi da tra tien) */
    public DeleteTierResponse deleteTier(String channelId, String tierId) {
        MembershipTier tier = ownTier(channelId, tierId);
        long members = memberships.countByTierIdAndExpiresAtAfter(tierId, new Date());
        if (members > 0) {
            tier.setActive(false);
            tier.setUpdatedAt(new Date());
            tiers.save(tier);
            return new DeleteTierResponse(true, true,
                    "Gói đang có " + members + " hội viên nên chỉ được ẩn, họ vẫn giữ quyền lợi tới hết hạn");
        }
        tiers.deleteOneById(tierId);
        return new DeleteTierResponse(true, false, "Đã xoá gói");
    }

    // ================= Hoi vien =================

    /** Ban ghi hoi vien con han cua userId o kenh channelId, khong co thi null */
    public Membership activeMembership(String userId, String channelId) {
        if (userId == null || channelId == null) return null;
        return memberships.findOneByUserIdAndChannelIdAndExpiresAtAfter(userId, channelId, new Date()).orElse(null);
    }

    public boolean isMember(String userId, String channelId) {
        return activeMembership(userId, channelId) != null;
    }

    public boolean hasActiveTier(String channelId) {
        return tiers.countOnSale(channelId) > 0;
    }

    /**
     * Cong them `months` thang cho hoi vien sau khi thanh toan. Con han thi cong tiep tu ngay het han cu
     * (mua tiep khong bi mat ngay), het han / chua co thi tinh tu bay gio. Goi moi thay goi cu.
     * @return ngay het han moi
     */
    public Date extend(String userId, String channelId, String tierId, int months) {
        Membership current = memberships.findOneByUserIdAndChannelId(userId, channelId).orElse(null);
        long now = System.currentTimeMillis();
        boolean renewing = current != null && current.isActive();
        long base = renewing ? current.getExpiresAt().getTime() : now;
        Date expires = new Date(base + months * MONTH_MS);

        Update update = new Update()
                .setOnInsert("id", MongoUtils.uuid())
                .set("tier_id", tierId)
                .set("expires_at", expires)
                .set("updated_at", new Date(now));
        // Het han roi mua lai = bat dau lai tu dau
        if (renewing) update.setOnInsert("started_at", new Date(now));
        else update.set("started_at", new Date(now));
        mongo.upsert(Query.query(Criteria.where("user_id").is(userId).and("channel_id").is(channelId)), update, Membership.class);
        return expires;
    }

    /** Thong tin hoi vien cua kenh cho trang xem video / profile: cac goi, so hoi vien, goi cua nguoi xem */
    public ChannelMembershipResponse channelInfo(String channelId, String viewerId) {
        Membership mine = activeMembership(viewerId, channelId);
        ChannelMembershipResponse.MyMembership my = null;
        if (mine != null) {
            MembershipTier tier = findTier(mine.getTierId());
            my = new ChannelMembershipResponse.MyMembership(mine.getTierId(), tier == null ? null : tier.getName(),
                    mine.getStartedAt(), mine.getExpiresAt());
        }
        return new ChannelMembershipResponse(channelId, listTiers(channelId, false),
                memberships.countByChannelIdAndExpiresAtAfter(channelId, new Date()), my, channelId.equals(viewerId));
    }

    /** userId -> ten goi dang dung, chi gom nguoi con han (huy hieu canh ten trong binh luan) */
    public Map<String, String> activeTierNames(String channelId, Collection<String> userIds) {
        Map<String, String> out = new HashMap<>();
        if (channelId == null || userIds == null || userIds.isEmpty()) return out;
        List<Membership> rows = memberships.findByChannelIdAndUserIdInAndExpiresAtAfter(channelId, userIds, new Date());
        Map<String, MembershipTier> tierMap = tiersById(rows.stream().map(Membership::getTierId).toList());
        for (Membership m : rows) {
            MembershipTier tier = tierMap.get(m.getTierId());
            out.put(m.getUserId(), tier == null ? "Hội viên" : tier.getName());
        }
        return out;
    }

    /** Hoi vien con han cua kenh (Studio), han gan nhat len truoc */
    public List<MemberDto> listMembers(String channelId) {
        List<Membership> rows = memberships.findByChannelIdAndExpiresAtAfterOrderByExpiresAtAsc(channelId, new Date());
        Map<String, User> userMap = usersById(rows.stream().map(Membership::getUserId).toList());
        Map<String, MembershipTier> tierMap = tiersById(rows.stream().map(Membership::getTierId).toList());
        return rows.stream().map(m -> {
            User u = userMap.get(m.getUserId());
            MembershipTier t = tierMap.get(m.getTierId());
            return new MemberDto(m.getUserId(), u == null ? null : u.getUsername(), u == null ? null : u.getAvatar(),
                    t == null ? null : t.getName(), m.getStartedAt(), m.getExpiresAt());
        }).toList();
    }

    /** Cac kenh userId dang la hoi vien (trang Giao dich cua toi) */
    public List<MyMembershipDto> myMemberships(String userId) {
        List<Membership> rows = memberships.findByUserIdAndExpiresAtAfterOrderByExpiresAtAsc(userId, new Date());
        Map<String, User> channels = usersById(rows.stream().map(Membership::getChannelId).toList());
        Map<String, MembershipTier> tierMap = tiersById(rows.stream().map(Membership::getTierId).toList());
        return rows.stream().map(m -> {
            User c = channels.get(m.getChannelId());
            MembershipTier t = tierMap.get(m.getTierId());
            return new MyMembershipDto(m.getChannelId(), c == null ? null : c.getUsername(), c == null ? null : c.getAvatar(),
                    m.getTierId(), t == null ? null : t.getName(), t == null || t.getPrice() == null ? 0 : t.getPrice(),
                    m.getStartedAt(), m.getExpiresAt());
        }).toList();
    }

    /** Xoa tai khoan: bo goi cua kenh do va moi tu cach hoi vien lien quan (don hang giu lai de doi soat) */
    public void removeUser(String userId) {
        tiers.deleteByChannelId(userId);
        memberships.deleteByUserIdOrChannelId(userId, userId);
    }
}
