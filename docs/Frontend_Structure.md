# CẤU TRÚC FRONTEND – TIGERNET (Flutter)

> Nguồn: `BACKLOG.md`, NOTE UXUI (`Kế hoạch TigerNet.xlsx`), `BIEU_MAU.md`, `Backend_Structure_v6.md`, `Database_Design_v7.md` (ERD v7 — nguồn chuẩn, tên enum/trạng thái lấy theo đây).
> Checklist từng trang: `UXUI_Checklist.xlsx`.

- **Stack** (đã có trong `pubspec.yaml`): `flutter_riverpod`, `go_router`, `dio`, `flutter_secure_storage`, `google_fonts`, `intl`, `freezed_annotation` / `json_annotation`.
- **Target:** Flutter Web là chính (1 bản build, white-label theo subdomain). Android và Windows dùng chung code.
- **Hiện trạng:** `lib/main.dart` vẫn là app đếm số mẫu, chưa có cấu trúc.

---

## 1. Cây thư mục

Tổ chức **theo tính năng (package-by-feature)**, giống backend. Mỗi tính năng chia 3 lớp:

| Lớp | Chứa |
| --- | --- |
| `data/` | Gọi API (`*_api.dart`) và model freezed (`models/`) |
| `application/` | Provider / Notifier của Riverpod (state, gọi API) |
| `presentation/` | Trang (`*_page.dart`) và widget riêng của tính năng (`widgets/`) |

```
frontend/lib/
├── main.dart                         # khởi tạo ProviderScope, chạy App
├── app.dart                          # MaterialApp.router, theme sáng/tối
│
├── core/                             # Hạ tầng, không chứa nghiệp vụ
│   ├── config/
│   │   └── env.dart                  # API base URL theo dev/prod
│   ├── network/
│   │   ├── dio_client.dart
│   │   ├── auth_interceptor.dart     # gắn access token, tự refresh khi 401
│   │   ├── error_interceptor.dart    # đổi lỗi API → thông báo tiếng Việt cho toast
│   │   ├── api_response.dart         # khớp ApiResponse của backend
│   │   └── page_response.dart        # khớp PageResponse (phân trang)
│   ├── storage/
│   │   └── token_storage.dart        # flutter_secure_storage
│   ├── auth/
│   │   ├── session_controller.dart   # trạng thái đăng nhập + /identity/me
│   │   ├── role.dart                 # WAREHOUSE, ACCOUNTANT, SALES, MANAGER, ADMIN
│   │   ├── permissions.dart          # vai trò → quyền, menu nào hiện
│   │   └── warehouse_scope.dart      # kho đang chọn (mặc định Is_Default), "Tất cả kho" cho QL/QT/KT (CHUNG-07)
│   ├── router/
│   │   ├── app_router.dart           # khai báo GoRouter
│   │   ├── routes.dart               # tên và đường dẫn các route
│   │   └── guards.dart               # chưa đăng nhập → /login; sai vai trò → /403
│   ├── theme/
│   │   ├── app_theme.dart            # ThemeData sáng/tối, độ mờ modal (#4)
│   │   ├── app_colors.dart
│   │   ├── app_typography.dart       # Geist, tiêu đề in hoa (#2, #3, #10)
│   │   └── theme_mode_provider.dart
│   └── utils/
│       ├── formatters.dart           # tiền VND, ngày dd/MM/yyyy
│       ├── validators.dart           # số điện thoại, email, số không âm
│       └── file_saver.dart           # tải file Excel/PDF từ API in
│
├── shared/                           # Widget dùng lại nhiều nơi
│   ├── layout/
│   │   ├── app_shell.dart            # khung chứa navbar, menu và nội dung
│   │   ├── nav_bar.dart              # logo to, chữ 20, chuông badge (#7, #8)
│   │   ├── warehouse_selector.dart   # chọn kho làm việc trên navbar (CHUNG-07)
│   │   ├── side_menu.dart            # lọc theo permissions (#12)
│   │   └── responsive_layout.dart    # Desktop / Tablet / Mobile
│   ├── data_table/
│   │   ├── app_data_table.dart       # header cố định, sort, kẻ dòng (#1, #13)
│   │   ├── column_filter.dart
│   │   └── pagination_bar.dart       # 20 / 50 dòng
│   ├── feedback/
│   │   ├── toast.dart                # (#14)
│   │   ├── confirm_dialog.dart
│   │   ├── app_modal.dart
│   │   ├── loading_view.dart
│   │   ├── empty_view.dart
│   │   └── error_view.dart
│   ├── form/
│   │   ├── money_field.dart
│   │   ├── date_range_field.dart
│   │   ├── search_field.dart
│   │   ├── async_dropdown.dart       # chọn NCC, KH, SP, kho
│   │   ├── serial_picker.dart        # chọn serial còn tồn
│   │   └── receipt_line_editor.dart  # bảng dòng SP dùng chung cho mọi loại phiếu
│   ├── widgets/
│   │   ├── page_header.dart          # tiêu đề in hoa + các nút thao tác
│   │   ├── status_chip.dart          # màu theo trạng thái phiếu
│   │   ├── print_button.dart         # menu chọn Excel / PDF
│   │   └── stat_card.dart
│   └── models/
│       └── enums.dart                # theo ERD v7: RentalType (RENT, LEND), InventoryStatus, ImportStatus,
│                                     # ExportStatus, StocktakeStatus, AlertType, PaymentStatus, ...
│
└── features/
    ├── public/
    │   └── presentation/home_page.dart
    ├── auth/
    │   ├── data/                     # auth_api.dart, login_request.dart, ...
    │   ├── application/              # login_controller.dart, forgot_password_controller.dart
    │   └── presentation/             # login_page, forgot_password_page, verify_otp_page,
    │                                 # reset_password_page
    ├── profile/                      # profile_page, change_password_page
    ├── dashboard/                    # dashboard_page + widgets/ (biểu đồ nhập/xuất, stat cards)
    ├── alerts/                       # alerts_page, alert_popup, alert_badge
    │
    ├── catalog/
    │   ├── products/                 # product_list, product_form, product_detail (tab serial)
    │   └── product_groups/           # product_group_page
    ├── partners/
    │   ├── suppliers/                # supplier_list, supplier_form, supplier_detail
    │   └── customers/                # customer_list, customer_form, customer_detail, receiver_dialog
    ├── warehouses/                   # [v6] warehouse_list, warehouse_form (QL-12); phân công kho
    │                                 # nằm ở admin/users/user_warehouses_dialog (QT-07)
    ├── inventory/                    # inventory_page, stock_summary_page, low_stock_page
    │
    ├── import_receipts/
    │   ├── data/
    │   │   ├── import_receipt_api.dart
    │   │   └── models/               # import_receipt.dart, import_receipt_line.dart
    │   ├── application/
    │   │   ├── import_receipt_list_provider.dart
    │   │   └── import_receipt_form_controller.dart
    │   └── presentation/
    │       ├── import_receipt_list_page.dart
    │       ├── import_receipt_form_page.dart
    │       └── import_receipt_detail_page.dart   # xác nhận / huỷ / in
    ├── export_receipts/              # list, form (chọn kho, kiểm tra tồn tại kho, VAT), detail (duyệt, hoàn tất),
    │                                 # payment_dialog (cộng Paid — không có lịch sử thanh toán ở P1),
    │                                 # chọn in phiếu xuất / phiếu giao hàng
    ├── rentals/                      # rental_list (tab RENT/LEND), rental_form, rental_detail,
    │                                 # expiring_page, rental_return_page, return_list, return_detail
    ├── transfers/                    # [v6] (P2 — TK-17) transfer_list, transfer_form, transfer_detail
    ├── stocktakes/                   # stocktake_list, stocktake_form (toàn kho / SP chọn — kiểm kê đột xuất),
    │                                 # stocktake_count_page, stocktake_detail (chốt = duyệt),
    │                                 # adjustment_list/detail (chỉ xem — sinh khi chốt kiểm kê)
    ├── reports/                      # inventory_report, import_export_report, receivables_report,
    │                                 # reconciliation_page, rental_report (phiếu thu/chi: P2 — KT-10)
    ├── admin/
    │   ├── users/                    # user_list, user_form, user_warehouses_dialog (QT-07)
    │   ├── roles/                    # role_matrix_page
    │   ├── audit_logs/               # audit_log_page
    │   ├── settings/                 # vat_options_page, rental_rules_page (chỉ đọc), lookups_page
    │   └── access_stats/             # (P2) access_stats_page
    └── sales_returns/                # (P2) list, form, detail, fee_policy_page
```

`import_receipts/` là ví dụ đầy đủ cả 3 lớp. Các tính năng khác dùng đúng khuôn này, cây trên chỉ liệt kê các trang chính.

---

## 2. Bản đồ route

Mọi route `/app/**` nằm trong một `ShellRoute` dùng `AppShell` và đi qua `guards.dart`.

| Route | Trang | Vai trò |
| --- | --- | --- |
| `/` | Trang chủ công khai | Khách |
| `/login`, `/forgot-password`, `/forgot-password/otp`, `/reset-password` | Xác thực | Khách |
| `/403`, `/404` | Lỗi | Tất cả |
| `/app/dashboard` | Dashboard | Tất cả |
| `/app/alerts` | Trung tâm cảnh báo | Tất cả |
| `/app/profile`, `/app/profile/password` | Hồ sơ, đổi mật khẩu | Tất cả |
| `/app/products[/new \| /:id \| /:id/edit]` | Sản phẩm | TK, QL (KT, BH xem) |
| `/app/product-groups` | Nhóm sản phẩm | QL |
| `/app/suppliers[...]` | Nhà cung cấp | TK, QL (KT xem) |
| `/app/customers[...]` | Khách hàng + người nhận | BH, QL |
| `/app/warehouses[...]` | Kho hàng **[v6]** | QL, QT |
| `/app/inventory`, `/summary`, `/low-stock` | Tồn kho (theo kho đang chọn) | TK, KT, QL |
| `/app/import-receipts[...]` | Phiếu nhập | TK, KT, QL |
| `/app/export-receipts[...]` | Phiếu xuất / bán | BH, TK, KT, QL |
| `/app/rentals[...]`, `/app/rentals/expiring`, `/app/rentals/:id/return` | Thuê / mượn, trả hàng | BH, TK, QL |
| `/app/rental-returns[/:id]` | Phiếu trả hàng thuê | TK, BH, QL |
| `/app/stocktakes[...]`, `/app/stocktakes/:id/count` | Kiểm kê | TK, QL |
| `/app/adjustments`, `/app/adjustments/:id` | Phiếu điều chỉnh (chỉ xem) | TK, KT, QL |
| `/app/transfers[...]` | (P2) Chuyển kho **[v6]** | TK, QL |
| `/app/reports/inventory` | Báo cáo tồn kho | TK, KT, QL |
| `/app/reports/import-export`, `/receivables`, `/reconciliation` | Báo cáo kế toán | KT, QL |
| `/app/reports/rental` | Báo cáo thuê / mượn | QL |
| `/app/admin/users`, `/roles`, `/audit-logs` | Quản trị người dùng, phân công kho | QT (QL xem) |
| `/app/admin/settings/*` | Cấu hình VAT, quy tắc thuê, danh mục tra cứu | QT, QL |
| `/app/admin/access-stats`, `/app/admin/branding` | (P2) | QT |
| `/app/sales-returns[...]` | (P2) Trả hàng bán | BH, QL |

`[...]` là viết tắt của `/new`, `/:id`, `/:id/edit`.

---

## 3. Menu theo vai trò

`permissions.dart` là nơi duy nhất quyết định menu nào hiện. Dữ liệu lấy từ `/identity/me` và `/identity/roles` (QT-02).

| Menu | Thủ kho | Kế toán | Bán hàng | Quản lý | Quản trị |
| --- | :-: | :-: | :-: | :-: | :-: |
| Dashboard, Cảnh báo | ✓ | ✓ | ✓ | ✓ | ✓ |
| Sản phẩm / Tồn kho | sửa | xem | xem | ✓ | |
| Nhà cung cấp | ✓ | xem | | ✓ | |
| Khách hàng | | | ✓ | ✓ | |
| Phiếu nhập | ✓ | xem/in | | duyệt | |
| Phiếu xuất | hoàn tất | thanh toán | tạo | duyệt | |
| Thuê/mượn, trả hàng | trả | | ✓ | ✓ | |
| Kiểm kê / Điều chỉnh | ✓ | xem | | chốt | |
| Kho hàng | | | | ✓ | ✓ |
| Chuyển kho (P2) | ✓ | | | ✓ | |
| Báo cáo | tồn kho | nhập-xuất, công nợ | | tất cả | |
| Người dùng, Audit, Cấu hình | | | | xem | ✓ |

Ẩn menu chưa đủ. Guard của router phải chặn luôn khi người dùng gõ URL trực tiếp (PUB-01 AC2), và backend vẫn trả 403 (TC-RBAC-01).

**Phạm vi kho:** Thủ kho và Bán hàng chỉ thấy các kho được phân công (`/identity/me`); Quản lý, Quản trị, Kế toán có thêm "Tất cả kho". Mọi provider danh sách/báo cáo đọc `warehouse_scope` và gửi `warehouseId`.

---

## 4. Quy ước

1. **Gọi API:** chỉ đi qua `core/network/dio_client.dart`. Trang không gọi `Dio` trực tiếp.
2. **State:** mỗi danh sách dùng `AsyncNotifier` giữ filter, sort và trang hiện tại. Mỗi form dùng `Notifier` riêng.
3. **Model:** dùng freezed + json_serializable, đặt trong `data/models/`. Enum dùng chung đặt ở `shared/models/enums.dart`.
4. **In phiếu:** backend sinh file (`GET .../print?format=excel|pdf`). FE chỉ dùng `print_button` + `file_saver`, không dựng PDF ở client.
5. **Lỗi và thông báo:** `error_interceptor` đổi mã lỗi thành câu tiếng Việt và hiện bằng `toast` (#14). 409 nghĩa là sai trạng thái phiếu, 403 nghĩa là không có quyền.
6. **Bảng dữ liệu:** mọi trang danh sách dùng `AppDataTable`, không tự dựng `DataTable` (#1, #13).
7. **Đặt tên file:** `snake_case`. Trang dùng hậu tố `_page.dart`, dialog dùng `_dialog.dart`, API dùng `_api.dart`.

---

## 5. Thứ tự dựng (bám milestone)

| Milestone | Phần frontend |
| --- | --- |
| **M1** (tuần 1–5) | `core/*`, `shared/layout`, `shared/feedback`, `shared/data_table`, `auth/` (đăng nhập), `public/`, `profile/`, guard + menu theo vai trò |
| **M2** (tuần 6–8) | Quên mật khẩu (OTP), `catalog/`, `partners/`, `warehouses/` + `warehouse_selector` + phân công kho, `inventory/`, `admin/users`, `roles`, `audit_logs` |
| **M3** (tuần 8–10) | `shared/form/receipt_line_editor`, `serial_picker`, `import_receipts/`, `export_receipts/` + thanh toán + in |
| **M4** (tuần 10–12) | `rentals/` (thuê, mượn, trả), `stocktakes/` (kiểm kê, chốt, xem điều chỉnh), xem quy tắc thuê |
| **M5** (tuần 11–13) | `dashboard/`, `alerts/` (pop-up + badge), `reports/`, `admin/settings`, kiểm tra responsive + dark mode |
| **M6** (Phase 2) | `transfers/`, phiếu thu/chi, `sales_returns/`, `admin/access_stats`, branding, tab tài liệu / TK ngân hàng NCC |

---

## 6. Đã chốt (06/10/2026)

1. **Kho và chuyển kho [v6]:** danh mục kho, phân công nhân viên và bộ chọn kho là **P1** (`CHUNG-07`, `QL-12`, `QT-07`); chuyển kho (`transfers/`) là **P2** (`TK-17`).
2. **Tên loại phiếu mượn:** thống nhất **`LEND`** (theo ERD v7) ở FE, BE và tài liệu.
3. **Còn mở — Trang đăng ký (G-1):** NOTE UXUI #11 ghi "đã xong" nhưng chưa có API. Khuyến nghị bỏ trang này, để Quản trị tạo tài khoản.
