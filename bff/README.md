# TIGERNET BFF (Backend for Frontend)

Lớp trung gian giữa Flutter Web và backend Spring Boot.

```
Flutter Web (5173)  ──►  BFF (8081)  ──►  Backend (8080)  ──►  MySQL
```

## Vì sao có BFF

- **Gộp dữ liệu cho từng màn hình.** Ví dụ `/bff/dashboard` gọi song song nhiều API backend (tồn kho, phiếu nhập, phiếu xuất, cảnh báo) rồi trả một JSON đúng như màn Tổng quan cần.
- **Phân trang, tìm kiếm, sắp xếp thống nhất.** Backend hiện trả danh sách đầy đủ. BFF lọc theo `keyword`, sắp xếp theo `sort=field,asc|desc`, phân trang `page` (từ 1) và `size`, rồi trả `PageResponse`.
- **Tính trạng thái hiển thị.** Ví dụ trạng thái tồn kho (Hết hàng / Tồn thấp / Sắp hết / Bình thường) và trạng thái cho thuê (Đang thuê / Quá hạn / Đã trả).
- **Không giữ phiên.** BFF chỉ chuyển tiếp header `Authorization: Bearer <access token>` sang backend. Mọi kiểm tra quyền vẫn nằm ở backend.

## Các endpoint

| Endpoint | Mô tả |
| --- | --- |
| `POST /bff/auth/login`, `POST /bff/auth/refresh` | Đăng nhập, làm mới token (chuyển tiếp `/auth/login`, `/auth/refresh`) |
| `GET /bff/session` | Người dùng, vai trò, danh sách kho được xem, số cảnh báo chưa đọc |
| `GET /bff/dashboard?warehouseId=` | KPI, mục cần chú ý, giao dịch gần đây |
| `GET /bff/products`, `/bff/import-receipts`, `/bff/export-receipts`, `/bff/rental-receipts`, `/bff/suppliers`, `/bff/customers`, `/bff/alerts`, `/bff/warehouses`, `/bff/users`, `/bff/stock-transfers`, `/bff/stocktakes`, `/bff/sales-returns` | Danh sách có phân trang |
| `GET /bff/reports/stock`, `/bff/reports/movements`, `/bff/reports/receivables` | Báo cáo |
| `/bff/api/**` | Proxy thẳng tới backend (bỏ tiền tố `/bff/api`), dùng cho thêm/sửa/xóa |

Lỗi từ backend được trả nguyên mã HTTP và body `ApiResponse`. Khi backend không chạy, BFF trả `503` với `code = 9503`.

## Chạy cục bộ

1. Backend (cổng 8080), cần MySQL và các biến môi trường như `DB_PASSWORD`, `JWT_SIGNER_KEY`:

   ```bash
   cd backend && ./mvnw spring-boot:run
   ```

2. BFF (cổng 8081):

   ```bash
   cd bff && ./mvnw spring-boot:run
   ```

   | Biến môi trường | Mặc định |
   | --- | --- |
   | `BACKEND_BASE_URL` | `http://localhost:8080` |
   | `BFF_PORT` | `8081` |
   | `BFF_ALLOWED_ORIGINS` | `http://localhost:*,http://127.0.0.1:*` |

   Khi triển khai, đặt `BFF_ALLOWED_ORIGINS` thành domain thật của Flutter Web.

3. Flutter Web:

   ```bash
   cd frontend
   flutter pub get
   flutter run -d chrome --dart-define=BFF_BASE_URL=http://localhost:8081
   ```

   Build bản tĩnh:

   ```bash
   flutter build web --dart-define=BFF_BASE_URL=http://localhost:8081
   ```

   Nếu mạng chặn `www.gstatic.com` (nơi tải CanvasKit), thêm `--no-web-resources-cdn` để đóng gói CanvasKit vào bản build.

## Token

- Backend cấp `token` (access, mặc định 1 giờ) và `refreshToken` (mặc định 7 ngày, cấu hình `jwt.refresh-duration`).
- Refresh token có claim `typ=refresh` và bị từ chối nếu dùng làm access token.
- Flutter lưu hai token trong `flutter_secure_storage`. Khi gặp 401, interceptor gọi `/bff/auth/refresh` một lần rồi gửi lại request. Nếu refresh thất bại thì đăng xuất.
- Chưa có cơ chế thu hồi refresh token (đăng xuất chỉ xóa token ở client).
