# YT Creator Platform — Microservices & ReactJS (Lab 03)

Dự án nền tảng chia sẻ Video, Super Thanks và Gói hội viên (Membership) theo kiến trúc **Spring Boot Microservices độc lập + RestClient + MongoDB + ReactJS (Vite)**.

## 1. Cấu trúc thư mục độc lập (không phụ thuộc `v2-java`)

| Thư mục / Service | Port | Database (MongoDB) | Chức năng chính |
|---|---:|---|---|
| `user-service/` | `3003` | `YT_USER` | Quản lý người dùng & kênh nhà sáng tạo (`/api/users`) |
| `video-service/` | `3004` | `YT_VIDEO` | Quản lý video, lượt xem, lọc thể loại (`/api/videos`) |
| `payment-service/` | `3005` | **MySQL** (`yt_payment`) | Quản lý gói hội viên (`/api/tiers`), đơn thanh toán (`/api/orders`) và gọi đồng bộ `user-service`, `video-service` qua **RestClient** |
| `frontend/` (ReactJS) | `5173` | — | Giao diện Client (`/`, `/videos`, `/videos/:id`, `/memberships`, `/order-success`) & Admin (`/admin/*`) |
| `database/` | — | — | Script tạo dữ liệu mẫu MongoDB (`seed-mongodb.js`) |

Mỗi Microservice đều có `pom.xml` độc lập kế thừa trực tiếp từ `spring-boot-starter-parent:3.2.3`, có thể mở và chạy riêng lẻ không cần `v2-java`.

## 2. Quy trình nghiệp vụ liên Microservices

- **Đăng ký Gói hội viên (`POST /api/orders`, `type = MEMBERSHIP`)**:
  1. `payment-service` nhận `OrderRequest` (được kiểm tra bằng Jakarta Validation).
  2. Gọi `user-service` qua `UserClient` (`RestClient`) để xác thực người mua & kênh sở hữu gói.
  3. Kiểm tra gói hội viên đang hoạt động, tính `totalAmount = price * months`, lưu vào `YT_PAYMENT` và trả về `201 Created`.
- **Gửi Super Thanks (`POST /api/orders`, `type = SUPER_THANKS`)**:
  1. `payment-service` gọi `user-service` (`UserClient`) xác thực người gửi.
  2. Gọi `video-service` (`VideoClient`) lấy thông tin video & kênh sở hữu.
  3. Lưu đơn ủng hộ và trả về `201 Created`.
- **Xử lý lỗi tập trung (`GlobalExceptionHandler`)**:
  - `ResourceNotFoundException` / `HttpClientErrorException.NotFound` → `404 Not Found`
  - `MethodArgumentNotValidException` / `BadRequestException` → `400 Bad Request`
  - `ResourceAccessException` → `503 Service Unavailable`
  - `RestClientResponseException` → `502 Bad Gateway`

## 3. Hướng dẫn chạy dự án

### Backend (Spring Boot độc lập)
Yêu cầu MongoDB đang chạy ở `mongodb://127.0.0.1:27017` (mỗi service có sẵn `DataSeeder` tự tạo dữ liệu mẫu, hoặc chạy `mongosh database/seed-mongodb.js`).

```bash
# Terminal 1 - User Service (Port 3003)
cd user-service
mvn spring-boot:run

# Terminal 2 - Video Service (Port 3004)
cd video-service
mvn spring-boot:run

# Terminal 3 - Payment Service (Port 3005)
cd payment-service
mvn spring-boot:run
```

### Frontend (ReactJS + Vite)
```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```
Truy cập: `http://localhost:5173` (Client) và `http://localhost:5173/admin` (Admin).
