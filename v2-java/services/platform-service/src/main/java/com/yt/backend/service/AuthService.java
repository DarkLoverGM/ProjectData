package com.yt.backend.service;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.auth.BanList;
import com.yt.backend.common.ApiException;
import com.yt.backend.common.MongoUtils;
import com.yt.backend.dto.ErrorResponse;
import com.yt.backend.dto.auth.RegisterRequest;
import com.yt.backend.dto.auth.UserSummary;
import com.yt.backend.entity.EmailVerification;
import com.yt.backend.entity.Library;
import com.yt.backend.entity.User;
import com.yt.backend.repository.EmailVerificationRepository;
import com.yt.backend.repository.LibraryRepository;
import com.yt.backend.repository.UserRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.UUID;

/** Dang nhap, dang ky (kem ma xac minh email), dang nhap Google */
@Service
public class AuthService {

    // Ma xac minh email khi dang ky
    public static final int REGISTER_CODE_EXPIRES_MINUTES = 10;
    private static final int REGISTER_CODE_MAX_ATTEMPTS = 5;
    private static final int REGISTER_CODE_RESEND_SECONDS = 60;

    private final UserRepository users;
    private final EmailVerificationRepository verifications;
    private final LibraryRepository libraries;
    private final MongoTemplate mongo;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, EmailVerificationRepository verifications, LibraryRepository libraries,
                       MongoTemplate mongo, PasswordEncoder encoder) {
        this.users = users;
        this.verifications = verifications;
        this.libraries = libraries;
        this.mongo = mongo;
        this.encoder = encoder;
    }

    public static AuthUser toAuthUser(User user) {
        return new AuthUser(user.getId(), user.getEmail(), user.getUsername(), user.getRole());
    }

    /**
     * Kiem tra email + mat khau.
     * @throws ApiException 401 sai thong tin, 403 ACCOUNT_BANNED neu dung mat khau nhung dang bi khoa
     */
    public User login(String email, String password) {
        User user = users.findOneByEmailIgnoreCase(email).orElse(null);
        // Tai khoan tao bang Google chua dat mat khau -> chi dang nhap duoc qua Google
        if (user == null || !user.hasPassword() || !encoder.matches(password, user.getPassword())) {
            throw new ApiException(401, "Email hoặc mật khẩu không đúng");
        }
        BanList.Ban ban = BanList.activeBan(user);
        if (ban != null) {
            throw new ApiException(403, new ErrorResponse(false, "ACCOUNT_BANNED", BanList.banMessage(ban), null, ban.until()));
        }
        return user;
    }

    /** User con ton tai va khong bi khoa (dung khi refresh token / dang nhap Google), khong thi null */
    public User findActiveUser(String userId) {
        if (userId == null) return null;
        return users.findOneById(userId).orElse(null);
    }

    // ==================== MA XAC MINH EMAIL ====================

    /**
     * Tao ma xac minh email de dang ky, luu hash vao email_verification.
     * Moi email chi co 1 ma hieu luc, phai doi REGISTER_CODE_RESEND_SECONDS moi gui lai duoc.
     * @return ma (de gui mail)
     */
    public String createRegisterCode(String email) {
        if (users.findOneByEmailIgnoreCase(email).isPresent()) throw ApiException.conflict("Email đã tồn tại");

        EmailVerification recent = verifications.findOneByEmail(email).orElse(null);
        if (recent != null && recent.getCreatedAt() != null) {
            long age = Math.floorDiv(System.currentTimeMillis() - recent.getCreatedAt().getTime(), 1000L);
            if (age < REGISTER_CODE_RESEND_SECONDS) {
                long retryAfter = REGISTER_CODE_RESEND_SECONDS - age;
                throw new ApiException(429, new ErrorResponse(false, null,
                        "Vui lòng đợi " + retryAfter + "s rồi gửi lại mã", retryAfter, null));
            }
        }

        String code = String.valueOf(100000 + random.nextInt(900000));
        long now = System.currentTimeMillis();
        // Moi email 1 ma (unique index email): ghi de ma cu bang upsert
        mongo.upsert(Query.query(Criteria.where("email").is(email)), new Update()
                .set("code_hash", encoder.encode(code))
                .set("attempts", 0)
                .set("createAt", new Date(now))
                .set("expiresAt", new Date(now + REGISTER_CODE_EXPIRES_MINUTES * 60_000L)), EmailVerification.class);
        return code;
    }

    /** Huy ma (vd. gui mail loi) de nguoi dung khong phai doi het thoi gian cho gui lai */
    public void discardRegisterCode(String email) {
        if (email != null) verifications.deleteByEmail(email);
    }

    /** @throws ApiException 400 neu ma sai / het han / nhap sai qua nhieu */
    private void checkRegisterCode(String email, String code) {
        EmailVerification record = verifications.findOneByEmail(email).orElse(null);
        if (record == null) throw ApiException.badRequest("Chưa gửi mã xác minh tới email này. Vui lòng bấm \"Gửi mã\".");
        if (record.getExpiresAt() == null || !record.getExpiresAt().after(new Date())) {
            throw ApiException.badRequest("Mã xác minh đã hết hạn. Vui lòng gửi lại mã.");
        }
        int attempts = record.getAttempts() == null ? 0 : record.getAttempts();
        if (attempts >= REGISTER_CODE_MAX_ATTEMPTS) throw ApiException.badRequest("Bạn đã nhập sai quá nhiều lần. Vui lòng gửi lại mã.");

        if (record.getCodeHash() == null || !encoder.matches(code, record.getCodeHash())) {
            mongo.updateFirst(Query.query(Criteria.where("email").is(email)), new Update().inc("attempts", 1), EmailVerification.class);
            throw ApiException.badRequest("Mã xác minh không đúng. Còn " + (REGISTER_CODE_MAX_ATTEMPTS - attempts - 1) + " lần thử.");
        }
    }

    // ==================== DANG KY ====================

    /** Kiem tra trung email/username roi den ma xac minh email, tao user + thu vien mac dinh */
    public UserSummary register(RegisterRequest req, String avatar) {
        String username = req.username().trim();
        if (users.findOneByEmailIgnoreCase(req.email()).isPresent()) throw ApiException.conflict("Email đã tồn tại");
        if (users.existsByUsernameIgnoreCase(username)) throw ApiException.conflict("Username đã tồn tại");

        // Check ma sau cung: trung username thi sua lai la dung tiep duoc ma cu
        checkRegisterCode(req.email(), req.code());

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setEmail(req.email());
        user.setPassword(encoder.encode(req.password()));
        user.setRole(User.ROLE_USER);
        user.setAvatar(avatar);
        user.setCreatedAt(new Date());
        users.insert(user);
        createLibrary(user);

        // Ma da dung xong -> xoa de khong dung lai duoc
        discardRegisterCode(req.email());
        return UserSummary.from(user);
    }

    private void createLibrary(User user) {
        Library library = new Library();
        library.setId(MongoUtils.uuid());
        library.setUserId(user.getId());
        library.setLibraryName("Thư viện của " + user.getUsername());
        libraries.insert(library);
    }

    // ==================== GOOGLE ====================

    /**
     * Dang nhap bang Google: tim theo google_id, roi theo email (gan google_id vao tai khoan cu),
     * khong co thi tao user moi (password null, dat sau bang Quen mat khau).
     * @return user, null neu email Google chua xac minh
     */
    public User loginWithGoogle(String sub, String email, boolean emailVerified, String name, String picture) {
        // Email chua xac minh thi khong duoc gan vao tai khoan co san (chiem tai khoan nguoi khac)
        if (sub == null || sub.isEmpty() || email == null || email.isEmpty() || !emailVerified) return null;

        User user = users.findOneByGoogleId(sub).orElse(null);
        if (user != null) return user;

        user = users.findOneByEmailIgnoreCase(email).orElse(null);
        if (user != null) {
            mongo.updateFirst(Query.query(Criteria.where("id").is(user.getId())), Update.update("google_id", sub), User.class);
            user.setGoogleId(sub);
            return user;
        }

        user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setUsername(uniqueUsername(name != null && !name.isEmpty() ? name : email.split("@")[0]));
        user.setRole(User.ROLE_USER);
        user.setAvatar(picture != null && !picture.isEmpty() ? picture : "/default-avatar.png");
        user.setGoogleId(sub);
        user.setCreatedAt(new Date());
        users.insert(user);
        createLibrary(user);
        return user;
    }

    /** Ten Google co the trung username nguoi khac -> them so ngau nhien phia sau */
    private String uniqueUsername(String base) {
        String trimmed = base.trim();
        String name = trimmed.isEmpty() ? "user" : trimmed.substring(0, Math.min(40, trimmed.length()));
        if (!users.existsByUsernameIgnoreCase(name)) return name;
        for (int i = 0; i < 10; i++) {
            String candidate = name + (1000 + random.nextInt(9000));
            if (!users.existsByUsernameIgnoreCase(candidate)) return candidate;
        }
        return name + UUID.randomUUID().toString().substring(0, 8);
    }
}
