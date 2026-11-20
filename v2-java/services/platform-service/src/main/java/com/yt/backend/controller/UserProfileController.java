package com.yt.backend.controller;

import com.yt.backend.auth.AuthUser;
import com.yt.backend.common.ApiException;
import com.yt.backend.common.ImageUpload;
import com.yt.backend.config.RateLimits;
import com.yt.backend.dto.MessageResponse;
import com.yt.backend.dto.ErrorResponse;
import com.yt.backend.dto.auth.CaptchaRequest;
import com.yt.backend.dto.auth.CaptchaResponse;
import com.yt.backend.dto.user.PublicProfileResponse;
import com.yt.backend.dto.user.UpdateProfileRequest;
import com.yt.backend.dto.user.UpdateProfileResponse;
import com.yt.backend.dto.user.UserInfoResponse;
import com.yt.backend.dto.video.VideoDto;
import com.yt.backend.dto.ValidationPatterns;
import com.yt.backend.entity.User;
import com.yt.backend.service.CaptchaService;
import com.yt.backend.service.FollowService;
import com.yt.backend.service.UserService;
import com.yt.backend.service.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Thong tin / cap nhat profile + captcha (controllers/user/profileController.js, captchaController.js) */
@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    private final UserService userService;
    private final FollowService followService;
    private final VideoService videoService;
    private final CaptchaService captchaService;
    private final ImageUpload imageUpload;
    private final RateLimits rateLimits;

    public UserProfileController(UserService userService, FollowService followService, VideoService videoService,
                                 CaptchaService captchaService, ImageUpload imageUpload, RateLimits rateLimits) {
        this.userService = userService;
        this.followService = followService;
        this.videoService = videoService;
        this.captchaService = captchaService;
        this.imageUpload = imageUpload;
        this.rateLimits = rateLimits;
    }

    /**
     * Cap nhat username/email/avatar/banner va (tuy chon) mat khau, bat buoc qua CAPTCHA.
     * multipart/form-data, anh o field `avatar` / `banner`. Anh chi duoc luu sau khi qua moi buoc kiem tra.
     */
    @PostMapping("/update")
    @PreAuthorize("isAuthenticated()")
    public UpdateProfileResponse update(@AuthenticationPrincipal AuthUser me, @Valid @ModelAttribute UpdateProfileRequest body,
                                        HttpServletRequest req) {
        ImageUpload.Saved images = imageUpload.save(req, "avatar", "banner");
        try {
            if (!captchaService.verifyCaptcha(body.captchaToken(), body.captcha())) {
                throw ApiException.badRequest("Mã CAPTCHA không hợp lệ hoặc đã hết hạn");
            }
            User user = userService.requireUser(me.id());
            userService.checkProfileConflicts(me.id(), body.username(), body.email());

            if (notBlank(body.oldPassword()) && notBlank(body.newPassword())) {
                if (body.newPassword().length() < ValidationPatterns.PASSWORD_MIN) {
                    throw ApiException.badRequest(ValidationPatterns.PASSWORD_MESSAGE);
                }
                userService.changePassword(me.id(), body.oldPassword(), body.newPassword());
            }

            String newAvatar = images.store("avatar");
            String newBanner = images.store("banner");
            userService.updateProfile(me.id(), body.username(), body.email(), newAvatar, newBanner);
            // Doi anh thanh cong moi xoa file cu
            if (newAvatar != null) imageUpload.removeStoredImage(user.getAvatar());
            if (newBanner != null) imageUpload.removeStoredImage(user.getBanner());

            return new UpdateProfileResponse(true, "Cập nhật thành công.",
                    newAvatar != null ? newAvatar : user.getAvatar(), newBanner != null ? newBanner : user.getBanner());
        } catch (RuntimeException e) {
            // Moi nhanh tu choi deu phai xoa file vua nhan, khong thi rac nam lai trong uploads/
            images.removeAll();
            throw e;
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isEmpty();
    }

    @GetMapping("/userinfo")
    @PreAuthorize("isAuthenticated()")
    public UserInfoResponse userinfo(@AuthenticationPrincipal AuthUser me) {
        User user = userService.requireUser(me.id());
        return new UserInfoResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.getAvatar(),
                user.getBanner(), followService.getFollowingIds(me.id()), userService.getBlockedUsers(me.id()));
    }

    /** Du lieu trang profile cong khai */
    @GetMapping("/userdata/{id}")
    public PublicProfileResponse userdata(@PathVariable String id) {
        User user = userService.findUserById(id);
        if (user == null) throw ApiException.notFound("Không tìm thấy người dùng");
        List<VideoDto> videos = videoService.getAllVideosByUser(user.getId());
        return new PublicProfileResponse(user.getId(), user.getUsername(), user.getAvatar(), user.getBanner(),
                videos.size(), videos, userService.countFollowers(user.getId()));
    }

    @GetMapping("/create-captcha")
    public CaptchaResponse createCaptcha() {
        String token = captchaService.createCaptcha();
        return new CaptchaResponse(true, "Tạo captcha thành công", token,
                new CaptchaResponse.Svg(captchaService.createCaptchaSVG(token)));
    }

    @PostMapping("/verify-captcha")
    public MessageResponse verifyCaptcha(@RequestBody CaptchaRequest body) {
        if (!captchaService.verifyCaptcha(body.captchaToken(), body.userInput())) {
            throw ApiException.badRequest("Captcha không hợp lệ");
        }
        return MessageResponse.ok("Captcha hợp lệ");
    }

    /**
     * Captcha khi vuot rate limit: frontend (src/utils/captchaGate.js) goi khi nhan 429 CAPTCHA_REQUIRED;
     * dung thi duoc tinh luot lai tu dau
     */
    @PostMapping("/captcha/solve")
    public MessageResponse solveCaptcha(HttpServletRequest req, @RequestBody CaptchaRequest body) {
        if (!captchaService.verifyCaptcha(body.captchaToken(), body.userInput())) {
            throw new ApiException(400, ErrorResponse.of("CAPTCHA_INVALID", "Captcha không đúng, vui lòng thử lại"));
        }
        rateLimits.resetCaptchaLimits(req);
        return MessageResponse.ok("Captcha đúng");
    }
}
