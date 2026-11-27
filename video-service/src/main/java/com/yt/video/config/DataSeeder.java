package com.yt.video.config;

import com.yt.video.entity.Video;
import com.yt.video.repository.VideoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedVideos(VideoRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            Instant now = Instant.now();
            repository.saveAll(List.of(
                    new Video("vid-001", "Spring Boot 3 & Microservices thực chiến từ A-Z",
                            "Hướng dẫn xây dựng kiến trúc Microservices với RestClient, DTO, Validation và ReactJS.",
                            "https://picsum.photos/seed/springboot3/640/360",
                            "/video/spring-microservices.mp4", "creator-2", "Education", 15420, false, "PUBLIC",
                            now.minus(10, ChronoUnit.DAYS)),
                    new Video("vid-002", "Đánh giá Neverness to Everness - Siêu phẩm RPG 2026",
                            "Trải nghiệm sớm thế giới mở đồ họa Unreal Engine 5 và hệ thống chiến đấu tốc độ cao.",
                            "https://picsum.photos/seed/nte-game/640/360",
                            "/video/nte-preview.mp4", "creator-1", "Gaming", 42800, false, "PUBLIC",
                            now.minus(8, ChronoUnit.DAYS)),
                    new Video("vid-003", "Nhạc Lo-Fi Hip Hop giúp tập trung lập trình 3 giờ",
                            "Playlist nhạc Lo-Fi nhẹ nhàng không lời giúp tăng hiệu suất học tập và làm việc.",
                            "https://picsum.photos/seed/lofi-code/640/360",
                            "/video/lofi-coding.mp4", "creator-3", "Music", 89500, false, "PUBLIC",
                            now.minus(7, ChronoUnit.DAYS)),
                    new Video("vid-004", "Review Laptop Gaming 2026: Hiệu năng thực tế & tản nhiệt",
                            "Đo hiệu năng thực tế khi chạy Docker, IntelliJ IDEA và render video 4K.",
                            "https://picsum.photos/seed/laptop2026/640/360",
                            "/video/laptop-review.mp4", "creator-4", "Technology", 23100, false, "PUBLIC",
                            now.minus(5, ChronoUnit.DAYS)),
                    new Video("vid-005", "Phrolova Crimson Bloom 4K Cinematic Showcase",
                            "Video chất lượng 4K 60fps dành riêng cho hội viên ủng hộ kênh Dark Lover GM.",
                            "https://picsum.photos/seed/crimson4k/640/360",
                            "/video/crimson-bloom.mp4", "creator-1", "Gaming", 12900, true, "PUBLIC",
                            now.minus(4, ChronoUnit.DAYS)),
                    new Video("vid-006", "Xử lý Exception & Validation chuẩn RESTful API trong Spring",
                            "Sử dụng @RestControllerAdvice, Java Record DTO và Jakarta Validation hiệu quả.",
                            "https://picsum.photos/seed/rest-api/640/360",
                            "/video/spring-validation.mp4", "creator-2", "Education", 9840, false, "PUBLIC",
                            now.minus(3, ChronoUnit.DAYS)),
                    new Video("vid-007", "Top 5 bàn phím cơ Custom dưới 2 triệu cho lập trình viên",
                            "So sánh switch tactile, linear, độ bền keycap và cảm giác gõ thực tế.",
                            "https://picsum.photos/seed/mechkey/640/360",
                            "/video/keyboard-top5.mp4", "creator-4", "Technology", 31200, false, "PUBLIC",
                            now.minus(2, ChronoUnit.DAYS)),
                    new Video("vid-008", "Acoustic Rain & Piano - Thư giãn buổi tối",
                            "Âm thanh tiếng mưa rơi kết hợp piano nhẹ nhàng cho giấc ngủ sâu.",
                            "https://picsum.photos/seed/rain-piano/640/360",
                            "/video/rain-piano.mp4", "creator-3", "Music", 54700, false, "PUBLIC",
                            now.minus(1, ChronoUnit.DAYS))
            ));
        };
    }
}
