-- Database MySQL cho Payment Service (yt_payment)
CREATE DATABASE IF NOT EXISTS yt_payment CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE yt_payment;

CREATE TABLE IF NOT EXISTS membership_tiers (
    id VARCHAR(64) PRIMARY KEY,
    channel_id VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(15, 2) NOT NULL,
    perks VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    user_name VARCHAR(120),
    type VARCHAR(30) NOT NULL,
    channel_id VARCHAR(64) NOT NULL,
    channel_name VARCHAR(120),
    video_id VARCHAR(64),
    video_title VARCHAR(255),
    tier_id VARCHAR(64),
    tier_name VARCHAR(120),
    months INT,
    unit_price DECIMAL(15, 2),
    total_amount DECIMAL(15, 2) NOT NULL,
    message VARCHAR(300),
    provider VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO membership_tiers (id, channel_id, name, price, perks, active) VALUES
('tier-001', 'creator-1', 'Đồng Hành', 25000.00, 'Huy hiệu hội viên bên cạnh tên, emoji độc quyền khi bình luận.', TRUE),
('tier-002', 'creator-1', 'VIP Gamer 4K', 79000.00, 'Xem trước video 3 ngày, truy cập video 4K Members Only và Discord riêng.', TRUE),
('tier-003', 'creator-2', 'Java Supporter', 49000.00, 'Tải source code dự án mẫu, tài liệu PDF Spring Boot & Microservices.', TRUE),
('tier-004', 'creator-2', 'Architect Pro', 149000.00, 'Toàn bộ source code, review CV/dự án hàng tháng và Q&A livestream riêng.', TRUE),
('tier-005', 'creator-3', 'Chill Member', 19000.00, 'Tải nhạc Lo-Fi chất lượng Lossless không quảng cáo.', TRUE),
('tier-006', 'creator-4', 'Tech Insider', 39000.00, 'Nhận bảng benchmark chi tiết và tư vấn chọn mua laptop 1-1.', TRUE);

INSERT IGNORE INTO orders (id, user_id, user_name, type, channel_id, channel_name, video_id, video_title, tier_id, tier_name, months, unit_price, total_amount, message, provider, status) VALUES
('ord-001', 'user-1', 'Minh Quang', 'MEMBERSHIP', 'creator-2', 'Spring Boot Academy', NULL, NULL, 'tier-003', 'Java Supporter', 3, 49000.00, 147000.00, 'Cảm ơn anh chia sẻ kiến thức Microservices rất dễ hiểu!', 'MOMO', 'PAID'),
('ord-002', 'user-2', 'Ngọc Quyên', 'SUPER_THANKS', 'creator-3', 'Lo-Fi Chill Studio', 'vid-003', 'Nhạc Lo-Fi Hip Hop giúp tập trung lập trình 3 giờ', NULL, NULL, 0, 30000.00, 30000.00, 'Nhạc rất chill, giúp mình code xong bài Lab 03!', 'VNPAY', 'PAID'),
('ord-003', 'user-3', 'Hoàng Nam', 'MEMBERSHIP', 'creator-1', 'Dark Lover GM', NULL, NULL, 'tier-002', 'VIP Gamer 4K', 1, 79000.00, 79000.00, 'Ủng hộ kênh ra thêm nhiều video 4K!', 'MOMO', 'PAID'),
('ord-004', 'user-1', 'Minh Quang', 'SUPER_THANKS', 'creator-4', 'Tech Review VN', 'vid-004', 'Review Laptop Gaming 2026: Hiệu năng thực tế & tản nhiệt', NULL, NULL, 0, 50000.00, 50000.00, 'Bài đánh giá cực kỳ chi tiết!', 'BANK', 'PENDING');
