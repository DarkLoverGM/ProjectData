# server-java

Backend Spring Boot (Java 21) thay cho server Node `server/Servertest.js`.
Giữ nguyên toàn bộ API (đường dẫn, status code, JSON trả về), cookie đăng nhập và
WebSocket, nên frontend React chạy được mà không phải sửa gì.

Được port từ `server/controllers/` + `server/services/` + `server/middleware/` + `server/utils/`
(gồm cả Studio, duyệt video, khoá tài khoản, rate limit, device id, load shedding, Firebase Storage).
`/api/search` (`SearchController.js`) và `/api/tag` (`TagController.js`) nằm trong `DiscoveryController`.

## Chạy trên máy mới (không cần cấu hình)

Cần: **JDK 21**, **MongoDB** đang chạy ở `127.0.0.1:27017`, Node.js (chỉ để build giao diện React).
Không cần cài Maven: `mvnw` (Maven Wrapper) tự tải Maven 3.9.16 ở lần chạy đầu (cần Internet, như khi tải thư viện).

1. Chép `seed-media.zip` (nộp kèm, không có trong git vì nặng ~140MB) vào `server-java/seed/`.
   Không có file này thì dữ liệu vẫn nạp được, chỉ thiếu video / ảnh.
2. Build giao diện (server Java phục vụ thư mục `build/`):
   ```bash
   npm install && npm run build        # ở thư mục gốc
   ```
3. Build và chạy server:
   ```bash
   cd server-java
   ./mvnw clean package                # biên dịch + chạy unit test, tạo target/server-java-1.0.0.jar
   java -jar target/server-java-1.0.0.jar
   ```
   Windows: dùng `mvnw.cmd clean package` thay cho `./mvnw clean package`.
   Nếu báo không tìm thấy Java, hoặc `java -version` không phải 21: đặt `JAVA_HOME` trỏ tới thư mục JDK 21
   và chạy `java` bằng `$JAVA_HOME/bin/java` (Windows: `%JAVA_HOME%\bin\java`).
4. Mở http://localhost:3001 (trang quản trị: http://localhost:3001/admin/).

Lần chạy đầu, DB `YT` còn trống nên server tự nạp dữ liệu mẫu (xem [Dữ liệu mẫu](#dữ-liệu-mẫu-seed)).
Không cần file `.env`: các biến bắt buộc đều có giá trị mặc định để chạy thử.

**Tài khoản thử nghiệm** (mật khẩu chung `123456`):

| Email | Vai trò |
|---|---|
| `admin@test.local` | Quản trị viên, kênh "Dark Lover GM" (có 2 gói hội viên) |
| `user2@test.local` | Người dùng, kênh "admin" (có 1 gói hội viên, đang theo dõi admin) |
| `user1@test.local`, `user3@test.local`, `user4@test.local` | Người dùng |
| `an90@seed.local` ... (63 tài khoản `@seed.local`) | Người dùng có sẵn video, bình luận, lượt thích |

Thanh toán thử: xem [Thanh toán](#thanh-toán-hội-viên-kênh--super-thanks). Đăng ký tài khoản mới cần gửi mã qua Gmail
(`MAIL_SENDER_*`), máy không cấu hình mail thì dùng tài khoản thử nghiệm ở trên.

Build nhanh, bỏ qua test: `./mvnw clean package -DskipTests`.

Phải chạy từ thư mục `server-java/`: server đọc `server-java/.env`, phục vụ `../build`,
`../public`, ghi file upload vào `../server/uploads` và log truy cập vào `../server/data/log.json`.
Trang quản trị (`/admin/`) nằm trong `src/main/resources/admin/`, được đóng gói vào JAR
(không còn phụ thuộc `server/admin` của bản Node). Sửa giao diện admin xong phải build lại.
Chạy từ chỗ khác thì đặt `APP_ROOT_DIR=/duong/dan/toi/hello-react`.

## Cấu hình

Đọc từ `server-java/.env` (riêng của bản Java, không dùng `server/.env` nữa; file đã nằm trong `.gitignore`).
Biến môi trường thật của hệ điều hành được ưu tiên hơn file này. Không có `.env` thì dùng giá trị mặc định bên dưới
(đủ để chạy thử); triển khai thật phải đặt `JWT_SECRET` riêng.

| Biến | Mặc định |
|---|---|
| `PORT` | 3001 |
| `MONGO_URI` | `mongodb://127.0.0.1:27017/${DB_NAME}` (`DB_NAME` mặc định YT) |
| `JWT_SECRET` | chuỗi dev cố định trong `application.properties` (chỉ để chạy thử) |
| `APP_SEED_ENABLED` | true (DB trống thì nạp dữ liệu mẫu, xem bên dưới) |
| `JWT_REFRESH_SECRET` | `${JWT_SECRET}_refresh` |
| `ACCESS_TOKEN_EXPIRES`, `REFRESH_TOKEN_EXPIRES` | 15m, 7d |
| `COOKIE_SECURE` | false |
| `MAIL_SENDER_EMAIL`, `MAIL_SENDER_PASSWORD` | gửi OTP qua Gmail |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI` | đăng nhập Google (`/api/auth/google`) |
| `CLIENT_URL` | http://localhost:3000 (trang chuyển về sau khi đăng nhập Google, link `reviewUrl` của admin) |
| `COOKIE_SECRET` | `${JWT_SECRET}` (ký device id, cookie `device_id`) |
| `MAX_IN_FLIGHT` | 100 (số request `/api` xử lý cùng lúc, quá thì 503 `SERVER_BUSY`) |
| `FIREBASE_SERVICE_ACCOUNT_PATH` (tính từ `server-java/`, hoặc `FIREBASE_PROJECT_ID` / `FIREBASE_CLIENT_EMAIL` / `FIREBASE_PRIVATE_KEY`) | trống = không gửi push notification, không dùng được Firebase Storage |
| `FIREBASE_STORAGE_BUCKET` | trống = lưu file ở `server/uploads` (bucket phải được tạo trước trên Firebase Console) |
| `FFMPEG_PATH` | `ffmpeg` (đổi thumbnail sang WebP: GIF -> WebP động, JPG/PNG -> WebP tĩnh; không có ffmpeg thì giữ nguyên file gốc) |

JWT ký HS256 với cùng secret như bản Node nên token của hai server dùng chung được.

## Cấu trúc

Các lớp theo kiểu Spring Boot thông thường: **Controller → Service → Repository → Entity**, dữ liệu vào/ra là **DTO**.

```
com.yt.backend
├── entity/      @Document: mỗi collection MongoDB 1 class (User, Video, Comment, Order, MembershipTier...).
│                Tên field Java -> tên trường DB qua @Field("user_id"), giữ nguyên để dùng chung DB với bản Node
├── repository/  interface extends MongoRepository: findByUserIdAndStatus(...), @Query, Pageable (giống Spring Data JPA)
├── dto/         record request (kèm Bean Validation: @NotBlank, @Size, @Pattern...) và response (đúng dạng JSON frontend dùng)
├── service/     nghiệp vụ; dùng repository cho truy vấn, MongoTemplate cho update nguyên tử / aggregation
├── controller/  @RestController mỏng: nhận DTO đã @Valid, gọi service, trả DTO
├── security/    Spring Security: SecurityConfig, JwtAuthenticationFilter, entry point 401 / 403
├── auth/        JwtService, AuthCookies, BanList, DeviceIds, AuthUser (principal của request)
├── payment/     PaymentProvider + MomoGateway, VnpayGateway
├── common/      ApiException, GlobalExceptionHandler, FileStorage, ImageUpload, VideoRules, MongoUtils
├── config/      Mongo (tắt _class, tạo index), CORS, file tĩnh, WebSocket, rate limit, load shedding
└── websocket/   Realtime (phòng `video:<id>`, sự kiện RealtimeEvent), ViewSocketHandler
```

**Bảo mật** (Spring Security, `security/SecurityConfig`):
- Không dùng session: `JwtAuthenticationFilter` đọc JWT trong cookie `token` (hoặc header `Authorization: Bearer`)
  và gán `AuthUser` vào SecurityContext. Controller lấy bằng `@AuthenticationPrincipal AuthUser me`.
- Endpoint cần đăng nhập: `@PreAuthorize("isAuthenticated()")`. Admin: `/api/admin/**` chỉ role `ADMIN`
  (cấu hình URL) và `@PreAuthorize("hasRole('ADMIN')")` trên `AdminController`.
- Lỗi xác thực giữ mã cũ để frontend tự refresh token: 401 `NO_TOKEN` / `TOKEN_EXPIRED` / `TOKEN_INVALID`,
  403 `ACCOUNT_BANNED` / `FORBIDDEN`.
- Upload: chỉ nhận video MP4/WEBM/OGG/MOV/MKV và ảnh JPG/PNG/WEBP/GIF, tên file do server đặt theo MIME
  (không giữ đuôi file người dùng gửi -> không upload được `.html` / `.svg` để chạy script).
- `/api/log` chỉ admin; thông báo chỉ đọc / sửa được của chính mình; quên mật khẩu trả cùng 1 thông báo
  dù email có tồn tại hay không; đổi email / username kiểm tra trùng.

**Validation**: DTO request khai báo ràng buộc, controller nhận bằng `@Valid @RequestBody` / `@Valid @ModelAttribute`
(form multipart). Sai thì `GlobalExceptionHandler` trả 400 `{ success: false, message }` với thông báo tiếng Việt.
Lỗi nghiệp vụ trong service: `throw ApiException.notFound("...")`.

WebSocket: client gửi `{ type: 'join' | 'leave', room }` để vào/ra phòng `video:<id>`,
`{ type: 'view', id }` để tăng lượt xem. Server phát trong phòng: `video:stats`, `comment:new`,
`comment:reaction`, `comment:deleted`.

Hai kênh WebSocket cần đăng nhập (cookie `token`, Origin phải là server / frontend trong danh sách CORS):
- `/ws/admin` (chỉ admin, `AdminRealtime`): trang quản trị nhận `video:new`, `video:media`, `video:message`,
  `report:comment`, `report:user`, `user:new`, `admin:action`.
- `/ws/user` (`UserRealtime`, frontend `src/utils/userRealtime.js`): Studio nhận `studio:video` khi admin trả lời /
  chặn / hiện lại video của mình.
Access token hết hạn thì server đóng kết nối mã 4401 (client refresh token rồi nối lại); bị khoá / bị hạ quyền admin: 4403.

File upload (video, thumbnail, avatar, ảnh bìa) ghi tạm vào `server/uploads/`, rồi `FileStorage` giữ nguyên
ở đó (DB lưu `/video/123.mp4`) hoặc đẩy lên Firebase Storage nếu có cấu hình (DB lưu URL Firebase), giống bản Node.
Firebase được gọi qua REST (service account + JWT, `common/FirebaseCredentials`), không thêm thư viện.

Push notification (`service/PushService`, giống `utils/push.js`): trình duyệt đăng ký FCM token qua
`POST /api/user/push-token` (xoá bằng `DELETE`), lưu ở collection `push_tokens`. Mỗi lần tạo thông báo
(video mới cho follower, Studio, admin broadcast) server gửi thêm push qua FCM HTTP v1, chạy nền.

Rate limit (đếm trong RAM, chỉ đúng khi chạy 1 process) giống `middleware/rateLimit.js`: cùng mức giới hạn,
cùng body `{ code, error }` (429 `CAPTCHA_REQUIRED` / `TOO_MANY_REQUESTS`), header `RateLimit-*` kiểu draft-7.
IP client: chỉ tin `X-Forwarded-For` khi request đến từ loopback (nginx cùng máy), như `trust proxy 'loopback'`.

## Thanh toán (hội viên kênh + Super Thanks)

- **Hội viên kênh**: chủ kênh tạo tối đa 5 gói (Studio → Kiếm tiền), người xem mua 1 / 3 / 6 / 12 tháng
  (6 tháng giảm 5%, 12 tháng giảm 10%). Hội viên được xem video "chỉ dành cho hội viên" và có huy hiệu khi bình luận.
  Mua thêm thì cộng nối vào hạn cũ. Gói đang có hội viên thì chỉ ẩn được, không xoá.
- **Super Thanks**: tặng 10.000đ – 5.000.000đ cho 1 video, bình luận kèm lời nhắn được tô nổi bật.
- Nền tảng giữ 30% (`PaymentService.PLATFORM_FEE_PERCENT`), Studio hiện doanh thu tổng / thực nhận / theo tháng.

Code: `payment/` (`PaymentProvider` + `MomoGateway`, `VnpayGateway`, `Order`), `service/MembershipService`,
`service/PaymentService`, `controller/PaymentController`, `controller/MembershipController`.
Thêm cổng mới = thêm 1 class `@Component implements PaymentProvider`, không sửa service.

Luồng: `POST /api/payments/checkout` tạo đơn `pending` → frontend chuyển sang `payUrl` của cổng →
cổng chuyển về `/api/payments/{momo|vnpay}/return` (và gọi IPN) → server kiểm tra chữ ký HMAC, số tiền,
đổi `pending → paid` bằng 1 lệnh có điều kiện (gọi lặp không kích hoạt 2 lần) → về trang `/payment/result`.

| Biến | Mặc định |
|---|---|
| `SERVER_URL` | `http://localhost:${PORT}` (địa chỉ cổng thanh toán chuyển người dùng về) |
| `MOMO_PARTNER_CODE`, `MOMO_ACCESS_KEY`, `MOMO_SECRET_KEY`, `MOMO_ENDPOINT` | bộ key **test công khai** của MoMo, dùng được ngay |
| `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_URL` | key **sandbox** của nhóm, dùng được ngay. Đăng ký sandbox riêng miễn phí: https://sandbox.vnpayment.vn/devreg/ |

Thanh toán thử (không mất tiền thật):
- MoMo sandbox: chọn thẻ ATM nội địa, thẻ `9704000000000018`, tên `NGUYEN VAN A`, ngày phát hành `03/07`, OTP `OTP`
  (thẻ lỗi để thử: `9704000000000026` bị khoá, `9704000000000034` không đủ tiền, `9704000000000042` vượt hạn mức).
  Nguồn: https://developers.momo.vn/v3/docs/payment/onboarding/test-instructions (trang có thể trả 403 khi truy cập từ ngoài VN / qua VPN)
- VNPay sandbox: ngân hàng NCB, thẻ `9704198526191432198`, tên `NGUYEN VAN A`, ngày phát hành `07/15`, OTP `123456`

IPN chỉ tới được khi server có địa chỉ public (chạy local thì trang return đã tự cập nhật đơn).

Test: `./mvnw test` (kiểm tra chữ ký MoMo / VNPay với giá trị tính độc lập, tính giá gói, hết hạn đơn).

## Database (MongoDB, Spring Data)

Dùng **chung MongoDB với bản Node** (cùng collection, cùng tên field), nên chạy server nào cũng thấy cùng dữ liệu.
Ghi chú chi tiết về các collection và chỗ khác MySQL: xem [`server/MONGODB.md`](../server/MONGODB.md).

- Kết nối: `spring.data.mongodb.uri` = biến `MONGO_URI` (mặc định `mongodb://127.0.0.1:27017/YT`).
  Dùng `127.0.0.1`, không dùng `localhost`.
- Khoá của mỗi bản ghi là field `id` (chuỗi UUID, dùng chung với bản Node). `_id` (ObjectId) map vào `mongoId`
  trong `entity/BaseDocument`; vì vậy repository tìm theo UUID bằng `findOneById(...)`, không dùng `findById(...)`
  có sẵn (cái đó tìm theo `_id`).
- `config/MongoConfig` tắt field `_class` Spring Data tự thêm; `config/MongoIndexConfig` tạo index (unique: 1 người
  chỉ like / follow / lưu 1 lần...) khi server khởi động.
- MongoDB không có khoá ngoại: xoá user / video thì service tự xoá dữ liệu liên quan (`AdminService.deleteUser`).
- Thư mục `sql/` chỉ còn để tham khảo, server không dùng nữa.

### Dữ liệu mẫu (seed)

`server-java/seed/` chứa bản chụp DB đã ẩn danh, nạp tự động bằng Java (`seed/DataSeeder`), không cần mongosh hay Node:

- `<collection>.jsonl`: mỗi dòng 1 document dạng MongoDB Extended JSON (giữ kiểu ObjectId, Date). Có trong git.
- `seed-media.zip`: video, thumbnail, avatar, ảnh bìa mà DB tham chiếu. **Không** có trong git (~140MB), nộp kèm riêng.

Khi khởi động (trước khi tạo index):
- Collection `users` trống → insert toàn bộ file `.jsonl`. DB đã có dữ liệu thì bỏ qua, không ghi đè.
- Có `seed-media.zip` → giải nén các file còn thiếu vào `server/uploads/` (file đã có thì giữ nguyên).

Nạp lại từ đầu: xoá DB (`mongosh YT --eval 'db.dropDatabase()'` hoặc xoá trong MongoDB Compass) rồi khởi động lại server.

**Cập nhật dữ liệu mẫu** từ DB đang chạy (chạy độc lập, server không cần tắt):

```bash
cd server-java
./mvnw package -DskipTests  # nếu vừa sửa code
java -Dloader.main=com.yt.backend.seed.SeedExport -cp target/server-java-1.0.0.jar \
     org.springframework.boot.loader.launch.PropertiesLauncher    # [mongoUri] [thư mục gốc repo]
```

`SeedExport` ẩn danh hoá trước khi ghi file: email thật → `admin@test.local` / `userN@test.local`, mọi mật khẩu
→ `123456`, bỏ `google_id` và avatar Google, không xuất `push_tokens`, `email_verification`, `user_resetpass`.
File media DB trỏ tới mà không còn trên đĩa sẽ được in ra (`thieu file media: ...`).

Script Node cũ (`npm run db:seed`, `npm run db:migrate` để chép từ MySQL) vẫn còn nhưng không cần cho bản Java.

## Khác với bản Node

- `/api/admin/system` trả phiên bản Java ở trường `node`; `memory` là heap của JVM.
- Còn giữ 3 đường dẫn cũ bản Node đã bỏ (`ModerationController`): `/api/blockUser/:id`,
  `/api/report/user/:id`, `/api/blockComment/:commentId/:videoId`. Frontend hiện tại không gọi tới.
- Upload gửi file ở field lạ (không phải `video` / `thumb`) thì bị bỏ qua, bản Node (multer) trả 400.
- Dữ liệu gửi lên sai (thiếu field, mật khẩu dưới 6 ký tự, email sai định dạng...) trả 400 kèm thông báo,
  bản Node có chỗ lỗi 500.
- Log truy cập (`/api/track`, `/api/log`, `/api/admin/logs`) vẫn dùng `server/data/log.json` (giữ 5000 dòng mới nhất).
