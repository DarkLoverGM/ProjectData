package com.yt.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/** Gui email OTP dat lai mat khau / xac minh email dang ky (giong server/utils/mailer.js). */
@Service
public class MailService {

    private static final String APP_NAME = "MyWeb.com";
    private static final int OTP_EXPIRES_MINUTES = UserService.OTP_EXPIRES_MINUTES;

    /** Noi dung theo muc dich: dat lai mat khau / xac minh email khi dang ky */
    private record Purpose(String title, String intro, String resendIntro, String ignore) {}

    private static final Purpose RESET = new Purpose(
            "Xác minh đặt lại mật khẩu",
            "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Hãy dùng mã dưới đây để tiếp tục.",
            "Bạn vừa yêu cầu <b>gửi lại</b> mã xác minh để đặt lại mật khẩu. Mã cũ sẽ không còn hiệu lực.",
            "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này — mật khẩu của bạn vẫn an toàn.");

    private static final Purpose REGISTER = new Purpose(
            "Xác minh email đăng ký",
            "Bạn đang đăng ký tài khoản bằng email này. Hãy nhập mã dưới đây vào form đăng ký để xác minh email.",
            "Bạn vừa yêu cầu <b>gửi lại</b> mã xác minh email đăng ký. Mã cũ sẽ không còn hiệu lực.",
            "Nếu bạn không đăng ký tài khoản, hãy bỏ qua email này — sẽ không có tài khoản nào được tạo.");
    private static final DateTimeFormatter VI_TIME = DateTimeFormatter.ofPattern("HH:mm:ss d/M/yyyy");

    private final JavaMailSender sender;
    private final String from;

    public MailService(JavaMailSender sender, @Value("${spring.mail.username:}") String from) {
        this.sender = sender;
        this.from = from;
    }

    public void sendOTPEmail(String email, String otp) throws MessagingException {
        sendOTPEmail(email, otp, "Mã OTP đặt lại mật khẩu", false);
    }

    public void sendOTPEmail(String email, String otp, String subject, boolean resend) throws MessagingException {
        send(email, otp, subject, RESET, resend, OTP_EXPIRES_MINUTES);
    }

    public void sendRegisterCodeEmail(String email, String code) throws MessagingException {
        send(email, code, "Mã xác minh email đăng ký", REGISTER, false, AuthService.REGISTER_CODE_EXPIRES_MINUTES);
    }

    private void send(String email, String otp, String subject, Purpose purpose, boolean resend, int expiresMinutes)
            throws MessagingException {
        String time = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).format(VI_TIME);

        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        try {
            helper.setFrom(from, APP_NAME);
        } catch (UnsupportedEncodingException e) {
            helper.setFrom(from);
        }
        helper.setTo(email);
        helper.setSubject("[" + APP_NAME + "] " + subject);
        helper.setText(buildText(otp, time, purpose, expiresMinutes), buildHtml(otp, time, purpose, resend, expiresMinutes));
        sender.send(message);
    }

    private static String buildText(String otp, String time, Purpose purpose, int expiresMinutes) {
        return String.join("\n",
                APP_NAME + " - " + purpose.title(),
                "",
                "Mã OTP của bạn: " + otp,
                "Mã có hiệu lực trong " + expiresMinutes + " phút.",
                "",
                "Không chia sẻ mã này với bất kỳ ai. Nếu bạn không yêu cầu, hãy bỏ qua email này.",
                "Yêu cầu lúc " + time + ".");
    }

    private static String buildHtml(String otp, String time, Purpose purpose, boolean resend, int expiresMinutes) {
        String intro = resend ? purpose.resendIntro() : purpose.intro();

        StringBuilder digitCells = new StringBuilder();
        for (char d : otp.toCharArray()) {
            digitCells.append("""
                    <td style="padding:0 4px;">
                        <div style="width:44px;height:54px;line-height:54px;text-align:center;font-size:26px;font-weight:700;
                                    font-family:'Courier New',monospace;color:#111827;background:#f3f4f6;
                                    border:1px solid #e5e7eb;border-radius:8px;">%s</div>
                    </td>""".formatted(d));
        }

        return """
                <!doctype html>
                <html lang="vi">
                <body style="margin:0;padding:0;background:#f4f5f7;font-family:Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
                  <span style="display:none;max-height:0;overflow:hidden;">Mã OTP của bạn là {otp} — hết hạn sau {min} phút.</span>
                  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f4f5f7;padding:32px 12px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
                             style="max-width:520px;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.06);">
                        <tr>
                          <td style="background:#e11d48;padding:22px 28px;color:#ffffff;font-size:20px;font-weight:700;">
                            {app}
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:28px;color:#374151;font-size:15px;line-height:1.6;">
                            <h2 style="margin:0 0 12px;font-size:20px;color:#111827;">{title}</h2>
                            <p style="margin:0 0 20px;">{intro}</p>

                            <table role="presentation" cellpadding="0" cellspacing="0" align="center" style="margin:0 auto 20px;">
                              <tr>{digits}</tr>
                            </table>

                            <p style="margin:0 0 20px;text-align:center;font-size:14px;color:#6b7280;">
                              Mã có hiệu lực trong <b style="color:#e11d48;">{min} phút</b>
                            </p>

                            <div style="background:#fff7ed;border-left:4px solid #f97316;padding:12px 16px;border-radius:6px;font-size:13px;color:#7c2d12;">
                              <b>Lưu ý bảo mật:</b> Không chia sẻ mã này với bất kỳ ai, kể cả người tự xưng là nhân viên {app}.
                              {ignore}
                            </div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:16px 28px;background:#f9fafb;color:#9ca3af;font-size:12px;line-height:1.5;border-top:1px solid #f0f0f0;">
                            Yêu cầu lúc {time}.<br>
                            Đây là email tự động, vui lòng không trả lời.<br>
                            © {year} {app}
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>"""
                .replace("{otp}", otp)
                .replace("{min}", String.valueOf(expiresMinutes))
                .replace("{title}", purpose.title())
                .replace("{ignore}", purpose.ignore())
                .replace("{app}", APP_NAME)
                .replace("{intro}", intro)
                .replace("{digits}", digitCells)
                .replace("{time}", time)
                .replace("{year}", String.valueOf(Year.now().getValue()));
    }
}
