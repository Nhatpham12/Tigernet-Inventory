# Người dùng mẫu để test hiển thị theo chức danh

Hệ thống có đúng **5 chức danh** (bảng `Roles`, seed ở `database/tenant/V2__seed_reference_data.sql`;
frontend: `frontend/lib/core/auth/role.dart`). Mỗi chức danh có một tài khoản demo.

## Tài khoản

Mật khẩu chung là giá trị `DEMO_PASSWORD` (local: `backend/.env`; máy chủ: `/opt/tigernet/.env`).
Không ghi mật khẩu vào repo hay tài liệu.

| Tài khoản | Chức danh (Role_Name) | Tên hiển thị | Kho được phân công |
|---|---|---|---|
| `demo_admin` | Quản trị viên (`ADMIN`) | Demo Quản trị | — (thấy mọi kho) |
| `demo_manager` | Quản lý (`MANAGER`) | Demo Quản lý | — |
| `demo_accountant` | Kế toán (`ACCOUNTANT`) | Demo Kế toán | — |
| `demo_warehouse` | Thủ kho (`WAREHOUSE`) | Demo Thủ kho | `MAIN` (mặc định) |
| `demo_sales` | Bán hàng (`SALES`) | Demo Bán hàng | `MAIN` (mặc định) |

Tạo / đặt lại trên máy chủ Docker (máy chủ hoặc local chạy compose):

```bash
cd /opt/tigernet && ./deploy/scripts/seed-demo-users.sh
```

Script upsert theo `Username`, chạy lại bao nhiêu lần cũng được (đặt lại mật khẩu = `DEMO_PASSWORD`, bật lại tài khoản).

## Mỗi chức danh thấy gì trên menu

Nguồn: ma trận trong `frontend/lib/core/router/menu.dart` (theo `docs/Frontend_Structure.md` §3).
Nhóm menu không còn mục con nào thì bị ẩn. Đây là quyền **mặc định theo vai trò**; nếu Quản trị đã
đặt "Phân quyền trang" riêng cho nhân viên thì nhân viên đó theo danh sách riêng (trừ menu Quản trị).

✓ = thấy, — = ẩn.

| Menu / trang | ADMIN | MANAGER | ACCOUNTANT | WAREHOUSE | SALES |
|---|:-:|:-:|:-:|:-:|:-:|
| **Tổng quan** | ✓ | ✓ | ✓ | ✓ | ✓ |
| **Sản phẩm** › Danh sách sản phẩm | ✓ | ✓ | ✓ | ✓ | ✓ |
| Sản phẩm › Danh mục sản phẩm | ✓ | ✓ | — | ✓ | — |
| Sản phẩm › Thêm sản phẩm | ✓ | ✓ | — | ✓ | — |
| **Nhập hàng** › Danh sách phiếu nhập | ✓ | ✓ | ✓ | ✓ | — |
| Nhập hàng › Tạo phiếu nhập | ✓ | ✓ | — | ✓ | — |
| Nhập hàng › Danh sách nhà cung cấp | ✓ | ✓ | ✓ | ✓ | — |
| Nhập hàng › Thêm nhà cung cấp | ✓ | ✓ | — | ✓ | — |
| **Xuất kho** › Danh sách phiếu xuất | ✓ | ✓ | ✓ | ✓ | ✓ |
| Xuất kho › Tạo phiếu xuất | ✓ | ✓ | — | — | ✓ |
| Xuất kho › Khách hàng | ✓ | ✓ | ✓ | — | ✓ |
| Xuất kho › Danh sách đổi trả | ✓ | ✓ | — | ✓ | ✓ |
| **Chuyển kho** › Danh sách phiếu chuyển kho | ✓ | ✓ | — | ✓ | — |
| **Cho thuê** › Danh sách phiếu thuê | ✓ | ✓ | — | ✓ | ✓ |
| Cho thuê › Tạo phiếu cho thuê | ✓ | ✓ | — | — | ✓ |
| Cho thuê › Phiếu trả hàng | ✓ | ✓ | — | ✓ | ✓ |
| **Cho mượn** › Danh sách phiếu mượn | ✓ | ✓ | — | ✓ | ✓ |
| Cho mượn › Tạo phiếu cho mượn | ✓ | ✓ | — | — | ✓ |
| **Bảo hành** › Danh sách phiếu bảo hành | ✓ | ✓ | — | ✓ | ✓ |
| Bảo hành › Tiếp nhận bảo hành | ✓ | ✓ | — | ✓ | ✓ |
| **Kho** › Danh sách kho | ✓ | ✓ | — | — | — |
| Kho › Kiểm kê | ✓ | ✓ | — | ✓ | — |
| **Thu chi** › Phiếu thu / chi | ✓ | ✓ | ✓ | — | ✓ (chỉ phiếu thu) |
| Thu chi › Tạo phiếu thu | ✓ | ✓ | ✓ | — | — |
| Thu chi › Tạo phiếu chi | ✓ | ✓ | ✓ | — | — |
| **Báo cáo** › Tồn kho | ✓ | ✓ | ✓ | ✓ | — |
| Báo cáo › Nhập xuất kho | ✓ | ✓ | ✓ | — | — |
| Báo cáo › Công nợ | ✓ | ✓ | ✓ | — | — |
| Báo cáo › Cho thuê | ✓ | ✓ | — | — | — |
| Báo cáo › Đối chiếu công nợ | ✓ | ✓ | ✓ | — | — |
| **Quản trị** › Nhân viên | ✓ | ✓ (chỉ xem) | — | — | — |
| Quản trị › Phân quyền trang | ✓ | — | — | — | — |
| Quản trị › Lịch sử thao tác | ✓ | — | — | — | — |
| **Cài đặt** | ✓ | ✓ | ✓ | ✓ | ✓ |

Tóm tắt nhanh từng tài khoản:

- **demo_admin**: thấy toàn bộ menu, kể cả Phân quyền trang và Lịch sử thao tác; tạo/sửa/khóa nhân viên.
- **demo_manager**: thấy mọi nghiệp vụ và báo cáo; ở Quản trị chỉ có Nhân viên (xem danh sách, không thêm/sửa/xóa).
- **demo_accountant**: xem sản phẩm, phiếu nhập, nhà cung cấp, phiếu xuất, khách hàng; toàn quyền Thu chi;
  báo cáo Tồn kho, Nhập xuất, Công nợ, Đối chiếu công nợ. Không có Chuyển kho, Cho thuê, Cho mượn, Bảo hành, Kho.
- **demo_warehouse**: quản lý sản phẩm, danh mục, nhập hàng, nhà cung cấp, chuyển kho, kiểm kê, đổi trả, phiếu trả hàng,
  tiếp nhận bảo hành; xem phiếu xuất/thuê/mượn; báo cáo Tồn kho. Không có Thu chi, không tạo phiếu xuất/thuê/mượn.
- **demo_sales**: tạo phiếu xuất, cho thuê, cho mượn, tiếp nhận bảo hành, đổi trả, khách hàng; Thu chi chỉ xem phiếu thu.
  Không có Nhập hàng, Chuyển kho, Kho, Báo cáo.

## Quyền phía backend (chặn API, không chỉ ẩn menu)

Hiện backend (`SecurityConfig`) chỉ chặn theo vai trò ở các API sau; các API nghiệp vụ khác mở cho mọi người đã đăng nhập:

| API | Ai được gọi |
|---|---|
| `GET /users` (danh sách nhân viên) | ADMIN, MANAGER |
| Thêm/sửa/xóa nhân viên, phân quyền trang (`/users/**`) | ADMIN |
| `/audit-logs` (lịch sử thao tác) | ADMIN |
| Thêm/sửa/xóa kho (`POST/PUT/DELETE /warehouses`) | ADMIN, MANAGER |
| Sửa/xóa nhà cung cấp (`PUT/DELETE /suppliers/*`) | ADMIN, MANAGER, WAREHOUSE |

Vì vậy khi test: gõ thẳng URL một trang bị ẩn (VD `demo_sales` mở `/app/import-receipts`) thì router frontend chặn
và báo không có quyền; nhưng gọi API trực tiếp thì phần lớn vẫn được. Đây là việc còn mở.

## Gợi ý kịch bản test hiển thị

1. Đăng nhập lần lượt 5 tài khoản, chụp sidebar, so với bảng trên.
2. Với mỗi tài khoản, mở một URL trang bị ẩn để kiểm tra router chặn.
3. `demo_sales` mở Thu chi: danh sách chỉ có phiếu thu, không có nút tạo.
4. `demo_manager` mở Quản trị › Nhân viên: không có nút Thêm/Sửa/Xóa.
5. `demo_admin` đặt Phân quyền trang riêng cho `demo_sales` (VD bỏ Cho mượn), đăng nhập lại `demo_sales` để thấy menu đổi.
   Xong thì đặt lại về mặc định theo vai trò.
