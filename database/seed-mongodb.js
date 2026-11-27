// Seed data cho MongoDB (YT_USER, YT_VIDEO, YT_PAYMENT)
// Chạy bằng: mongosh database/seed-mongodb.js

const userDb = db.getSiblingDB("YT_USER");
userDb.users.deleteMany({});
userDb.users.insertMany([
  { _id: "creator-1", username: "darklovergm", email: "darklover@ytplatform.vn", channelName: "Dark Lover GM", avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=DarkLover", bio: "Kênh review game, esports và hướng dẫn build PC chuẩn chiến.", role: "CREATOR", createdAt: new Date() },
  { _id: "creator-2", username: "springmaster", email: "spring@ytplatform.vn", channelName: "Spring Boot Academy", avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=SpringMaster", bio: "Chia sẻ kiến thức Java, Spring Boot, Microservices và Docker thực chiến.", role: "CREATOR", createdAt: new Date() },
  { _id: "creator-3", username: "lofi_chill_vn", email: "lofi@ytplatform.vn", channelName: "Lo-Fi Chill Studio", avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=LofiChill", bio: "Nhạc Lo-Fi học bài, lập trình và thư giãn 24/7.", role: "CREATOR", createdAt: new Date() },
  { _id: "creator-4", username: "techreview_vn", email: "tech@ytplatform.vn", channelName: "Tech Review VN", avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=TechReview", bio: "Đánh giá laptop, tai nghe, bàn phím cơ và thiết bị công nghệ.", role: "CREATOR", createdAt: new Date() },
  { _id: "user-1", username: "minhquang", email: "minhquang@gmail.com", channelName: "Minh Quang", avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=MinhQuang", bio: "Khán giả yêu thích công nghệ và lập trình.", role: "USER", createdAt: new Date() },
  { _id: "user-2", username: "ngocquyen", email: "ngocquyen@gmail.com", channelName: "Ngọc Quyên", avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=NgocQuyen", bio: "Sinh viên CNTT, đam mê thiết kế web và nhạc Lo-Fi.", role: "USER", createdAt: new Date() },
  { _id: "user-3", username: "hoangnam", email: "hoangnam@gmail.com", channelName: "Hoàng Nam", avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=HoangNam", bio: "Fan cứng Dark Lover GM và các tựa game nhập vai.", role: "USER", createdAt: new Date() },
  { _id: "admin-1", username: "sysadmin", email: "admin@ytplatform.vn", channelName: "Quản trị hệ thống", avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Admin", bio: "Tài khoản quản trị viên nền tảng video.", role: "ADMIN", createdAt: new Date() }
]);

const videoDb = db.getSiblingDB("YT_VIDEO");
videoDb.videos.deleteMany({});
videoDb.videos.insertMany([
  { _id: "vid-001", title: "Spring Boot 3 & Microservices thực chiến từ A-Z", description: "Hướng dẫn xây dựng kiến trúc Microservices với RestClient, DTO, Validation và ReactJS.", thumbnail: "https://picsum.photos/seed/springboot3/640/360", videoUrl: "/video/spring-microservices.mp4", channelId: "creator-2", category: "Education", views: 15420, membersOnly: false, status: "PUBLIC", uploadAt: new Date() },
  { _id: "vid-002", title: "Đánh giá Neverness to Everness - Siêu phẩm RPG 2026", description: "Trải nghiệm sớm thế giới mở đồ họa Unreal Engine 5 và hệ thống chiến đấu tốc độ cao.", thumbnail: "https://picsum.photos/seed/nte-game/640/360", videoUrl: "/video/nte-preview.mp4", channelId: "creator-1", category: "Gaming", views: 42800, membersOnly: false, status: "PUBLIC", uploadAt: new Date() },
  { _id: "vid-003", title: "Nhạc Lo-Fi Hip Hop giúp tập trung lập trình 3 giờ", description: "Playlist nhạc Lo-Fi nhẹ nhàng không lời giúp tăng hiệu suất học tập và làm việc.", thumbnail: "https://picsum.photos/seed/lofi-code/640/360", videoUrl: "/video/lofi-coding.mp4", channelId: "creator-3", category: "Music", views: 89500, membersOnly: false, status: "PUBLIC", uploadAt: new Date() },
  { _id: "vid-004", title: "Review Laptop Gaming 2026: Hiệu năng thực tế & tản nhiệt", description: "Đo hiệu năng thực tế khi chạy Docker, IntelliJ IDEA và render video 4K.", thumbnail: "https://picsum.photos/seed/laptop2026/640/360", videoUrl: "/video/laptop-review.mp4", channelId: "creator-4", category: "Technology", views: 23100, membersOnly: false, status: "PUBLIC", uploadAt: new Date() },
  { _id: "vid-005", title: "Phrolova Crimson Bloom 4K Cinematic Showcase", description: "Video chất lượng 4K 60fps dành riêng cho hội viên ủng hộ kênh Dark Lover GM.", thumbnail: "https://picsum.photos/seed/crimson4k/640/360", videoUrl: "/video/crimson-bloom.mp4", channelId: "creator-1", category: "Gaming", views: 12900, membersOnly: true, status: "PUBLIC", uploadAt: new Date() },
  { _id: "vid-006", title: "Xử lý Exception & Validation chuẩn RESTful API trong Spring", description: "Sử dụng @RestControllerAdvice, Java Record DTO và Jakarta Validation hiệu quả.", thumbnail: "https://picsum.photos/seed/rest-api/640/360", videoUrl: "/video/spring-validation.mp4", channelId: "creator-2", category: "Education", views: 9840, membersOnly: false, status: "PUBLIC", uploadAt: new Date() },
  { _id: "vid-007", title: "Top 5 bàn phím cơ Custom dưới 2 triệu cho lập trình viên", description: "So sánh switch tactile, linear, độ bền keycap và cảm giác gõ thực tế.", thumbnail: "https://picsum.photos/seed/mechkey/640/360", videoUrl: "/video/keyboard-top5.mp4", channelId: "creator-4", category: "Technology", views: 31200, membersOnly: false, status: "PUBLIC", uploadAt: new Date() },
  { _id: "vid-008", title: "Acoustic Rain & Piano - Thư giãn buổi tối", description: "Âm thanh tiếng mưa rơi kết hợp piano nhẹ nhàng cho giấc ngủ sâu.", thumbnail: "https://picsum.photos/seed/rain-piano/640/360", videoUrl: "/video/rain-piano.mp4", channelId: "creator-3", category: "Music", views: 54700, membersOnly: false, status: "PUBLIC", uploadAt: new Date() }
]);

const paymentDb = db.getSiblingDB("YT_PAYMENT");
paymentDb.membership_tiers.deleteMany({});
paymentDb.membership_tiers.insertMany([
  { _id: "tier-001", channelId: "creator-1", name: "Đồng Hành", price: NumberDecimal("25000"), perks: "Huy hiệu hội viên bên cạnh tên, emoji độc quyền khi bình luận.", active: true, createdAt: new Date() },
  { _id: "tier-002", channelId: "creator-1", name: "VIP Gamer 4K", price: NumberDecimal("79000"), perks: "Xem trước video 3 ngày, truy cập video 4K Members Only và Discord riêng.", active: true, createdAt: new Date() },
  { _id: "tier-003", channelId: "creator-2", name: "Java Supporter", price: NumberDecimal("49000"), perks: "Tải source code dự án mẫu, tài liệu PDF Spring Boot & Microservices.", active: true, createdAt: new Date() },
  { _id: "tier-004", channelId: "creator-2", name: "Architect Pro", price: NumberDecimal("149000"), perks: "Toàn bộ source code, review CV/dự án hàng tháng và Q&A livestream riêng.", active: true, createdAt: new Date() },
  { _id: "tier-005", channelId: "creator-3", name: "Chill Member", price: NumberDecimal("19000"), perks: "Tải nhạc Lo-Fi chất lượng Lossless không quảng cáo.", active: true, createdAt: new Date() },
  { _id: "tier-006", channelId: "creator-4", name: "Tech Insider", price: NumberDecimal("39000"), perks: "Nhận bảng benchmark chi tiết và tư vấn chọn mua laptop 1-1.", active: true, createdAt: new Date() }
]);

print("Seeded YT_USER, YT_VIDEO, YT_PAYMENT successfully!");
