# Frontend

Code được gom theo tính năng. Một tính năng giữ trang, component riêng, API và hook cạnh nhau; không tạo thêm thư mục con khi chưa cần.

```text
src/
├── main.tsx                 # Khởi tạo React và providers
├── app/                     # Routing, layout chung, 404, CSS toàn ứng dụng
├── features/
│   ├── admin/               # Workspace quản trị và dữ liệu admin mock
│   ├── auth/                # Trang đăng nhập, phiên admin, API và hook auth
│   ├── movies/              # Trang chủ, chi tiết phim, MovieCard, API và hook
│   └── booking/             # Chọn suất/ghế, checkout, vé, lịch sử, API và hook
└── shared/                  # HTTP client, contract types, LoadingState
    └── mocks/               # API giả lập và dữ liệu dùng chung giữa tính năng
```

`features/booking` gom giao diện của hành trình đặt vé, gồm dữ liệu rạp/suất chiếu và checkout mock. Cách tổ chức frontend không thay đổi quyền sở hữu dữ liệu của các backend service.

- Import trực tiếp từ file cần dùng; không thêm barrel `index.ts` để export lại toàn bộ tính năng.
- API và TanStack Query hook thuộc tính năng tương ứng trong `api.ts` và `hooks.ts`.
- Chỉ đưa phần thật sự dùng chung vào `shared/`.
- Danh mục phim đọc API thật. Auth admin chuẩn bị dùng `/auth/login` và `/me`; backend chưa triển khai. Auth khách hàng và hành trình đặt vé vẫn là prototype mock.
- Dữ liệu admin vẫn là mock; xem [hướng dẫn auth-service](../docs/admin-auth-implementation.md) để triển khai đăng nhập thật.

Từ thư mục `frontend`, chạy `npm run dev`, `npm run build` hoặc `npm run lint`. Playwright được giữ trong `e2e/`; không chạy trong đợt refactor này theo yêu cầu của chủ dự án.
