package com.yt.user.config;

import com.yt.user.entity.User;
import com.yt.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedUsers(UserRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            Instant now = Instant.now();
            repository.saveAll(List.of(
                    new User("creator-1", "darklovergm", "darklover@ytplatform.vn", "Dark Lover GM",
                            "https://api.dicebear.com/7.x/avataaars/svg?seed=DarkLover",
                            "Kênh review game, esports và hướng dẫn build PC chuẩn chiến.", "CREATOR", now),
                    new User("creator-2", "springmaster", "spring@ytplatform.vn", "Spring Boot Academy",
                            "https://api.dicebear.com/7.x/avataaars/svg?seed=SpringMaster",
                            "Chia sẻ kiến thức Java, Spring Boot, Microservices và Docker thực chiến.", "CREATOR", now),
                    new User("creator-3", "lofi_chill_vn", "lofi@ytplatform.vn", "Lo-Fi Chill Studio",
                            "https://api.dicebear.com/7.x/avataaars/svg?seed=LofiChill",
                            "Nhạc Lo-Fi học bài, lập trình và thư giãn 24/7.", "CREATOR", now),
                    new User("creator-4", "techreview_vn", "tech@ytplatform.vn", "Tech Review VN",
                            "https://api.dicebear.com/7.x/avataaars/svg?seed=TechReview",
                            "Đánh giá laptop, tai nghe, bàn phím cơ và thiết bị công nghệ.", "CREATOR", now),
                    new User("user-1", "minhquang", "minhquang@gmail.com", "Minh Quang",
                            "https://api.dicebear.com/7.x/avataaars/svg?seed=MinhQuang",
                            "Khán giả yêu thích công nghệ và lập trình.", "USER", now),
                    new User("user-2", "ngocquyen", "ngocquyen@gmail.com", "Ngọc Quyên",
                            "https://api.dicebear.com/7.x/avataaars/svg?seed=NgocQuyen",
                            "Sinh viên CNTT, đam mê thiết kế web và nhạc Lo-Fi.", "USER", now),
                    new User("user-3", "hoangnam", "hoangnam@gmail.com", "Hoàng Nam",
                            "https://api.dicebear.com/7.x/avataaars/svg?seed=HoangNam",
                            "Fan cứng Dark Lover GM và các tựa game nhập vai.", "USER", now),
                    new User("admin-1", "sysadmin", "admin@ytplatform.vn", "Quản trị hệ thống",
                            "https://api.dicebear.com/7.x/avataaars/svg?seed=Admin",
                            "Tài khoản quản trị viên nền tảng video.", "ADMIN", now)
            ));
        };
    }
}
