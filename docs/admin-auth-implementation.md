# Các bước triển khai auth-service cho đăng nhập admin

## 1. Phạm vi và trạng thái hiện tại

Frontend đã có `/admin/login`, chặn truy cập các trang `/admin/*` trước khi đăng nhập và kiểm tra quyền `ADMIN` từ `/api/v1/me`. Phim, rạp, lịch chiếu, đặt vé và thanh toán trong admin vẫn dùng `adminService` mock; không cần viết các API quản trị đó trong giai đoạn này. Đăng nhập khách hàng `/login` vẫn là prototype riêng, không cấp quyền admin.

Backend **chưa có login hoặc `/me`**. `services/auth-service` đã có entity/repository cho `users`, `roles`, `user_roles`, `refresh_sessions`, schema tham chiếu, cấu hình datasource, trace filter và lỗi chuẩn. Security của auth-service và gateway hiện `denyAll` với business API. Route gateway cho auth và `/me` đã tồn tại. File OpenAPI hiện mới là baseline, chưa định nghĩa DTO đăng nhập.

Tài liệu này là kế hoạch triển khai, không phải bằng chứng backend đã hoạt động. Không có tài khoản/mật khẩu admin mặc định để đăng nhập thật ngay lúc này. Mock phản hồi xác thực chỉ nằm trong Playwright.

Đọc trước: [AGENTS.md](../AGENTS.md), [API contracts](api-contracts.md), [service boundaries](service-boundaries.md), [database design](database-design.md), ADR [009](ADR/ADR-009-database-ownership-per-microservice.md), [012](ADR/ADR-012-simple-service-package-structure.md), [013](ADR/ADR-013-manual-database-schema-management.md).

## 2. Chốt hợp đồng tối thiểu tương thích frontend

Frontend hiện dùng bearer token, giữ trong bộ nhớ React, không lưu token/mật khẩu vào localStorage hoặc sessionStorage và không gửi cookie. Tải lại/đóng trang hoặc hết hạn token yêu cầu đăng nhập lại. Chưa có refresh tự động, remember-me, đăng ký admin hay reset password. Đây là phạm vi tích hợp ban đầu; trước khi triển khai cơ chế ký/thu hồi token thật, ghi quyết định bảo mật vào ADR và cập nhật API contracts/OpenAPI cùng kiểm thử hợp đồng.

### POST `/api/v1/auth/login`

Request:

```json
{ "email": "admin@example.test", "password": "<mật khẩu do người dùng nhập>" }
```

Response `200` dự kiến:

```json
{ "accessToken": "<JWT được ký bởi auth-service>", "tokenType": "Bearer", "expiresIn": 900 }
```

`expiresIn` là số nguyên giây tính từ khi phát hành; frontend hiện chấp nhận 1–86400 giây. Đề xuất access token 15 phút; backend là nơi quyết định thời hạn. Không trả password hash, signing key hoặc dữ liệu nhạy cảm. Trả `Cache-Control: no-store` cho các response xác thực.

### GET `/api/v1/me`

Header: `Authorization: Bearer <accessToken>`.

Response `200` dự kiến:

```json
{ "id": "<UUID của users.id>", "email": "admin@example.test", "roles": ["ADMIN"] }
```

Frontend gọi `/me` sau login và chỉ mở admin nếu `roles` chứa chính xác `ADMIN`, không phải `ROLE_ADMIN`. Nếu sau này JWT được chuyển thành Spring authorities, ánh xạ `ADMIN` → `ROLE_ADMIN` ở backend để dùng `hasRole("ADMIN")`; không thay đổi JSON ngoài ý muốn. `/me` đọc trạng thái ACTIVE và quyền hiện tại từ DB auth, không lấy quyền từ request body.

Login chung có thể xác thực cả CUSTOMER; người không có ADMIN bị frontend từ chối vào workspace. Mọi API admin thật trong tương lai phải tự kiểm tra quyền ở service sở hữu, kể cả khi gọi trực tiếp bỏ qua gateway. Route guard frontend chỉ điều khiển giao diện, không bảo vệ dữ liệu server.

Lỗi theo `ApiError` hiện có: `{code,message,traceId,timestamp,details}`. Quy ước đề xuất:

| HTTP | code | Hành vi |
|---|---|---|
| 400 | VALIDATION_ERROR | Email/body không hợp lệ |
| 401 | INVALID_CREDENTIALS | Sai mật khẩu, email không tồn tại hoặc tài khoản DISABLED; cùng thông báo an toàn |
| 401 | TOKEN_INVALID | Token hết hạn, sai chữ ký/issuer/audience hoặc không hợp lệ |
| 403 | FORBIDDEN | Đã xác thực nhưng thiếu quyền với API quản trị |
| 429 | RATE_LIMITED | Quá số lần thử; trả Retry-After |
| 503 | DEPENDENCY_UNAVAILABLE | DB hoặc hạ tầng xác thực không sẵn sàng; không cấp token |

Frontend không tự retry login, giới hạn chờ mỗi request 10 giây, khóa form khi đang gửi và hiện traceId để hỗ trợ khi lỗi có mã này.

## 3. Khôi phục kiểm thử backend trước khi viết logic

Thêm `spring-boot-starter-test` cho auth-service và gateway; thêm các dependency Testcontainers PostgreSQL/JUnit/Spring Boot phù hợp BOM hiện có cho auth-service. Parent đang đặt `maven.test.skip=true`; chạy với `-Dmaven.test.skip=false` sau khi bổ sung dependency. Không coi `mvn test` hiện tại là đã chạy test.

Test auth dùng PostgreSQL container riêng, khởi tạo bằng schema tham chiếu của auth-service và dữ liệu tổng hợp; không kết nối DB quản lý hoặc dùng tài khoản thật. Gateway dùng kiểm thử WebFlux tương ứng. Không thêm H2 để thay thế bằng chứng PostgreSQL.

## 4. Chuẩn bị dữ liệu admin trong DB do auth-service sở hữu

Kiểm tra `services/auth-service/src/main/resources/db/schema.sql` và DB auth đã được provision: `users.email_normalized` unique; `users.status` là ACTIVE/DISABLED; FK `user_roles` chỉ tới bảng trong DB auth. Giai đoạn login tối thiểu có thể dùng schema hiện có, không cần tạo lại bảng hay triển khai refresh_sessions.

Tạo role `ADMIN` và gán bằng `user_roles` cho một user ACTIVE thông qua quy trình bootstrap chỉ chạy thủ công ở local/dev. Dùng PasswordEncoder để tạo `password_hash`, không dùng plaintext hoặc SHA-256 trực tiếp cho mật khẩu. Bootstrap cần idempotent theo email_normalized/role code, từ chối ghi đè tài khoản đã có ngoài ý muốn và chạy trong transaction local. Mật khẩu bootstrap lấy từ biến môi trường/secret, không ghi vào SQL, Git hay log; không tự tạo admin mỗi lần startup. Không mở public endpoint cho phép khách chọn role ADMIN.

Không replay toàn bộ `schema.sql` trên DB đã có dữ liệu. Nếu cần sửa schema, viết kế hoạch SQL incremental được review, đồng bộ entity và snapshot; giữ Hibernate `validate`, SQL init `never`. Chỉ thực hiện thay đổi DB ngoài local/dev khi có xác nhận rõ ràng. Không dùng Supabase Auth thay cho Spring auth-service trong phạm vi này và không cho frontend truy cập DB auth trực tiếp.

## 5. Hoàn thiện entity và repository hiện có

Entity hiện là skeleton với field package-private; bổ sung accessor cần thiết để service đọc id, email, hash, status mà không trả entity trực tiếp qua HTTP. Không dùng generated `toString` chứa hash.

Thêm truy vấn `UserRepository.findByEmailNormalized(...)`; truy vấn role qua UserRoleRepository/RoleRepository hoặc projection service-owned, chỉ join bảng auth. Chuẩn hóa email thống nhất với CHECK `lower(btrim(email))`; test khoảng trắng và chữ hoa, không chuẩn hóa/trim mật khẩu. Tránh lazy-loading sau khi transaction đóng vì `open-in-view=false`.

## 6. Viết DTO và AuthService

Tạo class khi thực sự sử dụng:

```text
com.cinema.auth
├── controller/AuthController.java
├── controller/MeController.java
├── service/AuthService.java
├── service/TokenService.java
├── dto/LoginRequest.java
├── dto/LoginResponse.java
└── dto/MeResponse.java
```

DTO login dùng validation email, NotBlank và giới hạn kích thước hợp lý. Frontend giới hạn email 254 ký tự, password 128 ký tự; backend phải enforce độc lập. Cấu hình PasswordEncoder adaptive của Spring Security; nếu chọn BCrypt, xử lý rõ giới hạn 72 byte UTF-8 bằng validation đồng nhất khi tạo và kiểm tra mật khẩu, không âm thầm cắt chuỗi. `DelegatingPasswordEncoder` cần hash có prefix đúng định dạng; kiểm tra dữ liệu đang lưu trước khi áp dụng.

AuthService nhận request → chuẩn hóa email → lấy user → kiểm tra mật khẩu bằng `matches` → kiểm tra ACTIVE → lấy quyền → TokenService ký token → trả DTO. Với user không tồn tại, thực hiện so khớp dummy hash để giảm khác biệt timing; trả lỗi chung, không tiết lộ email có tồn tại hay tài khoản bị khóa. Controller chỉ validate/call service, không chứa logic mật khẩu hoặc transaction. Dùng transaction read-only ngắn để lấy dữ liệu auth; không gọi service nghiệp vụ khác hay Kafka.

MeController lấy subject từ principal đã được Spring Security xác thực. Service đọc user ACTIVE và roles hiện tại theo subject; không nhận userId từ client để giả mạo danh tính. Tuyệt đối không trả password_hash/token_hash hoặc entity trực tiếp.

## 7. Cấu hình JWT và Security tại auth-service

Dùng hỗ trợ Resource Server/Jose của Spring Security để kiểm tra token; bổ sung dependency theo BOM của repository sau khi kiểm tra parent POM. Dùng JwtEncoder/JwtDecoder thay vì tự viết parser/chữ ký. Quyết định ký bất đối xứng và vòng đời khóa phải nằm trong ADR: auth-service giữ private key qua secret/environment; gateway và service nhận chỉ public key hoặc JWKS theo hợp đồng cụ thể. Không cấu hình issuer-uri tới một endpoint discovery chưa tồn tại; có thể cấu hình decoder với public key đã provision.

Token cần sub=user UUID, iss, aud, iat, exp, jti và roles tối thiểu. Kiểm tra chữ ký, thuật toán cho phép, issuer, audience, exp/nbf và clock skew có giới hạn. Không nhét email/mật khẩu/hash không cần thiết vào token. Không cho client quyết định role hay thời hạn.

Chỉ `POST /api/v1/auth/login` được public trong đợt này; `GET /api/v1/me` yêu cầu bearer hợp lệ. Giữ health/OpenAPI public theo cấu hình hiện có, các business route chưa triển khai vẫn denyAll. Bỏ rejecting UserDetailsService skeleton khi triển khai provider thật; không vô tình bật user/password mặc định, formLogin hoặc HTTP Basic. AuthenticationEntryPoint/AccessDeniedHandler phải trả JSON lỗi chuẩn có traceId.

Frontend hiện `credentials: omit`; chưa dùng cookie nên không triển khai cookie/refresh trong đợt này. Nếu chuyển sang cookie sau này phải review CSRF, CORS, Secure/HttpOnly/SameSite và cập nhật frontend, không giữ `.csrf(disable)` theo quán tính.

## 8. Cấu hình gateway và giới hạn request

Route `/api/v1/auth/**,/api/v1/me` đã trỏ tới auth-service 8081; không tạo route/service mới. Cho phép đúng POST login, yêu cầu bearer cho GET me; những API còn lại giữ chính sách hiện có. Gateway là WebFlux nên dùng ReactiveJwtDecoder và SecurityWebFilterChain, không sao chép HttpSecurity của servlet. Dùng cùng issuer/audience/public key và ánh xạ role có kiểm thử; auth-service vẫn tự xác thực khi được gọi trực tiếp.

Giới hạn login theo IP và tài khoản chuẩn hóa bằng cơ chế rate limit dùng chung giữa các instance, áp dụng ở ingress và bảo vệ auth-service khi gọi trực tiếp. Không dùng bộ đếm RAM làm bảo vệ duy nhất; xác định hành vi khi Redis lỗi, không vô tình bỏ giới hạn. Giới hạn body, cấu hình timeout và CORS allowlist cụ thể nếu browser khác origin; không bật wildcard tùy tiện. Dùng HTTPS khi triển khai.

TraceId được propagate qua gateway và auth filter. Chỉ log mã lỗi và identifier an toàn; loại Authorization header, password, request body login và raw token khỏi log/trace.

## 9. Kết nối và chạy local

Các lệnh dưới đây khớp scripts/POM hiện tại. Maven đã cài trên máy; repository không có Maven wrapper.

```powershell
# Root repository: sau khi khôi phục dependency test
mvn -pl services/auth-service,gateway -am test -Dmaven.test.skip=false
mvn -pl services/auth-service,gateway -am package -Dmaven.test.skip=false

# Chạy mỗi service trong terminal riêng, sau khi DB auth sẵn sàng
mvn -pl services/auth-service spring-boot:run
mvn -pl gateway spring-boot:run

# Terminal frontend
cd frontend
npm run dev
```

Cấp `AUTH_DB_URL`, `AUTH_DB_USER`, `AUTH_DB_PASSWORD` cho process backend và các biến khóa JWT do bước 7 định nghĩa. Maven không tự đọc `.env`; nạp biến môi trường bằng công cụ local của bạn. Không commit giá trị secret. Hạ tầng local có thể khởi động bằng `docker compose up -d`, nhưng không thay thế bước kiểm tra/provision schema.

Vite đã proxy `/api` tới `http://localhost:8080`; có thể đặt `API_PROXY_TARGET` cho target local khác. Frontend dùng `VITE_API_BASE_URL` mặc định `/api/v1`. Giá trị VITE là public bundle, không chứa secret. Khi deploy production phải cấu hình reverse proxy hoặc base URL/CORS phù hợp; Vite dev proxy không tồn tại trong bundle build.

Mở `/admin/login` → nhập user ADMIN đã provision → frontend POST login → GET me bearer → quay lại trang admin đã yêu cầu. Chỉ hai API auth này cần thật; các API/data quản trị vẫn mock. Không dùng login mock của khách hàng để vào admin.

## 10. Kiểm thử và tiêu chí hoàn thành

- Auth integration: đúng/sai mật khẩu, email không tồn tại, DISABLED, email chuẩn hóa, thiếu field/body sai; lỗi không tiết lộ sự tồn tại user. Kiểm tra password hash và không lộ secret trong response/log.
- JWT/service security: chữ ký bị sửa, thuật toán không hợp lệ, thiếu/sai issuer/audience, hết hạn, chưa có hiệu lực, thiếu token; `/me` đọc đúng user và roles, user DISABLED bị từ chối. Test trực tiếp auth-service và qua gateway.
- Rate limit: trả 429, Retry-After, reset theo window và chính sách dependency failure. OpenAPI/DTO/error-shape phải khớp frontend.
- Frontend: kiểm tra quay lại trang ban đầu, role CUSTOMER bị từ chối, sai mật khẩu/retry, loading/chống gửi trùng, hết hạn/logout/reload và mobile. Bổ sung smoke test tích hợp local sau khi backend hoàn thành, không coi API mock là bằng chứng bảo mật backend. Test admin mock hiện có đã được điều chỉnh để đi qua login bằng phản hồi API giả lập; chưa chạy Playwright theo yêu cầu của chủ dự án trong đợt làm giao diện này.
- Chạy `npm run build`, `npm run lint` từ frontend. Áp dụng [code-review skill](../skills/code-review/SKILL.md) trước khi hoàn tất. Playwright có thể chạy sau khi cần kiểm chứng luồng trình duyệt.

Đăng xuất hiện tại chỉ xóa token/profile khỏi bộ nhớ frontend. Nó **không thu hồi JWT đã phát hành**; bản sao token còn hợp lệ đến exp. Nếu cần logout server ngay lập tức, phải thiết kế/persist cơ chế thu hồi và kiểm tra nó tại các service, cập nhật `/auth/logout`, frontend và ADR trước khi tuyên bố đã có revocation. Refresh/rotation qua refresh_sessions là bước mở rộng riêng, chưa cần cho login tối thiểu. Các endpoint refresh/logout trong baseline không có nghĩa đã được triển khai.

Không cần Kafka, Saga hoặc Outbox cho login đọc dữ liệu và cấp token hiện tại. Nếu thêm business event về tài khoản sau này, tuân thủ Outbox/idempotency của repository. Không thay đổi booking/payment để triển khai auth.

## Nguồn tham khảo chính thức

- [Spring Security servlet JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html): cấu hình kiểm tra JWT tại auth-service.
- [Spring Security reactive JWT Resource Server](https://docs.spring.io/spring-security/reference/reactive/oauth2/resource-server/jwt.html): cấu hình gateway WebFlux.
- [Spring Security password storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html): PasswordEncoder và adaptive hashing.
