# Kiến trúc mục tiêu

```mermaid
flowchart LR
    Client[React / Mobile] --> Gateway[API Gateway :8080]
    Gateway --> Platform[Platform Service :3001]
    Platform --> Mongo[(MongoDB)]

    Gateway -. lộ trình .-> Auth[Auth Service]
    Gateway -. lộ trình .-> User[User Service]
    Gateway -. lộ trình .-> Video[Video Service]
    Gateway -. lộ trình .-> Payment[Payment Service]
    Gateway -. lộ trình .-> Admin[Admin Service]
```

## Nguyên tắc chuyển đổi

- Strangler pattern: giữ platform-service hoạt động đồng thời với các service mới.
- API Gateway là điểm vào duy nhất ở môi trường v2.
- Không sao chép trực tiếp database; mỗi bounded context sở hữu schema collection riêng.
- Giai đoạn đầu dùng REST đồng bộ; sau đó chuyển event bất đồng bộ cho notification và realtime.
- JWT phải được xác thực hoặc hợp lệ qua gateway; nội bộ service truyền identity qua header đã tin cậy.
