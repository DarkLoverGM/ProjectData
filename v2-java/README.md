# v2-java — Microservices migration scaffold

Bản `v2-java` được tạo từ `server-java` theo kiến trúc đa-module/Maven.

## Cấu trúc

- `gateway/`: Spring Cloud API Gateway, cổng chung `8080`.
- `services/platform-service/`: service monolith tạm thời, là bản sao đầy đủ của `server-java` để giữ API và dữ liệu cũ trong giai đoạn chuyển đổi.
- `shared/shared-contracts/`: nơi đặt DTO/event contract dùng chung khi tách service tiếp theo.
- `docker/`: cấu hình triển khai container.
- `docs/`: tài liệu kiến trúc và lộ trình bóc tách.

## Chạy

```powershell
cd d:\WebpageTester\v2-java
.\mvnw.cmd clean package

cd services\platform-service
.\..\..\mvnw.cmd spring-boot:run

cd ..\..\gateway
.\..\mvnw.cmd spring-boot:run
```

Gateway mặc định chạy `8080`, platform service giữ cổng `3001`.

## Lộ trình tách thành microservices thật

1. Tách `auth-service`: đăng nhập, JWT, refresh token, OAuth.
2. Tách `user-service`: hồ sơ, follow, block, notification.
3. Tách `video-service`: video, watch, library, reaction, subtitle.
4. Tách `payment-service`: order, membership, MoMo, VNPay.
5. Tách `admin-service`: moderation, audit, reports, analytics.
6. Mỗi service dùng database/logical collection riêng và giao tiếp qua REST/event.
