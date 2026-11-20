# Bounded contexts

| Service | Port | Collections sở hữu | API chính |
|---|---:|---|---|
| auth-service | 3002 | users, email_verification, password_reset, push_tokens | login, register, OTP, OAuth, JWT |
| user-service | 3003 | users, follows, blocked_users, notifications, push_tokens | profile, follow, block, report, notification |
| video-service | 3004 | videos, comments, reactions, libraries, subtitles, tags, video_messages | upload/watch, comments, library, Studio media |
| payment-service | 3005 | orders, membership_tiers, memberships | checkout, MoMo, VNPay, membership, revenue |
| admin-service | 3006 | admin_audit, reports, comment_reports, user_reports | moderation, analytics, audit, broadcast |
| platform-service | 3001 | fallback; đang giữ toàn bộ implementation cũ | legacy/API chưa chuyển sang service riêng |

## Quy tắc

- Không service nào truy cập trực tiếp collection của service khác.
- Giao tiếp qua REST hoặc event contract trong `shared/shared-contracts`.
- Giai đoạn migration: route đã chuyển sang service mới; route chưa chuyển đi `platform-fallback`.
