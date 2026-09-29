# TIGERNET — PHÂN TÍCH YÊU CẦU CỐT LÕI & BACKLOG THEO VAI TRÒ

Kết quả của **GD1 — "Phân rã yêu cầu thành backlog/use case cho từng vai trò"** (Kế hoạch chi tiết, việc #2, tuần 1–2).

- **Nguồn:** `Kế hoạch TigerNet.xlsx` (Tổng Quan, Kế hoạch chi tiết — 57 việc, NOTE UXUI — 14 mục, API & Testing — 112 endpoint / 11 nhóm test / 20 test case), `tigernet_erd_final (1).png` (ERD ~30 bảng), `How_To_Sale.md`, `CRUD_ANALYSIS.md`, `Deployment_Guide.md`.
- **Loại bỏ:** `Kế hoạch TigerNet.backup.xlsx` (bản cũ, NOTE UXUI còn nội dung dự án weather-map — không dùng làm nguồn).
- **Ưu tiên:** **P1** = Phase 1 (bắt buộc trong 3 tháng / 13 tuần), **P2** = Phase 2 (mở rộng, không chặn nghiệm thu 27/12/2026).
- **Mã story:** `PUB` (chưa đăng nhập) · `CHUNG` (mọi vai trò) · `TK` (Thủ kho) · `KT` (Kế toán) · `BH` (Bán hàng) · `QL` (Quản lý) · `QT` (Quản trị hệ thống) · `OPS` (Admin vận hành SaaS) · `DEV` (nền tảng kỹ thuật).
- **Cấu trúc:** Phần 1 yêu cầu cốt lõi · Phần 2 backlog 84 story theo vai trò · Phần 3 truy vết 112 endpoint → story → test case · Phần 4 milestone M1–M6 · Phần 5 quy ước DoR/DoD & checklist tuần 1.
- **Bản rà soát (tuần 1):** đối soát 112/112 endpoint có story nhận (bổ sung 20 endpoint, sửa 4 tham chiếu chéo sai), chuẩn hoá bảng tổng kết 74 P1 / 10 P2, thêm G-7 (tải lịch tuần 12–13).

---

# PHẦN 1 — PHÂN TÍCH YÊU CẦU CỐT LÕI

## 1.1 Bối cảnh & mục tiêu doanh nghiệp

TigerNet là phần mềm **quản lý kho kiểu SaaS đa khách hàng** (mỗi khách hàng 1 database riêng) kèm gói **On-Premise Enterprise** bán riêng. Bốn yêu cầu cốt lõi rút ra từ toàn bộ tài liệu:

1. **Cách ly dữ liệu tuyệt đối giữa các tenant** — giá nhập, NCC, công nợ là dữ liệu nhạy cảm → `Database-per-tenant` + kiểm thử TC-TENANT-01 (0 rò rỉ chéo).
2. **Vận hành tập trung** — provisioning, migration hàng loạt, backup, phát hành version làm 1 lần cho tất cả tenant (Control Plane).
3. **Đủ nghiệp vụ một kho hàng thật** — danh mục → nhập → xuất/bán → cho thuê–mượn–trả → kiểm kê → báo cáo/cảnh báo.
4. **Phân trách rõ theo 5 vai trò, có đối soát** — mỗi hành động ghi `Audit_Logs` ("ai – làm gì – khi nào").

## 1.2 Phạm vi Phase 1 / Phase 2

| Hạng mục | Phase 1 (P1) | Phase 2 (P2) |
|---|---|---|
| Xác thực | JWT + refresh, bcrypt, OTP quên mật khẩu, khóa 15’ sau 5 lần sai | — |
| Danh mục | SP, nhóm SP, NCC, KH, người nhận, tồn kho, serial | Ảnh SP, tài liệu NCC, TK ngân hàng NCC |
| Nhập kho | Tạo/sửa/xác nhận/huỷ/in phiếu, cộng tồn, audit | — |
| Xuất & bán | Kiểm tra tồn, phiếu xuất 3 trạng thái, VAT/thanh toán/công nợ | — |
| Cho thuê | Phiếu thuê, cảnh báo trước 7 ngày, trả + phạt quá hạn ≥10 ngày | Trả hàng bán + `Return_Fee_Policy` |
| Kiểm kê | Phiếu kiểm kê → chênh lệch → phiếu điều chỉnh → duyệt | — |
| Báo cáo | Dashboard, tồn / nhập-xuất / cho thuê / công nợ, Excel/PDF | Xuất XML/Word |
| Hệ thống | Alerts, VAT/Payment/Transaction/Rental-rules, health | Branding tenant, license, backup API, CRUD mẫu VAT |
| Kiến trúc | SaaS multi-tenant, Flyway/Liquibase, CI/CD | License Server, gói On-Premise |

## 1.3 Định nghĩa 5 vai trò (RBAC)

| Vai trò | Trách nhiệm chính | Quyền tinh hoa |
|---|---|---|
| **Thủ kho** | Hàng vào/ra kho, serial, kiểm kê | Tạo & xác nhận phiếu nhập, hoàn tất xuất, nhập số liệu kiểm kê, lập phiếu điều chỉnh |
| **Kế toán** | Tiền, công nợ, đối soát | Ghi nhận thanh toán/công nợ, xem & in phiếu, báo cáo nhập-xuất/công nợ, TK ngân hàng NCC |
| **Bán hàng** | Khách hàng, chào bán, cho thuê | CRUD khách/người nhận, tạo phiếu xuất & phiếu thuê, theo dõi hạn thuê |
| **Quản lý** | Duyệt & kiểm soát | Duyệt phiếu xuất/kiểm kê/điều chỉnh, toàn bộ báo cáo, giám sát người dùng & audit log |
| **Quản trị hệ thống** | Quản trị trong tenant | CRUD người dùng & gán vai trò, mẫu VAT, cấu hình hệ thống |

**Ma trận nhanh (P1):** xem bảng map endpoint → story ở Phần 3. Nguyên tắc: *xem* thường rộng (Đã đăng nhập), *ghi* và *duyệt* siết theo vai trò; mọi vi phạm trả `403` (TC-RBAC-01, TC-SEC-01).

**Admin vận hành (tách riêng, không thuộc 5 vai trò):** người của công ty vận hành SaaS — quản lý tenant, secret kết nối DB, backup/restore, migration hàng loạt, CI/CD, License Server. Một số chức năng (license, backup) hiển thị cho `Quản trị hệ thống` trong giao diện tenant, nhưng *quyền sở hữu vận hành* thuộc nhóm `OPS`.

## 1.4 Yêu cầu phi chức năng (NFR)

| # | NFR | Ngưỡng kiểm thử |
|---|---|---|
| NFR-1 | Bảo mật | bcrypt cho mật khẩu, JWT refresh, OTP 10’, khóa 15’ sau 5 lần sai (TC-AUTH-01/02), OWASP Top 10, 0 lỗ hổng Critical/High |
| NFR-2 | Cách ly tenant | Đọc/ghi chéo tenant → 403/404 (TC-TENANT-01) |
| NFR-3 | Hiệu năng | Trang chính ≤2.5s, API p95 ≤500ms, 10.000 giao dịch/ngày (TC-PERF-01) |
| NFR-4 | Giao diện | Responsive Desktop/Tablet/Mobile, dark/light, đúng 14 mục NOTE UXUI, font Geist |
| NFR-5 | Truy vết | Mọi thao tác ghi/bấm quan trọng → `Audit_Logs` |
| NFR-6 | Backup | Backup tự động DB + file, restore về thời điểm bất kỳ (TC-BAK-01) |
| NFR-7 | Khả dụng | `/system/health` cho liveness/readiness, CI chạy xanh, 100% endpoint có test case |

## 1.5 Ràng buộc & giả định

- **Nhân sự:** 2 người — Phương (DB, FE), Nhựt (BE, Testing, Deploy); 13 tuần (28/09/2026 → 27/12/2026).
- **Stack:** Flutter Web (1 build duy nhất, white-labeling theo subdomain), Spring Boot (context-path `/identity` cho auth per `CRUD_ANALYSIS.md`), MySQL 8 database-per-tenant.
- **Giả định:** thứ tự GD1→GD12 trong kế hoạch giữ nguyên; backlog dưới đây bám 112 endpoint của sheet *API & Testing*.

## 1.6 Gap & rủi ro phát hiện khi review (cần chốt ở tuần 1)

| # | Gap | Đề xuất |
|---|---|---|
| G-1 | NOTE UXUI #11 ghi "trang **đăng ký**" nhưng 112 endpoint **không có** `POST /auth/register` — chỉ có `POST /users` (Quản trị) | Chốt: hoặc bỏ self-register (khuyến nghị, đúng mô hình B2B), hoặc bổ sung endpoint ở P1 → story `PUB-04` |
| G-2 | `GET /config/rental-rules` (xem quy tắc 7 ngày / 10 ngày phạt) **không có API sửa** | Giữ giá trị hard-code P1; thêm `PUT` ở P2 → `QL-11` |
| G-3 | GD7 có việc "trả bảo hành & hàng hư hỏng" (`WARRANTY`, `OTHER`) nhưng không có endpoint riêng | Dùng `POST /return-receipts` + `Transaction_Types` → gộp vào `TK-12` |
| G-4 | NOTE UXUI #9 (trang admin: thống kê hiển thị & lượt truy cập theo tháng) chưa có endpoint | Đưa sang P2 → `QT-06` |
| G-5 | 5/14 mục NOTE UXUI chưa fixed (#8 alert pop-up, #9, #12 giới hạn menu, #13 filter/sort, #14 toast) | Dành story `PUB-01`, `CHUNG-04`, `CHUNG-06`, `QT-06` |
| G-6 | File backup chứa nội dung dự án khác (weather-map) | Không dùng làm nguồn; cân nhắc xoá để tránh nhiễu |
| G-7 | Tuần 12–13 dồn cùng lúc GD10 (tích hợp/perf/security) + GD11 (backup/CI-CD/UAT env) + GD12 (UAT/docs/nghiệm thu) với đúng 2 người | Kéo perf + security sang tuần 11–12, chốt UAT env ở tuần 12; giữ tuần 13 cho UAT, fix lỗi và bàn giao |

> **Đã xử lý trong bản rà soát này:** 20 endpoint list/detail chưa có story nhận và 4 tham chiếu chéo sai (`KT-08`, `BH-02` → `QL-10` phải là `QL-07`; `BH-12` → `QL-07` phải là `QL-05`; `G-2` → `QL-09` phải là `QL-11`) — xem 3.1.1.

---

# PHẦN 2 — BACKLOG THEO VAI TRÒ

> Mỗi story: **Ưu tiên · Giai đoạn/GĐ + tuần · Endpoint · AC (tiêu chí chấp nhận)**. Story đánh dấu *dùng chung* có trong một vai trò chính, vai trò còn lại chỉ tham chiếu.

## 0. PUBLIC & CHUNG — khách chưa đăng nhập / mọi vai trò

### Epic PUB — Trang công khai & phiên đăng nhập

**PUB-01 — Giới hạn nội dung theo trạng thái đăng nhập**
- P1 · GD2 (tuần 1–4) · FE · Endpoint: — (NOTE UXUI #12)
- *Với tư cách khách chưa đăng nhập, tôi chỉ muốn xem được trang công khai để hệ thống không lộ dữ liệu nghiệp vụ.*
- AC:
  1. Chưa đăng nhập: chỉ thấy trang chủ công khai, trang đăng nhập, quên mật khẩu.
  2. Menu, bảng dữ liệu, dashboard bị ẩn/redirect về `/login` khi truy cập URL trực tiếp.
  3. Sau đăng nhập: menu hiển thị đúng theo vai trò trả về từ `/identity/me`.

**PUB-02 — Đăng nhập & phiên token**
- P1 · GD3 (tuần 4–5) · BE · `POST /identity/auth/login`, `POST /identity/auth/refresh`, `POST /identity/auth/logout`
- *Với tư cách người dùng, tôi muốn đăng nhập để truy cập hệ thống theo vai trò của mình.*
- AC:
  1. Đăng nhập đúng trả access + refresh token; sai mật khẩu trả 401.
  2. Sai mật khẩu 5 lần liên tiếp → khóa tạm 15 phút, trả 429/423 (TC-AUTH-01).
  3. Refresh token hết hạn → 401; logout thu hồi refresh token.
  4. Mật khẩu lưu dạng bcrypt, không bao giờ trả plaintext.

**PUB-03 — Quên & đặt lại mật khẩu bằng OTP email**
- P1 · GD3 (tuần 5–6) · BE · `POST /identity/auth/forgot-password`, `POST /identity/auth/reset-password`
- AC:
  1. Nhập email → gửi OTP; không tiết lộ email có tồn tại hay không (response thống nhất).
  2. OTP hiệu lực 10 phút, sai/hết hạn → 400 (TC-AUTH-02).
  3. Đặt lại mật khẩu thành công → hash bcrypt, thu hồi toàn bộ session cũ.

**PUB-04 — Đăng ký tài khoản (CẦN CHỐT — G-1)**
- P1/P2 · tuần 1 chốt · `POST /identity/auth/register` (chưa có trong 112 endpoint)
- *Nếu chốt bỏ self-register:* Quản trị hệ thống tạo user qua `POST /identity/users` → dời toàn bộ sang `QT-01`.

**PUB-05 — White-labeling theo tenant**
- P2 · GD2 · `GET /tenant/branding`
- AC: trả logo + theme màu theo subdomain; không thấy tenant → theme mặc định; không lộ thông tin kết nối DB.

**PUB-06 — Health check**
- P1 · GD2 (tuần 4) · `GET /system/health`
- AC: trả 200 khi DB kết nối OK; dùng cho Docker/K8s probe.

### Epic CHUNG — mọi vai trò đã đăng nhập

**CHUNG-01 — Hồ sơ & phân quyền hiện hành**
- P1 · GD3 · `GET /identity/me`, `GET /identity/roles`
- AC: trả thông tin user + vai trò + RBAC matrix; FE dùng để render menu (ghép `PUB-01`).

**CHUNG-02 — Đổi mật khẩu**
- P1 · GD3 · `POST /identity/auth/change-password`
- AC: yêu cầu mật khẩu cũ; sai → 400; thành công → bcrypt hash mới.

**CHUNG-03 — Dashboard tổng quan**
- P1 · GD9 (tuần 11–12) · `GET /dashboard/summary`, `GET /dashboard/charts`
- AC: hiển thị tồn kho, số phiếu chờ duyệt, doanh thu ngày; biểu đồ nhập/xuất theo kỳ; biểu đồ đặt đầu trang, co giãn theo màn hình (NOTE UXUI #5).

**CHUNG-04 — Trung tâm cảnh báo**
- P1 · GD9 · `GET /alerts`, `GET /alerts/unread-count`, `PUT /alerts/{id}/read`
- AC: 3 loại cảnh báo LOW_STOCK, RENTAL_EXPIRING, PENDING_APPROVAL; badge đếm chưa đọc trên Navbar, icon nổi bật; popup mỗi lần vào trang (NOTE UXUI #8); bấm đã đọc → badge giảm.

**CHUNG-05 — Xuất báo cáo ra file**
- P1 · GD9 (tuần 13) · `GET /reports/export?format=excel|pdf`
- AC: file mở được, số liệu khớp DB (TC-REP-01); Phase 2 mở rộng `xml|word`.

**CHUNG-06 — Bộ giao diện bảng dữ liệu & thông báo chuẩn**
- P1 · GD2/GD10 · FE · (NOTE UXUI #1, #13, #14)
- AC: bảng cỡ chữ lớn, kẻ dòng rõ, phân trang 20–50 dòng; filter/sort theo cột, header sticky khi cuộn; toast lỗi/thành công tiếng Việt; font Geist thống nhất, tiêu đề in hoa (NOTE UXUI #2, #10).

---

## 1. THỦ KHO (TK) — hàng vào/ra, serial, kiểm kê

### Epic TK-A — Danh mục & tồn kho

**TK-01 — Thêm/sửa sản phẩm & quản lý serial**
- P1 · GD4 (tuần 6–7) · `GET /products`, `POST /products`, `GET /products/{id}`, `PUT /products/{id}`, `GET /products/{id}/serials`
- *Với tư cách Thủ kho, tôi muốn thêm và sửa sản phẩm kèm giá nhập/ra, mức tồn tối thiểu để nhập hàng đúng giá.*
- AC:
  1. Thiếu tên / giá âm → 400, không ghi DB (TC-PROD-01).
  2. Sản phẩm serial bật cờ `is_serializable`; không serial thì bỏ qua bước nhập serial.
  3. `GET .../serials` trả trạng thái từng serial: tồn / đang thuê / đã xuất.
  4. Mọi thay đổi ghi `Audit_Logs`.

**TK-02 — Xem tồn kho & cảnh báo dưới ngưỡng**
- P1 · GD4 (tuần 7) · `GET /inventory`, `GET /inventory/{id}`, `GET /stock-summary`, `GET /inventory/low-stock`
- AC: lọc theo sản phẩm, serial, trạng thái, ngày nhập/ra; dòng dưới `min_stock_level` hiện cờ đỏ và sinh alert LOW_STOCK (ghép `CHUNG-04`).
- *Dùng chung: Kế toán (xem), Quản lý (xem).*

**TK-03 — CRUD nhà cung cấp**
- P1 · GD4 (tuần 7–8) · `GET /suppliers`, `POST /suppliers`, `GET /suppliers/{id}`, `PUT /suppliers/{id}`
- AC: search/phân trang; validate SĐT/email; xem lịch sử nhập theo NCC (module NCC).

**TK-04 — Tài liệu NCC & ảnh sản phẩm**
- P2 · `GET /suppliers/{id}/documents`, `POST /suppliers/{id}/documents`, `POST /products/{id}/image` (xoá tài liệu thuộc `QL-07`)
- AC: upload multipart, lưu `file_path`/`image_path`; giới hạn dung lượng & định dạng; chỉ Thủ kho/Quản lý.

### Epic TK-B — Nhập kho (GD5)

**TK-05 — Tạo phiếu nhập**
- P1 · GD5 (tuần 8–9) · `POST /import-receipts`
- AC:
  1. Chọn NCC, thêm nhiều dòng SP: số lượng, giá, serial (bắt buộc với SP serial).
  2. Tự tính tổng giá trị phiếu.
  3. Thiếu NCC/SP, giá âm → 400, không ghi DB.
  4. Phiếu sinh ra ở trạng thái *Chưa xác nhận*; ghi audit người tạo.

**TK-06 — Sửa phiếu nhập chưa xác nhận**
- P1 · GD5 (tuần 9) · `PUT /import-receipts/{id}`
- AC: chỉ sửa được phiếu chưa xác nhận; phiếu đã xác nhận → 409 (TC-IMP-02); nếu cho sửa thì tồn kho được ánh xạ ngược chính xác.

**TK-07 — Xác nhận phiếu nhập → cộng tồn**
- P1 · GD5 (tuần 8–9) · `PUT /import-receipts/{id}/confirm`
- AC: tồn tăng đúng số lượng, serial chuyển sang *tồn* (TC-IMP-01); sinh cảnh báo LOW_STOCK nếu vượt ngưỡng; ghi audit.

**TK-08 — Huỷ phiếu nhập chưa xác nhận**
- P1 · GD5 · `DELETE /import-receipts/{id}`
- AC: chỉ huỷ được phiếu chưa xác nhận; huỷ không ảnh hưởng tồn; ghi audit.

**TK-09 — In/xuất phiếu nhập**
- P1 · GD5 (tuần 9) · `GET /import-receipts/{id}/print?format=excel|pdf`
- AC: file mở được, tổng tiền khớp phiếu.

### Epic TK-C — Xuất kho: khâu thực hiện

**TK-10 — Kiểm tra tồn realtime trước khi xuất**
- P1 · GD6 (tuần 9–10) · `GET /stock-check`
- AC: trả tồn khả dụng theo productIds/serials; vượt tồn → 409, không trừ tồn (TC-EXP-01). *Dùng chung: Bán hàng.*

**TK-11 — Hoàn tất xuất kho (trừ tồn thực tế)**
- P1 · GD6 (tuần 10) · `PUT /export-receipts/{id}/complete`
- AC: chỉ từ trạng thái *Đã xác nhận* → *Đã xuất*; trừ tồn + serial; sai thứ tự trạng thái → 409 (TC-EXP-03); ghi audit. *Dùng chung: Quản lý.*

### Epic TK-D — Cho thuê & trả hàng

**TK-12 — Trả hàng thuê / bảo hành / hàng hư hỏng + tính phạt**
- P1 · GD7 (tuần 11–12) · `PUT /rental-receipts/{id}/return`, `POST /return-receipts`, `GET /return-receipts`, `GET /return-receipts/{id}`
- AC:
  1. Sinh `Return_Receipt` + chi tiết (condition_note, serial); serial chuyển trạng thái theo kết quả trả.
  2. Quá hạn ≥10 ngày: phạt = số ngày quá hạn × tiền thuê/ngày (TC-RENT-02).
  3. Giao dịch bảo hành/hư hỏng dùng `Transaction_Types = WARRANTY | OTHER` (đóng G-3).
  4. Ghi audit; tồn khả dụng cập nhật ngay. *Dùng chung: Bán hàng.*

### Epic TK-E — Kiểm kê & điều chỉnh (GD8)

**TK-13 — Lập phiếu kiểm kê & nhập số liệu thực tế**
- P1 · GD8 (tuần 11) · `POST /stocktakes`, `GET /stocktakes`, `GET /stocktakes/{id}`, `PUT /stocktakes/{id}/items`
- AC: phiếu kiểm kê liệt kê toàn bộ SP; nhập SL thực tế → hệ thống tự tính `difference`; SL âm/lớn bất thường → chặn, yêu cầu nhập lại (TC-STK-02).

**TK-14 — Tạo phiếu điều chỉnh thủ công**
- P1 · GD8 · `POST /stock-adjustments`
- AC: ghi lý do điều chỉnh (`Reason`); chờ Quản lý duyệt (→ `QL-04`).

**TK-15 — Xem báo cáo tồn kho**
- P1 · GD9 (tuần 12) · `GET /reports/inventory`
- AC: báo cáo theo thời điểm/danh mục/sản phẩm/địa điểm. *Dùng chung: Kế toán, Quản lý.*

## 2. KẾ TOÁN (KT) — tiền, công nợ, đối soát

**KT-01 — Xem & in phiếu nhập**
- P1 · GD5 (tuần 9) · `GET /import-receipts` (lọc NCC, ngày, trạng thái), `GET /import-receipts/{id}`, `GET /import-receipts/{id}/print`
- AC: lọc theo NCC/kỳ; in excel|pdf khớp dữ liệu phiếu. *Lịch sử nhập theo NCC phục vụ đối soát.*

**KT-02 — Xem tồn kho phục vụ đối soát**
- P1 · GD4 · `GET /inventory`, `GET /reports/inventory`
- AC: chỉ đọc, không chỉnh sửa tồn. *Dùng chung: Thủ kho, Quản lý.*

**KT-03 — Ghi nhận thanh toán & công nợ**
- P1 · GD6 (tuần 10) · `POST /export-receipts/{id}/payments`
- AC:
  1. Cập nhật theo `Payment_Statuses`; tính Tổng tiền – VAT – Đã trả = Còn lại đúng (TC-EXP-02).
  2. Không cho số tiền vượt còn lại; lịch sử thanh toán lưu vết.
  3. Ghi audit người ghi nhận.

**KT-04 — In phiếu xuất**
- P1 · GD6 · `GET /export-receipts/{id}/print?format=excel|pdf`
- AC: số liệu khớp phiếu, hiển thị VAT & công nợ còn lại.

**KT-05 — Báo cáo nhập/xuất theo NCC, khách hàng, sản phẩm**
- P1 · GD9 (tuần 12) · `GET /reports/import-export?groupBy=supplier|customer|product`
- AC: tổng hợp theo kỳ; số khớp chi tiết phiếu. *Dùng chung: Quản lý.*

**KT-06 — Báo cáo công nợ, tiền bán/thuê chưa thu**
- P1 · GD9 (tuần 12–13) · `GET /reports/receivables`
- AC: liệt kê KH còn công nợ, phân biệt bán/thuê; dữ liệu nguồn từ `Payment_Statuses`. *Dùng chung: Quản lý.* Lưu ý: Thủ kho gọi endpoint này phải 403 (TC-RBAC-01).

**KT-07 — Xem phiếu điều chỉnh tồn**
- P1 · GD8 · `GET /stock-adjustments`, `GET /stock-adjustments/{id}`
- AC: xem lý do & chênh lệch để hạch toán; không duyệt (duyệt thuộc `QL-04`).

**KT-08 — Quản lý tài khoản ngân hàng của NCC**
- P2 · `GET /suppliers/{id}/bank-accounts`, `POST /suppliers/{id}/bank-accounts`, `DELETE /suppliers/{id}/bank-accounts/{bankId}`
- AC: nhiều TK/NCC; xoá chỉ bởi Quản lý (xem `QL-07`).

## 3. BÁN HÀNG (BH) — khách hàng, xuất bán, cho thuê

### Epic BH-A — Khách hàng & người nhận

**BH-01 — CRUD khách hàng**
- P1 · GD4 (tuần 7–8) · `GET /customers`, `POST /customers`, `GET /customers/{id}`, `PUT /customers/{id}`
- AC: phân biệt KH bán / KH thuê; search, phân trang; validate SĐT/email.

**BH-02 — CRUD người nhận hàng**
- P1 · GD4 · `GET /customers/{id}/receivers`, `POST /customers/{id}/receivers`, `PUT /receivers/{receiverId}`, `DELETE /receivers/{receiverId}`
- AC: 1 KH nhiều người nhận; mặc định 1 người nhận; xoá mềm (xoá bởi Quản lý → `QL-07`).

### Epic BH-B — Xuất kho & bán hàng (GD6)

**BH-03 — Tạo phiếu xuất khi bán hàng**
- P1 · GD6 (tuần 9–10) · `POST /export-receipts`
- AC: chọn KH/người nhận, dòng SP + serial; áp mẫu VAT (`VAT_Options`); trạng thái khởi tạo *Chờ xác nhận*; gọi `stock-check` trước (→ `TK-10`).

**BH-04 — Sửa / huỷ phiếu xuất chưa xác nhận**
- P1 · GD6 (tuần 10) · `PUT /export-receipts/{id}`, `DELETE /export-receipts/{id}`
- AC: chỉ tác động được phiếu *Chờ xác nhận*; đã xác nhận → 409; ghi audit.

**BH-05 — In phiếu xuất**
- P1 · GD6 · `GET /export-receipts/{id}/print?format=excel|pdf`
- AC: bản in đủ KH, SP, VAT, đã trả/còn lại. *Dùng chung: Kế toán.*

**BH-06 — Duyệt & luồng trạng thái phiếu xuất** *(vai trò chính: Quản lý)*
- P1 · `PUT /export-receipts/{id}/confirm` → xem `QL-01`. Bán hàng theo dõi trạng thái qua `GET /export-receipts` (lọc trạng thái/KH/ngày) và `GET /export-receipts/{id}`.

### Epic BH-C — Cho thuê (GD7)

**BH-07 — Tạo phiếu cho thuê**
- P1 · GD7 (tuần 10–11) · `POST /rental-receipts`
- AC:
  1. Chọn serial còn tồn (TC-RENT-01), thời hạn, tiền thuê/ngày, tiền cọc.
  2. Serial chuyển *đang thuê*, loại khỏi tồn khả dụng ngay khi xác nhận.
  3. Tính tổng tiền thuê dựa số ngày; validate ngày trả > ngày thuê.
  4. Trạng thái khởi tạo *Chờ xác nhận*; ghi audit.

**BH-08 — Sửa / huỷ phiếu thuê chưa xác nhận**
- P1 · GD7 · `PUT /rental-receipts/{id}`, `DELETE /rental-receipts/{id}`
- AC: huỷ phiếu chưa xác nhận thì serial trả về tồn khả dụng.

**BH-09 — Theo dõi hàng đang thuê & hạn trả**
- P1 · GD7 (tuần 11) · `GET /rental-receipts` (lọc hạn trả), `GET /rental-receipts/{id}`, `GET /rental-receipts/expiring?days=7`
- AC: danh sách phiếu theo trạng thái/KH/hạn; còn 7 ngày → sinh RENTAL_EXPIRING (TC-RENT-03) hiện ở `CHUNG-04`.

**BH-10 — Trả hàng thuê & hoàn cọc** *(dùng chung)*
- P1 · xem `TK-12` — Bán hàng cùng quyền gọi `PUT /rental-receipts/{id}/return`.

### Epic BH-D — Trả hàng bán (P2)

**BH-11 — Trả hàng / đổi khách & phí hoàn trả**
- P2 · `POST /sales-returns`, `GET /sales-returns`, `GET /sales-returns/{id}`, `GET /return-fee-policies`, `PUT /return-fee-policies/{id}` (PUT: chỉ Quản lý)
- AC: áp phí theo `Return_Fee_Policy`; lưu original_value, refund_amount, fee_percent; chờ duyệt.

**BH-12 — Duyệt trả hàng bán** *(vai trò chính: Quản lý)*
- P2 · `PUT /sales-returns/{id}/approve` → xem `QL-05`.

## 4. QUẢN LÝ (QL) — duyệt, kiểm soát, báo cáo

### Epic QL-A — Duyệt & phê quyết

**QL-01 — Duyệt phiếu xuất**
- P1 · GD6 (tuần 10) · `PUT /export-receipts/{id}/confirm`
- AC: *Chờ xác nhận → Đã xác nhận*; không nhảy trạng thái sai; mọi bước ghi audit (TC-EXP-03).

**QL-02 — Duyệt / giám sát phiếu nhập**
- P1 · GD5 · `PUT /import-receipts/{id}/confirm`, `DELETE /import-receipts/{id}` (quyền Thủ kho + Quản lý)
- AC: xác nhận cùng AC với `TK-07`; có thể giám sát, huỷ phiếu sai phạm; ghi audit.

**QL-03 — Chốt kiểm kê → tự tạo phiếu điều chỉnh**
- P1 · GD8 (tuần 11) · `POST /stocktakes/{id}/approve`
- AC: SL thực tế ≠ SL hệ thống → tự sinh phiếu điều chỉnh, tồn khớp số thực tế (TC-STK-01); không chênh lệch → chốt không sinh phiếu.

**QL-04 — Duyệt phiếu điều chỉnh tồn**
- P1 · GD8 (tuần 12) · `PUT /stock-adjustments/{id}/approve`
- AC: duyệt → áp dụng vào tồn kho ngay; từ chối → phiếu về trạng thái tương ứng; cả hai đều ghi audit.

**QL-05 — Duyệt trả hàng bán (P2)**
- P2 · `PUT /sales-returns/{id}/approve`
- AC: duyệt → hoàn tiền/đổi hàng theo phí `Return_Fee_Policy`; ghi audit.

### Epic QL-B — Danh mục: quyền nâng cao

**QL-06 — Quản lý nhóm sản phẩm**
- P1 · GD4 (tuần 6–7) · `GET /product-groups`, `POST /product-groups`, `PUT /product-groups/{id}`, `DELETE /product-groups/{id}`
- AC: tạo/sửa/xoá mềm nhóm; nhóm có sản phẩm không xoá được (409).

**QL-07 — Xoá mềm dữ liệu đối tác**
- P1 · GD4 · `DELETE /products/{id}`, `DELETE /suppliers/{id}`, `DELETE /customers/{id}`, `DELETE /receivers/{receiverId}`, `DELETE /suppliers/{id}/documents/{docId}`
- AC: mọi xoá là `is_deleted` (không xoá vật lý); bản ghi còn phát sinh giao dịch → 409; ghi audit.

**QL-08 — Xem người dùng, phân quyền & lịch sử hoạt động**
- P1 · GD3 (tuần 6–7) · `GET /identity/users` (lọc vai trò/trạng thái), `GET /identity/users/{userId}`, `GET /identity/audit-logs`
- AC: lọc theo vai trò/trạng thái; audit log hiện ai – làm gì – khi nào, phân trang; chỉ Quản lý/Quản trị (403 với vai trò khác).

### Epic QL-C — Báo cáo & cấu hình

**QL-09 — Báo cáo cho thuê & tình trạng trả hàng**
- P1 · GD9 (tuần 12–13) · `GET /reports/rental`
- AC: phiếu đang thuê/quá hạn/đã trả; tổng tiền thuê, phạt.

**QL-10 — Báo cáo nhập/xuất & công nợ (xem chung)**
- P1 · `GET /reports/import-export`, `GET /reports/receivables` → xem `KT-05`, `KT-06`.

**QL-11 — Quy tắc thuê (cảnh báo 7 ngày, ngưỡng phạt 10 ngày)**
- P1 · GD7 · `GET /config/rental-rules`
- AC: xem giá trị quy tắc đang áp dụng; **gap G-2:** chưa có API sửa — P2 thêm `PUT` (đề xuất mở rộng story này).

## 5. QUẢN TRỊ HỆ THỐNG (QT) — quản trị trong tenant

**QT-01 — Tạo, cập nhật, vô hiệu hoá người dùng & gán vai trò**
- P1 · GD3 (tuần 6–7) · `POST /identity/users`, `PUT /identity/users/{userId}`, `DELETE /identity/users/{userId}`
- AC:
  1. Chỉ Quản trị được tạo/sửa/vô hiệu hoá user (403 vai trò khác).
  2. `PUT` không có field `username` (theo `CRUD_ANALYSIS.md`) — username chỉ set lúc tạo.
  3. `DELETE` là xoá mềm/khoá tài khoản, không mất lịch sử giao dịch.
  4. Gán đúng 1 trong 5 vai trò; mọi thay đổi ghi audit.

**QT-02 — Ma trận phân quyền (RBAC)**
- P1 · GD3 · `GET /identity/roles`
- AC: trả 5 vai trò + danh sách quyền; dùng làm nguồn duy nhất cho FE render menu lẫn BE enforce.

**QT-03 — Theo dõi lịch sử hoạt động**
- P1 · GD3 · `GET /identity/audit-logs`
- AC: lọc theo người thực hiện, hành động, khoảng thời gian. *Dùng chung: Quản lý (`QL-08`).*

**QT-04 — Cấu hình mẫu VAT**
- P1: `GET /config/vat-options` (mọi vai trò) · P2: `POST /config/vat-options`, `PUT /config/vat-options/{id}` (Quản trị)
- AC: mẫu VAT áp cho phiếu xuất (`TC-EXP-02`); sửa mẫu không làm thay đổi phiếu đã phát sinh.

**QT-05 — Danh mục loại giao dịch & trạng thái thanh toán**
- P1 · `GET /config/transaction-types`, `GET /config/payment-statuses`
- AC: IMPORT, EXPORT, RENT, RETURN, WARRANTY, OTHER; đọc-only (dữ liệu chuẩn).

**QT-06 — Trang admin thống kê truy cập (G-4)**
- P2 · tuần 1 chốt phạm vi · FE + BE (chưa có endpoint)
- AC (đề xuất): thống kê người dùng, thông tin hiển thị theo từng trang, lượt truy cập theo tháng (NOTE UXUI #9).

## 6. ADMIN VẬN HÀNH (OPS) — công ty vận hành SaaS (tách khỏi QT)

**OPS-01 — Tự động provisioning tenant**
- P1 · How_To_Sale.md §2.1 · Control Plane
- AC: khách mới → tự tạo database, chạy migration khởi tạo schema (Users, Product_Groups, Products, Customers, Transaction_Types, VAT_Options, Payment_Statuses, Inventory + views), nạp dữ liệu mặc định.

**OPS-02 — Quản lý danh sách tenant & gói dịch vụ**
- P1 · Control Plane · `GET /tenant/*` (nội bộ)
- AC: lưu công ty thuê app, gói dịch vụ, trạng thái active/suspended/trial hết hạn; tách biệt khỏi KH mua hàng trong nghiệp vụ kho.

**OPS-03 — Bảo mật kết nối DB của tenant**
- P1 · How_To_Sale.md §2.1 · Control Plane
- AC: thông tin kết nối DB mã hoá bằng Vault/AWS Secrets Manager; không log ra console; xoay khoá được.

**OPS-04 — Migration hàng loạt schema**
- P1 · GD11 · Flyway/Liquibase
- AC: 1 thay đổi schema → chạy lần lượt qua từng DB tenant; rollback được; báo cáo DB nào fail.

**OPS-05 — Backup & khôi phục**
- P1 (chạy song song từ tuần 11) · GD11 · `POST /system/backup`, `GET /system/backup-status` (P2)
- AC: backup tự động DB + file đính kèm; restore về thời điểm bất kỳ thành công, dữ liệu khớp (TC-BAK-01, việc #50–51).

**OPS-06 — CI/CD & môi trường**
- P1 · GD11 (tuần 12–13) · Deployment_Guide.md
- AC: pipeline build Flutter web + JAR → Docker image → registry → deploy staging/production; unit/API test chạy trước khi merge; domain/SSL cấu hình cho UAT.

**OPS-07 — License Server (gói On-Premise)**
- P2 · How_To_Sale.md §3.2 · `GET /tenant/license`
- AC: backend on-premise gọi về mỗi 24h; hết hạn → read-only, **không xoá dữ liệu**; biến môi trường `LICENSE_SERVER_URL`, `TENANT_ID`.

**OPS-08 — Đóng gói On-Premise Enterprise**
- P2 · Deployment_Guide.md §3–4 · `docker-compose.yml` + `init.sql` + `.env`
- AC: khách chạy `docker compose up -d` là hệ thống lên ngay; có script/hướng dẫn backup kèm; phát hành qua `docker compose pull && up -d`.

## 7. DEV — nền tảng kỹ thuật (không gắn vai trò nghiệp vụ)

| ID | Việc | Ưu tiên | GĐ / Tuần | Deliverable |
|---|---|---|---|---|
| DEV-01 | Chốt kiến trúc Flutter + Spring Boot + MySQL database-per-tenant | P1 | GD1 / 2 | Architecture decision |
| DEV-02 | Thiết kế ERD & mapping schema theo `tigernet_erd_final.png` | P1 | GD1 / 2–3 | ERD + schema draft |
| DEV-03 | Quy chuẩn code, Git flow, môi trường Dev/Test | P1 | GD1 / 2–3 | Development guideline |
| DEV-04 | UI Dashboard/đăng nhập/SP/nhập/xuất/báo cáo (baseline) | P1 | GD2 / 1–4 | UI screens baseline |
| DEV-05 | Khởi tạo Flutter project, kiến trúc module | P1 | GD2 / 2–3 | Flutter skeleton |
| DEV-06 | Khởi tạo Backend: auth, catalog, inventory, import, export, rental, stocktake, report | P1 | GD2 / 3–4 | API + DB skeleton |
| DEV-07 | Logging, cấu hình môi trường, quản lý secret | P1 | GD2 / 4 | Base configuration |
| DEV-08 | API contract & xử lý lỗi chuẩn hóa (mã lỗi, format JSON) | P1 | GD2 / 4–5 | API contract |
| DEV-09 | Fix 5 mục NOTE UXUI còn lại (#8, #9, #12, #13, #14) | P1/P2 | GD2–GD10 | UX log cập nhật |
| DEV-10 | Responsive Desktop/Tablet/Mobile + dark/light (NFR-4) | P1 | GD10 / 13 | UI test report |
| DEV-11 | Kiểm thử hiệu năng, bảo mật, cách ly multi-tenant | P1 | GD10 / 12–13 | Perf + security report |
| DEV-12 | Tài liệu hướng dẫn sử dụng, kỹ thuật, vận hành | P1 | GD12 / 13 | User + technical docs |

---

# PHẦN 3 — TRUY VẾN (TRACEABILITY)

## 3.1 Map 112 endpoint → story

| Nhóm API (sheet API & Testing) | STT | Story |
|---|---|---|
| A. Xác thực, người dùng & phân quyền | 1–6 | PUB-02, PUB-03, CHUNG-02, PUB-04 |
| A. (me, users, roles, audit) | 7–14 | CHUNG-01, QT-01, QL-08, QT-02, QT-03 |
| B. Sản phẩm & nhóm sản phẩm | 15–21 | TK-01, TK-04 |
| B. Nhóm sản phẩm | 22–25 | QL-06 |
| C. Tồn kho | 26–29 | TK-02, TK-15 |
| D. Nhà cung cấp | 30–34 | TK-03, QL-07 |
| D. Bank & documents (P2) | 35–40 | KT-08, TK-04, QL-07 |
| E. Khách hàng & người nhận | 41–49 | BH-01, BH-02, QL-07 |
| F. Nhập kho | 50–56 | TK-05 → TK-09, QL-02, KT-01 |
| G. Xuất kho & bán hàng | 57–66 | TK-10, TK-11, BH-03 → BH-06, QL-01, KT-03, KT-04 |
| H. Cho thuê, mượn, hoàn trả | 67–76 | BH-07 → BH-10, TK-12 |
| H. Sales returns & fee policy (P2) | 77–82 | BH-11, BH-12, QL-05 |
| I. Kiểm kê & điều chỉnh | 83–91 | TK-13, TK-14, QL-03, QL-04, KT-07 |
| J. Dashboard, báo cáo, cảnh báo | 92–101 | CHUNG-03, CHUNG-04, CHUNG-05, TK-15, KT-05, KT-06, QL-09 |
| K. Cấu hình & hệ thống | 102–112 | QT-04, QT-05, QL-11, PUB-05, PUB-06, OPS-07, OPS-05 |

## 3.1.1 Kết quả đối soát (bản rà soát)

- **112/112 endpoint đã có story nhận** — P1: 93 endpoint, P2: 19 endpoint.
- **20 endpoint list/detail từng không có story nhận** → đã bổ sung vào story chủ quản:

| STT | Endpoint bổ sung | Story nhận |
|---|---|---|
| 15, 17 | `GET /products`, `GET /products/{id}` | TK-01 |
| 22 | `GET /product-groups` | QL-06 |
| 28 | `GET /stock-summary` | TK-02 |
| 30, 32 | `GET /suppliers`, `GET /suppliers/{id}` | TK-03 |
| 38 | `GET /suppliers/{id}/documents` | TK-04 |
| 40 | `DELETE /suppliers/{id}/documents/{docId}` | QL-07 |
| 41, 43 | `GET /customers`, `GET /customers/{id}` | BH-01 |
| 46, 49 | `GET /customers/{id}/receivers`, `DELETE /receivers/{receiverId}` | BH-02 |
| 59 | `GET /export-receipts` | BH-06 |
| 69 | `GET /rental-receipts/{id}` | BH-09 |
| 75, 76 | `GET /return-receipts`, `GET /return-receipts/{id}` | TK-12 |
| 81, 82 | `GET /return-fee-policies`, `PUT /return-fee-policies/{id}` | BH-11 |
| 84, 85 | `GET /stocktakes`, `GET /stocktakes/{id}` | TK-13 |

- **4 tham chiếu chéo sai đã sửa:** `KT-08` và `BH-02` trỏ `QL-10` (báo cáo) → đúng `QL-07` (xoá mềm); `BH-12` trỏ `QL-07` → đúng `QL-05` (duyệt trả hàng bán); mục `G-2` trỏ `QL-09` → đúng `QL-11` (quy tắc thuê).
- **Endpoint ngoài 112:** `POST /identity/auth/register` (chưa chốt — G-1), `PUT /config/rental-rules` (P2 — G-2), endpoint trang admin thống kê (P2 — G-4).

## 3.2 Story → test case trọng tâm

| Story | Test case |
|---|---|
| PUB-02 | TC-AUTH-01, TC-SEC-01 |
| PUB-03 | TC-AUTH-02 |
| QT-02 / mọi story có quyền | TC-RBAC-01, TC-SEC-01 |
| TK-01 | TC-PROD-01 |
| TK-05, TK-07 | TC-IMP-01 |
| TK-06 | TC-IMP-02 |
| TK-10 | TC-EXP-01 |
| KT-03, BH-03 | TC-EXP-02 |
| QL-01 | TC-EXP-03 |
| BH-07 | TC-RENT-01 |
| TK-12 | TC-RENT-02 |
| BH-09 | TC-RENT-03 |
| TK-13, QL-03 | TC-STK-01, TC-STK-02 |
| TK-02 / CHUNG-04 | TC-ALERT-01 |
| CHUNG-05 | TC-REP-01 |
| OPS-01…03, database-per-tenant | TC-TENANT-01 |
| DEV-11 | TC-PERF-01 |
| OPS-05 | TC-BAK-01 |

> Các story còn lại (CRUD danh mục, FE/UI, OPS-04/06/07/08, DEV-01→DEV-09) không có TC riêng trong sheet *API & Testing* → dùng ca kiểm thử chung: validation 400, RBAC 403 (`TC-RBAC-01`), ghi `Audit_Logs`, CI xanh.

## 3.3 Tổng kết backlog

| Nhóm | Số story | P1 | P2 | Story tính P2 |
|---|---|---|---|---|
| PUB & CHUNG | 12 | 11 | 1 | PUB-05 |
| Thủ kho (TK) | 15 | 14 | 1 | TK-04 |
| Kế toán (KT) | 8 | 7 | 1 | KT-08 |
| Bán hàng (BH) | 12 | 10 | 2 | BH-11, BH-12 |
| Quản lý (QL) | 11 | 10 | 1 | QL-05 |
| Quản trị hệ thống (QT) | 6 | 5 | 1 | QT-06 |
| Admin vận hành (OPS) | 8 | 6 | 2 | OPS-07, OPS-08 |
| DEV (nền tảng) | 12 | 11 | 1 | DEV-09 (P1/P2) |
| **Tổng** | **84** | **74** | **10** | |

> **Ghi chú tính điểm:** `PUB-04` và `DEV-09` đang tính vào P1. Nếu chốt bỏ self-register (G-1) thì `PUB-04` chuyển sang P2 → **P1 = 73, P2 = 11**.

**Exit criteria của backlog này:** 112 endpoint đều có story nhận (xem 3.1 – 3.1.1) · mọi story P1 có AC · 20 test case trọng tâm đối chiếu đủ 11 nhóm test (3.2) · 7 gap G-1→G-7 được chốt trong tuần 1 (28/09–04/10/2026).

---

# PHẦN 4 — GÓI PHÁT HÀNH THEO TUẦN (MILESTONE)

> Cắt 84 story thành 6 lát theo đúng lịch GD1→GD12 trong `Kế hoạch TigerNet.xlsx`. M1–M5 là Phase 1 (nghiệm thu 27/12/2026); M6 là Phase 2, **không chặn nghiệm thu**.

| Milestone | Tuần | Giai đoạn | Nội dung (story) | Exit condition |
|---|---|---|---|---|
| **M1 — Nền tảng & xác thực** | 1–5 | GD1–GD3 | `PUB-01`, `PUB-02`, `PUB-04` (chốt G-1), `PUB-06`, `CHUNG-01`, `CHUNG-02`, `CHUNG-06`, `DEV-01`→`DEV-08` | Chốt 7 gap; đăng nhập JWT + refresh + khóa 15’ sau 5 lần sai; menu render theo vai trò qua `/identity/me`; API contract + format lỗi chuẩn hoá; `/system/health` xanh; **khoá 112 endpoint** |
| **M2 — Danh mục, phân quyền & quản trị** | 6–8 | GD3–GD4 | `PUB-03`, `QT-01`, `QT-02`, `QT-03`, `QL-06`, `QL-07`, `QL-08`, `TK-01`, `TK-02`, `TK-03`, `KT-02`, `BH-01`, `BH-02` | CRUD SP/nhóm/NCC/KH/người nhận + serial; tồn kho & cảnh báo low-stock; màn hình user/role/audit-log; `TC-PROD-01`, `TC-AUTH-02`, `TC-RBAC-01`, `TC-SEC-01` xanh |
| **M3 — Nhập & xuất kho** | 8–10 | GD5–GD6 | `TK-05`→`TK-11`, `KT-01`, `KT-03`, `KT-04`, `BH-03`→`BH-06`, `QL-01`, `QL-02` | Phiếu nhập/xuất đủ luồng xác nhận-huỷ-in; tồn tăng/trừ đúng; VAT/công nợ tính đúng; `TC-IMP-01`, `TC-IMP-02`, `TC-EXP-01`, `TC-EXP-02`, `TC-EXP-03` xanh |
| **M4 — Cho thuê, kiểm kê & điều chỉnh** | 10–12 | GD7–GD8 | `BH-07`→`BH-10`, `TK-12`, `TK-13`, `TK-14`, `QL-03`, `QL-04`, `KT-07`, `QL-11` | Serial chuyển "đang thuê"; cảnh báo trước 7 ngày; phạt ≥10 ngày đúng công thức; kiểm kê → phiếu điều chỉnh → duyệt; `TC-RENT-01/02/03`, `TC-STK-01/02` xanh |
| **M5 — Dashboard, báo cáo & nghiệm thu** | 11–13 | GD9–GD12 | `CHUNG-03`, `CHUNG-04`, `CHUNG-05`, `TK-15`, `KT-05`, `KT-06`, `QL-09`, `QL-10`, `QT-04`, `QT-05`, `DEV-10`, `DEV-11`, `DEV-12`, `OPS-01`→`OPS-06` | Dashboard + 4 nhóm báo cáo + xuất Excel/PDF; trung tâm cảnh báo 3 loại; backup/restore chạy được; perf + security + cách ly tenant đạt; UAT 5 vai trò sign-off **27/12/2026** |
| **M6 — Phase 2** | Sau nghiệm thu | — | `PUB-05`, `TK-04`, `KT-08`, `BH-11`, `BH-12`, `QL-05`, `QT-06`, `OPS-07`, `OPS-08`, `DEV-09` (+ `PUB-04` nếu giữ, `PUT /config/rental-rules` của `QL-11`) | Branding tenant, tài liệu/TK ngân hàng NCC, trả hàng bán + phí hoàn, trang admin thống kê, License Server, đóng gói On-Premise |

**Ràng buộc thứ tự:** M2 chặn M3 (không có danh mục + tồn thì không nhập/xuất) · M3 chặn M4 (không có phiếu xuất thì không có luồng thuê/trả) · M4 chặn M5 (báo cáo cần dữ liệu nghiệp vụ) · `OPS-01`→`OPS-06` chạy **song song từ tuần 11** (backup + CI/CD không phụ thuộc chức năng).

---

# PHẦN 5 — QUY ƯỚC HOÀN THÀNH

## 5.1 Definition of Ready (kéo story vào tuần làm việc)

1. Endpoint, method, query param đã chốt theo sheet *API & Testing* (112 endpoint khoá ở M1).
2. AC ghi rõ điều kiện đạt; có test case đối chiếu (TC riêng hoặc ca kiểm thử chung ở 3.2).
3. Schema/ERD tương ứng đã map (xem `tigernet_erd_final (1).png`).
4. Story Phase 2 chỉ mở khi story P1 cùng chuỗi đã done.

## 5.2 Definition of Done (story "Hoàn thành")

1. Đủ AC, chạy tay trên staging đúng kết quả.
2. Unit test service layer coverage ≥70%, 0 test fail (`TC` nhóm Unit — Nhựt).
3. API test: happy case + validation 400 + phân quyền 403 đúng chỗ.
4. Mọi thao tác ghi/bấm quan trọng đều ghi `Audit_Logs` (NFR-5).
5. API contract & mã lỗi cập nhật (theo `DEV-08`); response JSON đúng format lỗi chuẩn hoá.
6. CI xanh: lint + unit + API test trước khi merge (`DEV-03` Git flow).
7. FE: đúng NOTE UXUI tương ứng, responsive Desktop/Tablet/Mobile, dark/light (NFR-4).

## 5.3 Checklist chốt ở tuần 1 (28/09–04/10/2026)

- [ ] G-1 — self-register: bỏ hay bổ sung `POST /identity/auth/register`
- [ ] G-2 — giá trị thuê (7 ngày / 10 ngày phạt) hard-code P1
- [ ] G-3 — bảo hành/hư hỏng gộp qua `POST /return-receipts` + `Transaction_Types`
- [ ] G-4 — trang admin thống kê (NOTE UXUI #9) xác nhận P2
- [ ] G-5 — 5 mục NOTE UXUI chưa fixed gán rõ story + tuần
- [ ] G-6 — xoá/bỏ dùng `Kế hoạch TigerNet.backup.xlsx`
- [ ] G-7 — điều chỉnh tải tuần 11–13 (kéo perf/security về tuần 11–12)
- [ ] Duyệt milestone M1–M6 và thứ tự chặn ở Phần 4

