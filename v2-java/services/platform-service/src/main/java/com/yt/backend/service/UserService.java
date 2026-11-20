package com.yt.backend.service;

import com.yt.backend.common.ApiException;
import com.yt.backend.common.MongoUtils;
import com.yt.backend.dto.MessageResponse;
import com.yt.backend.entity.BlockedUser;
import com.yt.backend.entity.PasswordReset;
import com.yt.backend.entity.User;
import com.yt.backend.entity.UserReport;
import com.yt.backend.repository.BlockedUserRepository;
import com.yt.backend.repository.FollowRepository;
import com.yt.backend.repository.PasswordResetRepository;
import com.yt.backend.repository.UserReportRepository;
import com.yt.backend.repository.UserRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/** Thong tin user, doi mat khau, OTP quen mat khau, chan / bao cao user */
@Service
public class UserService {

    public static final int OTP_EXPIRES_MINUTES = 5;
    private static final int OTP_MAX_ATTEMPTS = 5;

    private final UserRepository users;
    private final FollowRepository follows;
    private final BlockedUserRepository blocked;
    private final PasswordResetRepository resets;
    private final UserReportRepository reports;
    private final MongoTemplate mongo;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();

    public UserService(UserRepository users, FollowRepository follows, BlockedUserRepository blocked,
                       PasswordResetRepository resets, UserReportRepository reports, MongoTemplate mongo,
                       PasswordEncoder encoder) {
        this.users = users;
        this.follows = follows;
        this.blocked = blocked;
        this.resets = resets;
        this.reports = reports;
        this.mongo = mongo;
        this.encoder = encoder;
    }

    /** OTP vua tao (de gui mail) */
    public record Otp(String userId, String code) {}

    private static Query byId(String userId) {
        return Query.query(Criteria.where("id").is(userId));
    }

    // ================================================================
    //  USER LOOKUP
    // ================================================================

    /** Email so khong phan biet hoa/thuong nhu MySQL cu */
    public User findUserByEmail(String email) {
        return email == null || email.isEmpty() ? null : users.findOneByEmailIgnoreCase(email).orElse(null);
    }

    public User findUserById(String userId) {
        return userId == null ? null : users.findOneById(userId).orElse(null);
    }

    public User requireUser(String userId) {
        User user = findUserById(userId);
        if (user == null) throw ApiException.notFound("Người dùng không tồn tại");
        return user;
    }

    public long countFollowers(String userId) {
        return follows.countByFollowingId(userId);
    }

    // ================================================================
    //  PROFILE UPDATE
    // ================================================================

    /**
     * Cap nhat username, email, avatar va/hoac banner (gia tri rong thi giu nguyen).
     * Username / email moi khong duoc trung nguoi khac (so khong phan biet hoa thuong).
     */
    public void updateProfile(String userId, String username, String email, String avatar, String banner) {
        Update update = new Update();
        if (username != null && !username.isBlank()) {
            if (users.existsByUsernameIgnoreCaseAndIdNot(username.trim(), userId)) throw ApiException.conflict("Username đã tồn tại");
            update.set("username", username.trim());
        }
        if (email != null && !email.isBlank()) {
            if (users.existsByEmailIgnoreCaseAndIdNot(email.trim(), userId)) throw ApiException.conflict("Email đã tồn tại");
            update.set("email", email.trim());
        }
        if (avatar != null && !avatar.isEmpty()) update.set("avatar", avatar);
        if (banner != null && !banner.isEmpty()) update.set("banner", banner);
        if (!update.getUpdateObject().isEmpty()) mongo.updateFirst(byId(userId), update, User.class);
    }

    /** Kiem tra trung truoc khi luu anh (de khong upload anh roi moi bao loi) */
    public void checkProfileConflicts(String userId, String username, String email) {
        if (username != null && !username.isBlank() && users.existsByUsernameIgnoreCaseAndIdNot(username.trim(), userId)) {
            throw ApiException.conflict("Username đã tồn tại");
        }
        if (email != null && !email.isBlank() && users.existsByEmailIgnoreCaseAndIdNot(email.trim(), userId)) {
            throw ApiException.conflict("Email đã tồn tại");
        }
    }

    // ================================================================
    //  PASSWORD
    // ================================================================

    /** Doi mat khau (kiem tra mat khau cu truoc). @throws ApiException 403 khi khong doi duoc */
    public void changePassword(String userId, String oldPassword, String newPassword) {
        User user = findUserById(userId);
        if (user == null) throw ApiException.notFound("Người dùng không tồn tại");
        if (!user.hasPassword()) {
            throw ApiException.forbidden("Tài khoản đăng nhập bằng Google chưa có mật khẩu, hãy dùng Quên mật khẩu để đặt");
        }
        if (oldPassword == null || !encoder.matches(oldPassword, user.getPassword())) {
            throw ApiException.forbidden("Mật khẩu cũ không đúng");
        }
        mongo.updateFirst(byId(userId), Update.update("password", encoder.encode(newPassword)), User.class);
    }

    /** Dat lai mat khau sau khi xac minh OTP, xoa OTP de khong dung lai duoc */
    public void resetPassword(String userId, String newPassword) {
        mongo.updateFirst(byId(userId), Update.update("password", encoder.encode(newPassword)), User.class);
        resets.deleteByUserId(userId);
    }

    // ================================================================
    //  OTP
    // ================================================================

    /** Tao OTP, luu hash vao user_resetpass. Moi user chi co 1 OTP hieu luc. Email khong ton tai thi null */
    public Otp createOTP(String email) {
        User user = findUserByEmail(email);
        if (user == null) return null;

        String code = String.valueOf(100000 + random.nextInt(900000));
        long now = System.currentTimeMillis();
        resets.deleteByUserId(user.getId());
        PasswordReset reset = new PasswordReset();
        reset.setId(MongoUtils.uuid());
        reset.setUserId(user.getId());
        reset.setOtpHash(encoder.encode(code));
        reset.setAttempts(0);
        reset.setVerified(0);
        reset.setCreatedAt(new Date(now));
        reset.setExpiresAt(new Date(now + OTP_EXPIRES_MINUTES * 60_000L));
        resets.insert(reset);
        return new Otp(user.getId(), code);
    }

    /** OTP moi nhat cua user theo email */
    private PasswordReset latestOTP(String email) {
        User user = findUserByEmail(email);
        return user == null ? null : resets.findFirstByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null);
    }

    private static boolean expired(PasswordReset r) {
        return r.getExpiresAt() == null || !r.getExpiresAt().after(new Date());
    }

    /** @throws ApiException 400 khi OTP sai / het han */
    public MessageResponse verifyOTP(String email, String otp) {
        PasswordReset record = latestOTP(email);
        if (record == null) throw ApiException.badRequest("Không tìm thấy OTP. Vui lòng yêu cầu mã mới.");
        if (expired(record)) throw ApiException.badRequest("OTP đã hết hạn.");

        int attempts = record.getAttempts() == null ? 0 : record.getAttempts();
        if (attempts >= OTP_MAX_ATTEMPTS) throw ApiException.badRequest("Bạn đã nhập sai quá nhiều lần. Vui lòng yêu cầu mã mới.");

        Query query = Query.query(Criteria.where("id").is(record.getId()));
        if (record.getOtpHash() == null || !encoder.matches(otp, record.getOtpHash())) {
            mongo.updateFirst(query, new Update().inc("attemps", 1), PasswordReset.class);
            throw ApiException.badRequest("OTP không đúng. Còn " + (OTP_MAX_ATTEMPTS - attempts - 1) + " lần thử.");
        }
        mongo.updateFirst(query, Update.update("verified", 1), PasswordReset.class);
        return MessageResponse.ok("OTP hợp lệ. Bạn có thể đổi mật khẩu.");
    }

    /** OTP da xac minh va con han chua (truoc khi cho dat lai mat khau). @return userId */
    public String requireVerifiedOTP(String email) {
        PasswordReset record = latestOTP(email);
        if (record == null || record.getVerified() == null || record.getVerified() == 0) {
            throw ApiException.forbidden("OTP chưa được xác minh.");
        }
        if (expired(record)) throw ApiException.badRequest("Phiên đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu mã mới.");
        return record.getUserId();
    }

    // ================================================================
    //  BLOCKED USERS ("khong de xuat video cua nguoi nay") & REPORT USER
    // ================================================================

    /** Danh sach id user ma `userId` da chan */
    public List<String> getBlockedUsers(String userId) {
        return blocked.findByUserId(userId).stream().map(BlockedUser::getBlockedId).toList();
    }

    /** Chan 1 user. Loi neu tu chan minh hoac nguoi bi chan khong ton tai; chan lai lan nua thi bo qua. */
    public MessageResponse blockUser(String userId, String blockedId) {
        if (Objects.equals(userId, blockedId)) throw ApiException.badRequest("Không thể tự chặn chính mình");
        User target = findUserById(blockedId);
        if (target == null) throw ApiException.notFound("Không tìm thấy người dùng");

        // Thay INSERT IGNORE: upsert theo cap (user_id, blocked_id), da co thi giu nguyen
        MongoUtils.ignoreDuplicate(() -> mongo.upsert(
                Query.query(Criteria.where("user_id").is(userId).and("blocked_id").is(blockedId)),
                new Update().setOnInsert("created_at", new Date()), BlockedUser.class));
        return MessageResponse.ok("Sẽ không đề xuất video của " + target.getUsername() + " nữa");
    }

    /** Bo chan (khong co trong danh sach thi cung coi nhu thanh cong) */
    public MessageResponse unblockUser(String userId, String blockedId) {
        blocked.deleteByUserIdAndBlockedId(userId, blockedId);
        return MessageResponse.ok("Đã đề xuất lại video của người dùng này");
    }

    /** Gui bao cao 1 nguoi dung (luu vao collection reports, admin xu ly sau) */
    public UserReport reportUser(String reporterId, String reportedId, String reason, String detail) {
        if (Objects.equals(reporterId, reportedId)) throw ApiException.badRequest("Không thể tự báo cáo chính mình");
        if (findUserById(reportedId) == null) throw ApiException.notFound("Không tìm thấy người dùng");
        // Chan spam: moi nguoi chi co 1 bao cao dang cho xu ly cho cung 1 user
        if (reports.existsPending(reporterId, reportedId)) {
            throw ApiException.conflict("Bạn đã báo cáo người dùng này, vui lòng chờ xử lý");
        }

        UserReport report = new UserReport();
        report.setId(MongoUtils.uuid());
        report.setReportedUser(reportedId);
        report.setReporter(reporterId);
        report.setReason(reason.length() > 255 ? reason.substring(0, 255) : reason);
        report.setDetail(detail == null ? "" : detail);
        report.setCreatedAt(new Date());
        report.setStatus("pending");
        reports.insert(report);
        return report;
    }
}
