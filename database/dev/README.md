# Database riêng cho nhánh `dev`

Nhánh `dev` (bản demo) dùng database **`tigernet_dev`**, tách khỏi database chính **`tigernet`**,
để dữ liệu thử nghiệm không lẫn vào dữ liệu thật. Cả hai cùng nằm trên MySQL 8 ở máy local.

Schema và dữ liệu danh mục nền lấy từ [`docs/Database_Script_v7.sql`](../../docs/Database_Script_v7.sql),
chỉ đổi tên database. Không cần sửa hay sao chép file script đó.

## 1. Tạo database `tigernet_dev`

Mở PowerShell ở thư mục gốc repo:

```powershell
powershell -ExecutionPolicy Bypass -File database\dev\create-dev-db.ps1
```

Script hỏi mật khẩu MySQL của `root`, rồi tạo `tigernet_dev` với đầy đủ bảng, view và dữ liệu nền
(vai trò, trạng thái, kho `MAIN`...). Database `tigernet` không bị đụng tới.

Tùy chọn:

| Tham số | Ý nghĩa |
|---|---|
| `-Reset` | Xóa `tigernet_dev` và tạo lại từ đầu (mất toàn bộ dữ liệu dev). Dùng khi database đã có bảng hoặc khi schema đổi. |
| `-User <tên>` | Tài khoản MySQL, mặc định `root`. |
| `-DbHost`, `-Port` | Mặc định `localhost`, `3306`. |
| `-DbName <tên>` | Đổi tên database dev, mặc định `tigernet_dev`. |
| `-MysqlExe <đường dẫn>` | Đường dẫn `mysql.exe` nếu không có trong PATH (script tự tìm trong `C:\Program Files\MySQL\MySQL Server *\bin`). |
| `-OutFile <file.sql>` | Chỉ sinh file SQL, không chạy. Mở file đó bằng MySQL Workbench và chạy toàn bộ. |

## 2. Trỏ backend vào `tigernet_dev`

Trong `backend/.env` (chép từ `backend/.env.example` nếu chưa có), thêm:

```properties
DB_NAME=tigernet_dev
```

Backend đọc `DB_NAME` trong `spring.datasource.url`. Không có dòng này thì backend dùng `tigernet` như trước.
Muốn quay về database chính, xóa dòng `DB_NAME` (đừng để trống giá trị).

Có thể đổi thêm `DB_HOST`, `DB_PORT` nếu MySQL không chạy ở `localhost:3306`.
