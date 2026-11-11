# TigerNet Inventory

Hệ thống quản lý kho TigerNet.

```
Flutter Web (5173)  ──►  BFF (8081)  ──►  Backend (8080)  ──►  MySQL 8 (3306)
```

| Thư mục | Nội dung |
| --- | --- |
| `backend/` | API Spring Boot (Java 21, Maven) |
| `bff/` | Backend for Frontend, gộp dữ liệu cho từng màn hình. Chi tiết: [`bff/README.md`](bff/README.md) |
| `frontend/` | Ứng dụng Flutter Web |
| `database/` | Script tạo database. Nhánh `dev`: [`database/dev/README.md`](database/dev/README.md) |
| `docs/` | SRS, thiết kế database, cấu trúc frontend, backlog |

## Cần cài trước

- JDK 21 (Maven không cần cài, dùng `mvnw` có sẵn)
- MySQL 8
- Flutter SDK (Dart `^3.12.2`)

## 1. Tạo database

Nhánh `dev` dùng database `tigernet_dev`. Mở PowerShell ở thư mục gốc repo:

```powershell
powershell -ExecutionPolicy Bypass -File database\dev\create-dev-db.ps1
```

Script hỏi mật khẩu MySQL rồi tạo bảng, view và dữ liệu nền (vai trò, kho `MAIN`...). Thêm `-Reset` để xoá và tạo lại. Các tuỳ chọn khác xem [`database/dev/README.md`](database/dev/README.md).

## 2. Cấu hình backend

Chép file mẫu rồi điền giá trị:

```powershell
Copy-Item backend\.env.example backend\.env
```

| Biến | Ý nghĩa |
| --- | --- |
| `DB_NAME` | `tigernet_dev` cho nhánh `dev`. Xoá dòng này (đừng để trống) để dùng `tigernet` |
| `DB_HOST`, `DB_PORT` | Mặc định `localhost`, `3306` |
| `DB_USERNAME`, `DB_PASSWORD` | Tài khoản MySQL |
| `JWT_SIGNER_KEY` | Khoá ký JWT HS512, tối thiểu 64 byte |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Tài khoản Gmail (app password) để gửi mail |

Tạo `JWT_SIGNER_KEY` ngẫu nhiên:

```powershell
$b = New-Object byte[] 64; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

`backend/.env` đã nằm trong `.gitignore`. Không commit file này.

## 3. Khởi chạy

Chạy từ thư mục gốc, mỗi lệnh một terminal, theo đúng thứ tự:

```powershell
# Terminal 1: Backend (8080)
cd backend; .\mvnw.cmd spring-boot:run

# Terminal 2: BFF (8081)
cd bff; .\mvnw.cmd spring-boot:run

# Terminal 3: Flutter Web (5173)
cd frontend; flutter pub get; flutter run -d web-server --web-port 5173 --web-hostname localhost
```

Trên macOS/Linux dùng `./mvnw` thay cho `.\mvnw.cmd`.

Mở **http://localhost:5173**. Dùng `localhost`, không dùng `127.0.0.1`: server web có thể chỉ nghe trên IPv6 `::1`.

Swagger của backend: http://localhost:8080/swagger-ui.html

## 4. Tạo tài khoản đầu tiên

Database mới chưa có người dùng. Endpoint `/users` đang tạm mở cho môi trường dev, nên có thể tạo tài khoản admin khi backend đang chạy:

```powershell
$body = @{
  username = "admin"
  password = "<mật khẩu từ 8 ký tự>"
  fullName = "Quản trị viên"
  email    = "admin@example.com"
  roleId   = 5   # ADMIN trong database vừa tạo
  isActive = $true
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/users -ContentType "application/json" -Body $body
```

Sau đó đăng nhập trên Flutter Web bằng `username` và mật khẩu vừa tạo. Vai trò có sẵn: `WAREHOUSE`, `ACCOUNTANT`, `SALES`, `MANAGER`, `ADMIN` (id lần lượt 1 → 5 khi database tạo mới).

## Chạy demo không cần backend

Flutter có chế độ dùng dữ liệu mẫu (`frontend/assets/demo/fixtures.json`), không gọi mạng:

```powershell
cd frontend
flutter run -d web-server --web-port 5173 --web-hostname localhost --dart-define=DEMO_MODE=true
```

## Biến môi trường khác

| Biến | Thành phần | Mặc định |
| --- | --- | --- |
| `BACKEND_BASE_URL` | BFF | `http://localhost:8080` |
| `BFF_PORT` | BFF | `8081` |
| `BFF_ALLOWED_ORIGINS` | BFF | `http://localhost:*,http://127.0.0.1:*` |
| `BFF_BASE_URL` | Flutter (`--dart-define`) | `http://localhost:8081` |

## Lỗi thường gặp

| Hiện tượng | Cách xử lý |
| --- | --- |
| Backend báo `Access denied for user` | Kiểm tra `DB_USERNAME`, `DB_PASSWORD` trong `backend/.env` |
| Backend báo `Unknown database 'tigernet_dev'` | Chạy lại bước 1 |
| Đăng nhập báo lỗi 503 / mã `9503` | Backend chưa chạy hoặc chưa khởi động xong |
| Trang trắng khi mở `127.0.0.1:5173` | Mở `http://localhost:5173` |
| Flutter không tải được CanvasKit | Mạng chặn `www.gstatic.com`; build với `--no-web-resources-cdn` |
