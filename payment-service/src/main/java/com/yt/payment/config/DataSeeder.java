package com.yt.payment.config;

import com.yt.payment.entity.MembershipTier;
import com.yt.payment.entity.Order;
import com.yt.payment.repository.MembershipTierRepository;
import com.yt.payment.repository.OrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedPaymentData(MembershipTierRepository tierRepository, OrderRepository orderRepository) {
        return args -> {
            Instant now = Instant.now();

            if (tierRepository.count() == 0) {
                tierRepository.saveAll(List.of(
                        new MembershipTier("tier-001", "creator-1", "Đồng Hành", new BigDecimal("25000"),
                                "Huy hiệu hội viên bên cạnh tên, emoji độc quyền khi bình luận.", true, now),
                        new MembershipTier("tier-002", "creator-1", "VIP Gamer 4K", new BigDecimal("79000"),
                                "Xem trước video 3 ngày, truy cập video 4K Members Only và Discord riêng.", true, now),
                        new MembershipTier("tier-003", "creator-2", "Java Supporter", new BigDecimal("49000"),
                                "Tải source code dự án mẫu, tài liệu PDF Spring Boot & Microservices.", true, now),
                        new MembershipTier("tier-004", "creator-2", "Architect Pro", new BigDecimal("149000"),
                                "Toàn bộ source code, review CV/dự án hàng tháng và Q&A livestream riêng.", true, now),
                        new MembershipTier("tier-005", "creator-3", "Chill Member", new BigDecimal("19000"),
                                "Tải nhạc Lo-Fi chất lượng Lossless không quảng cáo.", true, now),
                        new MembershipTier("tier-006", "creator-4", "Tech Insider", new BigDecimal("39000"),
                                "Nhận bảng benchmark chi tiết và tư vấn chọn mua laptop 1-1.", true, now)
                ));
            }

            if (orderRepository.count() == 0) {
                orderRepository.saveAll(List.of(
                        new Order("ord-001", "user-1", "Minh Quang", "MEMBERSHIP", "creator-2", "Spring Boot Academy",
                                null, null, "tier-003", "Java Supporter", 3,
                                new BigDecimal("49000"), new BigDecimal("147000"),
                                "Cảm ơn anh chia sẻ kiến thức Microservices rất dễ hiểu!", "MOMO", "PAID",
                                now.minus(3, ChronoUnit.DAYS)),
                        new Order("ord-002", "user-2", "Ngọc Quyên", "SUPER_THANKS", "creator-3", "Lo-Fi Chill Studio",
                                "vid-003", "Nhạc Lo-Fi Hip Hop giúp tập trung lập trình 3 giờ", null, null, 0,
                                new BigDecimal("30000"), new BigDecimal("30000"),
                                "Nhạc rất chill, giúp mình code xong bài Lab 03!", "VNPAY", "PAID",
                                now.minus(2, ChronoUnit.DAYS)),
                        new Order("ord-003", "user-3", "Hoàng Nam", "MEMBERSHIP", "creator-1", "Dark Lover GM",
                                null, null, "tier-002", "VIP Gamer 4K", 1,
                                new BigDecimal("79000"), new BigDecimal("79000"),
                                "Ủng hộ kênh ra thêm nhiều video 4K!", "MOMO", "PAID",
                                now.minus(1, ChronoUnit.DAYS)),
                        new Order("ord-004", "user-1", "Minh Quang", "SUPER_THANKS", "creator-4", "Tech Review VN",
                                "vid-004", "Review Laptop Gaming 2026: Hiệu năng thực tế & tản nhiệt", null, null, 0,
                                new BigDecimal("50000"), new BigDecimal("50000"),
                                "Bài đánh giá cực kỳ chi tiết!", "BANK", "PENDING",
                                now.minus(6, ChronoUnit.HOURS))
                ));
            }
        };
    }
}
