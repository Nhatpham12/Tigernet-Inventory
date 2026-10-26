# TIGERNET — PHÂN TÍCH YÊU CẦU CỐT LÕI & BACKLOG THEO VAI TRÒ

Kết quả của **GD1 — "Phân rã yêu cầu thành backlog/use case cho từng vai trò"** (Kế hoạch chi tiết, việc #2, tuần 1–2).

- **Nguồn:** `Kế hoạch TigerNet.xlsx` (Tổng Quan, Kế hoạch chi tiết — 57 việc, NOTE UXUI — 14 mục, API & Testing — 112 endpoint / 11 nhóm test / 20 test case), `tigernet_erd_v1_2.drawio.png` + `Database_Design_v7.md` (ERD v1.2 — đa kho, thanh toán, bảo hành, **nguồn chuẩn duy nhất về dữ liệu**), `database/tenant/*.sql` (Flyway V1–V4), `database/control-plane/*.sql`, `How_To_Sale.md`, `Deployment_Guide.md`, `BIEU_MAU.md` (biểu mẫu in).
- **Loại bỏ:** `Kế hoạch TigerNet.backup.xlsx` (bản cũ, NOTE UXUI còn nội dung dự án weather-map) — đã xoá khỏi repo.
- **Ưu tiên:** **P1** = Phase 1 (bắt buộc trong 3 tháng / 13 tuần), **P2** = Phase 2 (mở rộng, không chặn nghiệm thu 27/12/2026).
- **Mã story:** `PUB` (chưa đăng nhập) · `CHUNG` (mọi vai trò) · `TK` (Thủ kho) · `KT` (Kế toán) · `BH` (Bán hàng) · `QL` (Quản lý) · `QT` (Quản trị hệ thống) · `OPS` (Admin vận hành SaaS) · `DEV` (nền tảng kỹ thuật).
- **Cấu trúc:** Phần 1 yêu cầu cốt lõi · Phần 2 backlog 90 story theo vai trò · Phần 3 truy vết 112 endpoint → story → test case · Phần 4 milestone M1–M6 · Phần 5 quy ước DoR/DoD & checklist tuần 1.
- **Bản rà soát (tuần 1):** đối soát 112/112 endpoint có story nhận (bổ sung 20 endpoint, sửa 4 tham chiếu chéo sai), chuẩn hoá bảng tổng kết 74 P1 / 10 P2, thêm G-7 (tải lịch tuần 12–13).
- **Bản rà soát 2 (29/09/2026):** nhân sự thực tế 1 người (§1.5); thêm gap G-8→G-12 (cho mượn, endpoint in biểu mẫu, ERD↔SQL, Control Plane P1 tối thiểu, tài liệu thiếu); bổ sung cho mượn (`rentalType=RENT|LOAN`, nay là `LEND`) vào BH-07/08/09, TK-12, QL-09; thêm `RENTAL_OVERDUE` vào CHUNG-04; đổi tham chiếu bảng tra cứu cũ (`Transaction_Types`, `Payment_Statuses`, `VAT_Options`, `Return_Fee_Policy`) sang enum/bảng theo SQL; thêm 3 story in biểu mẫu `TK-16`, `KT-09`, `BH-13` → tổng 87 story (77 P1 / 10 P2).
- **Bản rà soát 3 (06/10/2026) — chuyển sang ERD v6:** ERD v6 + `Database_Design_v6.md` thay cho schema SQL cũ (đảo lại G-10), `database/tenant/` viết lại theo v6; loại phiếu mượn đổi `LOAN` → **`LEND`**; mã vai trò Thủ kho là `WAREHOUSE`; khóa đăng nhập giữ 15 phút (sửa thiết kế v6 cho khớp TC-AUTH-01); Đa kho: thêm `CHUNG-07`, `QL-12`, `QT-07` (P1) và `TK-17` chuyển kho (P2). Giữ đúng ERD (không thêm bảng thanh toán): `KT-03` chỉ cập nhật đã trả/còn lại, phiếu thu/chi tách sang `KT-10` (P2); điều chỉnh tồn luôn sinh từ kiểm kê nên gộp `TK-14` vào `TK-13`, `QL-04` vào `QL-03`. Thêm gap G-13→G-15. Tổng **90 story (78 P1 / 12 P2)**.

---

# PHẦN 1 — PHÂN TÍCH YÊU CẦU CỐT LÕI

## 1.1 Bối cảnh & mục tiêu doanh nghiệp

TigerNet là phần mềm **quản lý kho kiểu SaaS đa khách hàng** (mỗi khách hàng 1 database riêng) kèm gói **On-Premise Enterprise** bán riêng. Bốn yêu cầu cốt lõi rút ra từ toàn bộ tài liệu:

1. **Cách ly dữ liệu tuyệt đối giữa các tenant** — giá nhập, NCC, công nợ là dữ liệu nhạy cảm → `Database-per-tenant` + kiểm thử TC-TENANT-01 (0 rò rỉ chéo).
2. **Vận hành tập trung** — provisioning, migration hàng loạt, backup, phát hành version làm 1 lần cho tất cả tenant (Control Plane).
3. **Đủ nghiệp vụ kho hàng, nhiều kho** — danh mục → nhập → xuất/bán → cho thuê–mượn–trả → kiểm kê → báo cáo/cảnh báo; mỗi phiếu gắn đúng một kho, tồn theo dõi theo từng kho (ERD v6).
4. **Phân trách rõ theo 5 vai trò, có đối soát** — mỗi hành động ghi `Audit_Logs` ("ai – làm gì – khi nào").

## 1.2 Phạm vi Phase 1 / Phase 2

| Hạng mục            | Phase 1 (P1)                                                                                                    | Phase 2 (P2)                                                  |
| ------------------- | --------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------- |
| Xác thực            | JWT + refresh, bcrypt, OTP quên mật khẩu, khóa 15’ sau 5 lần sai                                                | —                                                             |
| Danh mục            | SP, nhóm SP, NCC, KH, người nhận, **kho + phân công nhân viên theo kho**, tồn kho theo kho, serial              | Ảnh SP, tài liệu NCC, TK ngân hàng NCC                        |
| Nhập kho            | Tạo/sửa/xác nhận/huỷ/in phiếu, cộng tồn, audit                                                                  | —                                                             |
| Xuất & bán          | Kiểm tra tồn tại kho xuất, phiếu xuất 3 trạng thái, VAT, ghi nhận đã trả/còn lại                                | Phiếu thu/chi, lịch sử thanh toán (G-13)                      |
| Cho thuê & cho mượn | Phiếu thuê/mượn (`RENT`/`LEND`), cảnh báo trước 7 ngày, trả + phạt quá hạn ≥10 ngày (thuê và mượn)              | Trả hàng bán + `Return_Fee_Policy`                            |
| Kiểm kê & kho       | Phiếu kiểm kê theo kho → chênh lệch → Quản lý chốt → tự sinh phiếu điều chỉnh                                   | Chuyển kho (`Stock_Transfers`)                                |
| Biểu mẫu in         | 8 biểu mẫu chứng từ PDF/Excel (xem `BIEU_MAU.md`)                                                               | Phiếu thu/chi, phiếu trả/đổi hàng bán, bảo hành, chuyển kho   |
| Báo cáo             | Dashboard, tồn / nhập-xuất / cho thuê / công nợ, Excel/PDF                                                      | Xuất XML/Word                                                 |
| Hệ thống            | Alerts, VAT/Payment statuses/Rental-rules (đọc), health                                                         | Branding tenant, license, backup API, CRUD mẫu VAT            |
| Kiến trúc           | SaaS multi-tenant, Control Plane tối thiểu (`tenants` + `tenant_databases` + routing datasource), Flyway, CI/CD | `tenant_branding`, `licenses`, License Server, gói On-Premise |

## 1.3 Định nghĩa 5 vai trò (RBAC)

| Vai trò               | Trách nhiệm chính                | Quyền tinh hoa                                                                           |
| --------------------- | -------------------------------- | ---------------------------------------------------------------------------------------- |
| **Thủ kho**           | Hàng vào/ra kho, serial, kiểm kê | Tạo & xác nhận phiếu nhập, hoàn tất xuất, nhập số liệu kiểm kê (cả kiểm kê đột xuất để điều chỉnh) — chỉ trong kho được phân công |
| **Kế toán**           | Tiền, công nợ, đối soát          | Ghi nhận đã thu/còn nợ trên phiếu xuất, xem & in phiếu, báo cáo nhập-xuất/công nợ mọi kho, TK ngân hàng NCC (P2) |
| **Bán hàng**          | Khách hàng, chào bán, cho thuê   | CRUD khách/người nhận, tạo phiếu xuất & phiếu thuê/mượn, theo dõi hạn thuê — chỉ trong kho được phân công |
| **Quản lý**           | Duyệt & kiểm soát                | Duyệt phiếu nhập/xuất, chốt kiểm kê (= duyệt điều chỉnh), quản lý danh mục kho, toàn bộ báo cáo mọi kho, giám sát người dùng & audit log |
| **Quản trị hệ thống** | Quản trị trong tenant            | CRUD người dùng, gán vai trò & phân công kho, danh mục kho, mẫu VAT, cấu hình hệ thống |

**Admin vận hành (tách riêng, không thuộc 5 vai trò):** người của công ty vận hành SaaS — quản lý tenant, secret kết nối DB, backup/restore, migration hàng loạt, CI/CD, License Server. Một số chức năng (license, backup) hiển thị cho `Quản trị hệ thống` trong giao diện tenant, nhưng _quyền sở hữu vận hành_ thuộc nhóm `OPS`.

## 1.4 Yêu cầu phi chức năng (NFR)

| #     | NFR            | Ngưỡng kiểm thử                                                                                                          |
| ----- | -------------- | ------------------------------------------------------------------------------------------------------------------------ |
| NFR-1 | Bảo mật        | bcrypt cho mật khẩu, JWT refresh, OTP 10’, khóa 15’ sau 5 lần sai (TC-AUTH-01/02), OWASP Top 10, 0 lỗ hổng Critical/High |
| NFR-2 | Cách ly tenant | Đọc/ghi chéo tenant → 403/404 (TC-TENANT-01)                                                                             |
| NFR-3 | Hiệu năng      | Trang chính ≤2.5s, API p95 ≤500ms, 10.000 giao dịch/ngày (TC-PERF-01)                                                    |
| NFR-4 | Giao diện      | Responsive Desktop/Tablet/Mobile, dark/light, đúng 14 mục NOTE UXUI, font Geist                                          |
| NFR-5 | Truy vết       | Mọi thao tác ghi/bấm quan trọng → `Audit_Logs`                                                                           |
| NFR-6 | Backup         | Backup tự động DB + file, restore về thời điểm bất kỳ (TC-BAK-01)                                                        |
| NFR-7 | Khả dụng       | `/system/health` cho liveness/readiness, CI chạy xanh, 100% endpoint có test case                                        |

## 1.5 Ràng buộc & giả định

- **Nhân sự:** **1 người** phụ trách toàn bộ DB, FE, BE, Testing, Deploy; 13 tuần (28/09/2026 → 27/12/2026). _(Bản trước ghi 2 người — không đúng thực tế.)_ Hệ quả: các việc ghi "Cả 2" trong kế hoạch chạy tuần tự, không song song; rủi ro tải xem G-7.
- **Stack:** Flutter Web (1 build duy nhất, white-labeling theo subdomain), Spring Boot (nhóm API xác thực/người dùng đặt dưới tiền tố `/identity`), MySQL 8 database-per-tenant theo ERD v6, Redis (khóa đăng nhập, rate limit, refresh token, OTP).
- **Giả định:** thứ tự GD1→GD12 trong kế hoạch giữ nguyên; backlog dưới đây bám 112 endpoint của sheet _API & Testing_.

## 1.6 Gap & rủi ro phát hiện khi review (cần chốt ở tuần 1)

| #    | Gap                                                                                                                                                                                                                                                                                                                                                                                | Đề xuất                                                                                                                                                                                                                                                                               |
| ---- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| G-1  | NOTE UXUI #11 ghi "trang **đăng ký**" nhưng 112 endpoint **không có** `POST /auth/register` — chỉ có `POST /users` (Quản trị)                                                                                                                                                                                                                                                      | Chốt: hoặc bỏ self-register (khuyến nghị, đúng mô hình B2B), hoặc bổ sung endpoint ở P1 → story `PUB-04`                                                                                                                                                                              |
| G-2  | `GET /config/rental-rules` (xem quy tắc 7 ngày / 10 ngày phạt) **không có API sửa**                                                                                                                                                                                                                                                                                                | Giữ giá trị hard-code P1; thêm `PUT` ở P2 → `QL-11`                                                                                                                                                                                                                                   |
| G-3  | GD7 có việc "trả bảo hành & hàng hư hỏng" nhưng không có endpoint riêng | Dùng `POST /return-receipts`; hàng hư/mất chuyển `Inventory.Status = DAMAGED`, tình trạng ghi `Return_Receipt_Details.Condition_Note` (ERD v6 không có bảng biến động tồn) → gộp vào `TK-12` |
| G-4  | NOTE UXUI #9 (trang admin: thống kê hiển thị & lượt truy cập theo tháng) chưa có endpoint                                                                                                                                                                                                                                                                                          | Đưa sang P2 → `QT-06`                                                                                                                                                                                                                                                                 |
| G-5  | 5/14 mục NOTE UXUI chưa fixed (#8 alert pop-up, #9, #12 giới hạn menu, #13 filter/sort, #14 toast)                                                                                                                                                                                                                                                                                 | Dành story `PUB-01`, `CHUNG-04`, `CHUNG-06`, `QT-06`                                                                                                                                                                                                                                  |
| G-6  | File backup chứa nội dung dự án khác (weather-map)                                                                                                                                                                                                                                                                                                                                 | **Đã xử lý:** xoá khỏi repo                                                                                                                                                                                                                                                           |
| G-7  | Tuần 12–13 dồn cùng lúc GD10 (tích hợp/perf/security) + GD11 (backup/CI-CD/UAT env) + GD12 (UAT/docs/nghiệm thu) với **1 người**                                                                                                                                                                                                                                                   | Kéo perf + security sang tuần 11–12, chốt UAT env ở tuần 12; giữ tuần 13 cho UAT, fix lỗi và bàn giao; nếu trễ, cắt UI polish (DEV-10) và OPS-04 nâng cao trước, giữ nguyên luồng nghiệp vụ                                                                                           |
| G-8  | **Cho mượn** (việc #33; ERD v6 `Rental_Receipts.Rental_Type = 'LEND'`) không có story/endpoint riêng — backlog chỉ nói "thuê" | Dùng chung `/rental-receipts` với `rentalType=RENT\|LEND`, lọc `?type=LEND`; bổ sung AC vào BH-07/08/09, TK-12, QL-09. **Đã chốt:** LEND không tiền thuê/cọc, nhưng **quá hạn tính phạt như RENT** (≥10 ngày, số ngày quá hạn × giá thuê/ngày); hư hỏng/mất ghi nhận trên phiếu trả cho cả hai (không có cột tiền bồi thường — G-14) |
| G-9  | 112 endpoint chỉ có API in cho phiếu nhập & phiếu xuất; 7 biểu mẫu P1 khác chưa có endpoint                                                                                                                                                                                                                                                                                        | Thêm endpoint in (xem 3.1.1) + story `TK-16`, `KT-09`, `BH-13`; danh sách biểu mẫu ở `BIEU_MAU.md`                                                                                                                                                                                    |
| G-10 | Ảnh ERD cũ (`tigernet_erd_final (1).png`), SQL cũ (`partners`, `stock_balances`, `payments`…) và thiết kế v6 lệch nhau | **Đã chốt lại 06/10/2026: ERD v6 (`TIGERNET_ERD_v6_dark.png`, `Database_Design_v6.md`) là nguồn chuẩn.** `database/tenant/` viết lại theo v6 (V1 bảng, V2 danh mục, V3 view); backlog đổi tham chiếu sang tên bảng v6; ERD cũ, SQL cũ và `Database_Design_v5.md` không dùng nữa |
| G-11 | `V1__init_control_plane.sql` ghi "Phase 2" nhưng OPS-01…03 là P1                                                                                                                                                                                                                                                                                                                   | **Đã chốt:** P1 tối thiểu = `tenants` + `tenant_databases` + routing datasource; `tenant_branding`, `licenses` sang P2                                                                                                                                                                |
| G-12 | Tài liệu được tham chiếu nhưng không có trong repo: `Database_Design.docx`, SRS | Bổ sung SRS vào repo hoặc chấp nhận chỉ trích dẫn (§) trong `Database_Design_v7.md` |
| G-13 | ERD v6/v7 không có bảng thanh toán: phiếu xuất chỉ lưu tổng `Paid`/`Remain`, phiếu nhập không có cột đã trả | **Đã giải quyết bởi ERD v1.2:** bảng `Payments` (mỗi lần thu/chi một dòng, `Direction` IN/OUT, `Reference_Type`/`Reference_ID`); `Import_Receipts` thêm `VAT_ID`, `Payment_Status_ID`, `Paid`, `Remain`. `KT-03` cập nhật `Paid`/`Remain`/`Payment_Status_ID` và **ghi 1 dòng `Payments`** mỗi lần thu (vẫn ghi `Audit_Logs` `PAYMENT`). API/in phiếu thu-chi, công nợ phải trả NCC → `KT-10` (P2, bảng đã có); biên bản đối chiếu (`KT-09`) vẫn đối chiếu phải thu theo phiếu ở P1 |
| G-14 | ERD v6/v7 không có: bước duyệt phiếu thuê, tiền bồi thường/hoàn cọc trên phiếu trả, trạng thái duyệt phiếu điều chỉnh, trạng thái hủy phiếu xuất/nhập, bảng cấu hình, bảng đã-đọc cảnh báo | **ERD v1.2 đã bổ sung:** `Rental_Receipts.Status/Approved_By/Approved_At` (mặc định `APPROVED`, tự duyệt khi lập — bước duyệt của Quản lý là P2); `Return_Receipts.Compensation_Amount/Deposit_Refund`; trạng thái `CANCELLED` + `Cancelled_By/At/Reason` cho phiếu nhập/xuất (hủy = giữ phiếu, không xóa). **Vẫn giữ:** điều chỉnh qua chốt kiểm kê (gộp `TK-14`→`TK-13`, `QL-04`→`QL-03`, bỏ 2 endpoint); quy tắc thuê/khóa đăng nhập ở `application.yaml`; cảnh báo dùng cờ chung `Is_Resolved` |
| G-15 | ERD v6 có đa kho (`Warehouses`, `User_Warehouses`, `Stock_Transfers`) nhưng backlog chưa có story | **Đã chốt:** kho + phân công + lọc theo kho là P1 (`CHUNG-07`, `QL-12`, `QT-07`); chuyển kho P2 (`TK-17`); mọi API danh sách/báo cáo nhận `warehouseId`; endpoint kho nằm ngoài 112 → thêm vào sheet _API & Testing_ |
| G-16 | ERD v7 (16/10/2026) thêm `Warehouse_Locations`, `Product_Warehouse_Settings`, `Stock_Movements`, `Inventory.Version`, bước duyệt chuyển kho; nhưng thiếu một số cột schema đang cần (`Warehouses.Phone/Manager_ID`, `User_Warehouses.Is_Default/Assigned_At`, `Stock_Transfers.Received_By`, `Stock_Transfer_Details.Inventory_ID/Serial`, `Alerts.Warehouse_ID`, `Inventory.Import_Detail_ID/Location_ID`, `Stock_Movements.Reference_ID`) | **Đã chốt 16/10/2026:** ERD v7 là chuẩn; `database/tenant/` và `Database_Design_v7.md` cập nhật theo; các cột trên **giữ lại** và cần vẽ bổ sung vào ERD (danh sách ở cuối `Database_Design_v7.md`). `Stock_Movements`, `Product_Warehouse_Settings` là P1 (`TK-02`, `TK-07`); `Warehouse_Locations` và bước duyệt chuyển kho là P2 (`TK-17`). ERD v6, `Database_Design_v5/v6.md`, `Database_Script_v6.sql` đã xóa |
| G-17 | ERD v1.2 (26/10/2026) thêm `Payments`, `Supplier_Returns` (+`Supplier_Return_Details`), `Warranty_Claims` và các cột hủy/duyệt/bồi thường/VAT nhập; đồng thời vẽ đủ 9 cột còn thiếu ở G-16 | **Đã chốt 26/10/2026:** ERD v1.2 là chuẩn; migration `database/tenant/V4__erd_v1_2.sql`, `Database_Design_v7.md` (mục 1.24–1.27, R71–R89) và entity JPA cập nhật theo. `Payments` ghi từ phiếu xuất ở P1; `Supplier_Returns`, `Warranty_Claims` đã có bảng + entity, API và màn hình là P2 (`BH-*`/`TK-*` bổ sung khi làm) |

> **Đã xử lý trong bản rà soát này:** 20 endpoint list/detail chưa có story nhận và 4 tham chiếu chéo sai (`KT-08`, `BH-02` → `QL-10` phải là `QL-07`; `BH-12` → `QL-07` phải là `QL-05`; `G-2` → `QL-09` phải là `QL-11`) — xem 3.1.1.

---

# PHẦN 2 — BACKLOG THEO VAI TRÒ

> Mỗi story: **Ưu tiên · Giai đoạn/GĐ + tuần · Endpoint · AC (tiêu chí chấp nhận)**. Story đánh dấu _dùng chung_ có trong một vai trò chính, vai trò còn lại chỉ tham chiếu.

## 0. PUBLIC & CHUNG — khách chưa đăng nhập / mọi vai trò

### Epic PUB — Trang công khai & phiên đăng nhập

**PUB-01 — Giới hạn nội dung theo trạng thái đăng nhập**

- P1 · GD2 (tuần 1–4) · FE · Endpoint: — (NOTE UXUI #12)
- _Với tư cách khách chưa đăng nhập, tôi chỉ muốn xem được trang công khai để hệ thống không lộ dữ liệu nghiệp vụ._
- AC:
  1. Chưa đăng nhập: chỉ thấy trang chủ công khai, trang đăng nhập, quên mật khẩu.
  2. Menu, bảng dữ liệu, dashboard bị ẩn/redirect về `/login` khi truy cập URL trực tiếp.
  3. Sau đăng nhập: menu hiển thị đúng theo vai trò và danh sách kho trả về từ `/identity/me`.

**PUB-02 — Đăng nhập & phiên token**

- P1 · GD3 (tuần 4–5) · BE · `POST /identity/auth/login`, `POST /identity/auth/refresh`, `POST /identity/auth/logout`
- _Với tư cách người dùng, tôi muốn đăng nhập để truy cập hệ thống theo vai trò của mình._
- AC:
  1. Đăng nhập đúng trả access + refresh token; sai mật khẩu trả 401.
  2. Sai mật khẩu 5 lần liên tiếp → khóa tạm 15 phút, trả 429/423 (TC-AUTH-01); đếm ở Redis `login_fail:{username}`.
  3. Refresh token lưu Redis `refresh:{user_id}:{token_id}`; hết hạn → 401; logout xóa key (thu hồi).
  4. Mật khẩu lưu dạng bcrypt, không bao giờ trả plaintext.

**PUB-03 — Quên & đặt lại mật khẩu bằng OTP email**

- P1 · GD3 (tuần 5–6) · BE · `POST /identity/auth/forgot-password`, `POST /identity/auth/reset-password`
- AC:
  1. Nhập email → gửi OTP; không tiết lộ email có tồn tại hay không (response thống nhất).
  2. OTP lưu Redis `otp:{email}` (dạng hash), hiệu lực 10 phút, sai/hết hạn → 400 (TC-AUTH-02).
  3. Đặt lại mật khẩu thành công → hash bcrypt, thu hồi toàn bộ session cũ.

**PUB-04 — Đăng ký tài khoản (CẦN CHỐT — G-1)**

- P1/P2 · tuần 1 chốt · `POST /identity/auth/register` (chưa có trong 112 endpoint)
- _Nếu chốt bỏ self-register:_ Quản trị hệ thống tạo user qua `POST /identity/users` → dời toàn bộ sang `QT-01`.

**PUB-05 — White-labeling theo tenant**

- P2 · GD2 · `GET /tenant/branding`
- AC: trả logo + theme màu theo subdomain; không thấy tenant → theme mặc định; không lộ thông tin kết nối DB.

**PUB-06 — Health check**

- P1 · GD2 (tuần 4) · `GET /system/health`
- AC: trả 200 khi DB kết nối OK; dùng cho Docker/K8s probe.

### Epic CHUNG — mọi vai trò đã đăng nhập

**CHUNG-01 — Hồ sơ & phân quyền hiện hành**

- P1 · GD3 · `GET /identity/me`, `GET /identity/roles`
- AC: trả thông tin user + vai trò + RBAC matrix + danh sách kho được phân công và kho mặc định (`User_Warehouses`); FE dùng để render menu và bộ chọn kho (ghép `PUB-01`, `CHUNG-07`).

**CHUNG-02 — Đổi mật khẩu**

- P1 · GD3 · `POST /identity/auth/change-password`
- AC: yêu cầu mật khẩu cũ; sai → 400; thành công → bcrypt hash mới.

**CHUNG-03 — Dashboard tổng quan**

- P1 · GD9 (tuần 11–12) · `GET /dashboard/summary`, `GET /dashboard/charts`
- AC: hiển thị tồn kho theo kho đang chọn hoặc toàn công ty (`View_Stock_By_Warehouse`, `View_Stock_Company_Total`), số phiếu chờ duyệt, doanh thu ngày; biểu đồ nhập/xuất theo kỳ; biểu đồ đặt đầu trang, co giãn theo màn hình (NOTE UXUI #5).

**CHUNG-04 — Trung tâm cảnh báo**

- P1 · GD9 · `GET /alerts`, `GET /alerts/unread-count`, `PUT /alerts/{id}/read`
- AC: loại cảnh báo theo `Alerts.Alert_Type`: `LOW_STOCK` (theo kho), `RENT_DUE` (sắp đến hạn trước 7 ngày và quá hạn; cả RENT lẫn LEND), `PENDING_APPROVAL`, `DEBT_OVERDUE`; `TRANSFER_PENDING` ở P2. Chỉ thấy cảnh báo của kho được phân công (`Alerts.Warehouse_ID`; NULL = toàn công ty). Badge đếm cảnh báo chưa xử lý (`Is_Resolved = FALSE`) trên Navbar, icon nổi bật; popup mỗi lần vào trang (NOTE UXUI #8); bấm đã đọc → `Is_Resolved = TRUE` (cờ chung cho mọi người — G-14), badge giảm.

**CHUNG-05 — Xuất báo cáo ra file**

- P1 · GD9 (tuần 13) · `GET /reports/export?format=excel|pdf`
- AC: file mở được, số liệu khớp DB (TC-REP-01); Phase 2 mở rộng `xml|word`.

**CHUNG-06 — Bộ giao diện bảng dữ liệu & thông báo chuẩn**

- P1 · GD2/GD10 · FE · (NOTE UXUI #1, #13, #14)
- AC: bảng cỡ chữ lớn, kẻ dòng rõ, phân trang 20–50 dòng; filter/sort theo cột, header sticky khi cuộn; toast lỗi/thành công tiếng Việt; font Geist thống nhất, tiêu đề in hoa (NOTE UXUI #2, #10).

**CHUNG-07 — Chọn kho làm việc & phạm vi dữ liệu theo kho (G-15)**

- P1 · GD3 (tuần 5–6) · FE + BE · `GET /identity/me` (trả danh sách kho), tham số `warehouseId` trên các API danh sách/báo cáo
- _Với tư cách nhân viên kho, tôi muốn chọn kho đang làm việc để chỉ thấy và thao tác hàng, phiếu của kho đó._
- AC:
  1. AppShell có bộ chọn kho, mặc định kho `Is_Default`; Quản lý, Quản trị, Kế toán có thêm lựa chọn "Tất cả kho".
  2. Mọi màn hình danh sách, dashboard, báo cáo, cảnh báo lọc theo kho đang chọn.
  3. Backend chặn ở tầng service (`WarehouseScopeGuard`): Thủ kho/Bán hàng đọc/ghi kho ngoài phân công → 403, kể cả khi gọi API trực tiếp (TC-RBAC-01).
  4. Mỗi phiếu gắn đúng một kho; đơn cần hàng ở nhiều kho thì tách phiếu (hoặc chuyển kho trước — P2).

---

## 1. THỦ KHO (TK) — hàng vào/ra, serial, kiểm kê

### Epic TK-A — Danh mục & tồn kho

**TK-01 — Thêm/sửa sản phẩm & quản lý serial**

- P1 · GD4 (tuần 6–7) · `GET /products`, `POST /products`, `GET /products/{id}`, `PUT /products/{id}`, `GET /products/{id}/serials`
- _Với tư cách Thủ kho, tôi muốn thêm và sửa sản phẩm kèm giá nhập/ra, mức tồn tối thiểu để nhập hàng đúng giá._
- AC:
  1. Thiếu tên / giá âm → 400, không ghi DB (TC-PROD-01).
  2. Sản phẩm serial bật cờ `Is_Serialized`; không serial thì bỏ qua bước nhập serial.
  3. `GET .../serials` trả trạng thái từng serial (`Inventory.Status`: IN_STOCK / RENTED / LENT / SOLD / DAMAGED) kèm kho đang giữ.
  4. Mọi thay đổi ghi `Audit_Logs`.

**TK-02 — Xem tồn kho & cảnh báo dưới ngưỡng**

- P1 · GD4 (tuần 7) · `GET /inventory`, `GET /inventory/{id}`, `GET /stock-summary`, `GET /inventory/low-stock`; **[v7, ngoài 112 endpoint]** `GET /stock-settings`, `PUT /stock-settings` (ngưỡng theo kho), `GET /stock-movements` (thẻ kho)
- AC: lọc theo kho (mặc định kho đang chọn), sản phẩm, serial, trạng thái, ngày nhập/ra; `GET /stock-summary` trả `Stock_Summary` theo (SP, kho) và tổng toàn công ty; dòng dưới ngưỡng (`Product_Warehouse_Settings.Min_Stock`, không có thì lấy `Products.Min_Stock_Level`) hiện cờ đỏ và sinh alert LOW_STOCK của kho đó (ghép `CHUNG-04`); dòng chạm `Reorder_Point` hiện gợi ý nhập/chuyển bổ sung. Quản lý/Thủ kho đặt Min/Max/Reorder theo (SP, kho). `GET /stock-movements` lọc theo kho, SP, khoảng ngày — hiện từng biến động kèm phiếu nguồn và `Balance_After`.
- _Dùng chung: Kế toán (xem), Quản lý (xem)._

**TK-03 — CRUD nhà cung cấp**

- P1 · GD4 (tuần 7–8) · `GET /suppliers`, `POST /suppliers`, `GET /suppliers/{id}`, `PUT /suppliers/{id}`
- AC: NCC = `Parties` + hồ sơ vai trò `Suppliers` (một đối tác có thể vừa là NCC vừa là KH); search/phân trang; validate SĐT/email, MST không trùng; xem lịch sử nhập theo NCC (`Import_Receipts.Source_Party_ID`).

**TK-04 — Tài liệu NCC & ảnh sản phẩm**

- P2 · `GET /suppliers/{id}/documents`, `POST /suppliers/{id}/documents`, `POST /products/{id}/image` (xoá tài liệu thuộc `QL-07`)
- AC: upload multipart lên object storage, DB lưu `Supplier_Documents.File_Path` / `Products.Image_Path`; giới hạn dung lượng & định dạng; chỉ Thủ kho/Quản lý.

### Epic TK-B — Nhập kho (GD5)

**TK-05 — Tạo phiếu nhập**

- P1 · GD5 (tuần 8–9) · `POST /import-receipts`
- AC:
  1. Chọn kho nhận (kho được phân công) và nguồn nhập `Source_Party_ID` (NCC, khách bán lẻ có CCCD hoặc khách đổi/trả); thêm nhiều dòng SP: số lượng, giá, serial (bắt buộc với SP serial), mã vận đơn.
  2. Tự tính `Line_Total` và `Total_Amount`.
  3. Thiếu kho/nguồn nhập/SP, giá âm → 400, không ghi DB.
  4. Phiếu sinh ra ở trạng thái `DRAFT`; "Lưu & gửi duyệt" chuyển `PENDING`; số phiếu `PN-{Warehouse_Code}-{yyyy}-{0001}`; ghi audit người tạo.

**TK-06 — Sửa phiếu nhập chưa xác nhận**

- P1 · GD5 (tuần 9) · `PUT /import-receipts/{id}`
- AC: chỉ sửa được phiếu `DRAFT`/`PENDING`; phiếu `CONFIRMED` → 409 (TC-IMP-02); nếu cho sửa thì tồn kho được ánh xạ ngược chính xác.

**TK-07 — Xác nhận phiếu nhập → cộng tồn**

- P1 · GD5 (tuần 8–9) · `PUT /import-receipts/{id}/confirm`
- AC: `DRAFT`/`PENDING` → `CONFIRMED`; sinh dòng `Inventory` tại kho của phiếu (serial: mỗi serial một dòng, trùng serial toàn hệ thống → 409; không serial: một dòng/lô với `Quantity_On_Hand`), cộng `Stock_Summary` (SP, kho) và ghi `Stock_Movements` (IN) trong cùng transaction (TC-IMP-01); tồn vượt ngưỡng thì đóng cảnh báo LOW_STOCK đang mở; ghi audit.

**TK-08 — Huỷ phiếu nhập chưa xác nhận**

- P1 · GD5 · `DELETE /import-receipts/{id}`
- AC: chỉ huỷ được phiếu `DRAFT`/`PENDING`; huỷ = chuyển `CANCELLED`, ghi `Cancelled_By`, `Cancelled_At`, `Cancel_Reason` (`DELETE /import-receipts/{id}?reason=`), phiếu vẫn tra cứu được, không thể sửa; ghi `Audit_Logs` (`CANCEL`); không ảnh hưởng tồn.

**TK-09 — In/xuất phiếu nhập**

- P1 · GD5 (tuần 9) · `GET /import-receipts/{id}/print?format=excel|pdf`
- AC: file mở được, tổng tiền khớp phiếu.

### Epic TK-C — Xuất kho: khâu thực hiện

**TK-10 — Kiểm tra tồn realtime trước khi xuất**

- P1 · GD6 (tuần 9–10) · `GET /stock-check`
- AC: trả tồn khả dụng **tại kho chỉ định** (`warehouseId` bắt buộc) theo productIds/serials — chỉ tính `Inventory.Status = IN_STOCK` của kho đó; vượt tồn → 409, không trừ tồn (TC-EXP-01). _Dùng chung: Bán hàng._

**TK-11 — Hoàn tất xuất kho (trừ tồn thực tế)**

- P1 · GD6 (tuần 10) · `PUT /export-receipts/{id}/complete`
- AC: chỉ từ `CONFIRMED` → `EXPORTED`; serial chuyển `SOLD`, hàng không serial trừ `Quantity_On_Hand`, cập nhật `Stock_Summary` của kho xuất; sai thứ tự trạng thái → 409 (TC-EXP-03); ghi audit. _Dùng chung: Quản lý._

### Epic TK-D — Cho thuê & trả hàng

**TK-12 — Trả hàng thuê/mượn / bảo hành / hàng hư hỏng + tính phạt**

- P1 · GD7 (tuần 11–12) · `PUT /rental-receipts/{id}/return`, `POST /return-receipts`, `GET /return-receipts`, `GET /return-receipts/{id}`
- AC:
  1. Lập phiếu trả (`Return_Receipts` + `Return_Receipt_Details`) gắn phiếu thuê/mượn gốc; kho nhận lại có thể khác kho cho thuê (`Return_Receipts.Warehouse_ID`). Cho phép trả từng phần: dòng thuê được trả đánh dấu `Is_Returned`; trả đủ mọi dòng → phiếu gốc `RETURNED` + `Return_Date`.
  2. Hàng trả về tốt → `IN_STOCK` tại kho nhận; hư/mất → `DAMAGED` (ghi `Condition_Note`, mất ghi thêm `Inventory.Note`); hàng bảo hành/hư hỏng cũng đi luồng này (đóng G-3).
  3. Phiếu **thuê/mượn** (`RENT`/`LEND`) quá hạn ≥ 10 ngày (`tigernet.rental.penalty-min-overdue-days`): tính `Days_Overdue`, phạt = số ngày quá hạn × `Rental_Price_Per_Day` (TC-RENT-02) — **cả hai loại đều tính phạt** (G-8).
  4. Bồi thường hư hỏng/mất và hoàn cọc ghi vào `Return_Receipts.Compensation_Amount` / `Deposit_Refund` (G-14, ERD v1.2); tình trạng hàng vẫn ghi ở `Condition_Note`.
  5. Ghi audit; `Stock_Summary` cập nhật ngay (kho cho thuê giảm `Qty_Rented`, kho nhận tăng `Qty_On_Hand`/`Qty_Damaged`). _Dùng chung: Bán hàng._

### Epic TK-E — Kiểm kê & điều chỉnh (GD8)

**TK-13 — Lập phiếu kiểm kê & nhập số liệu thực tế**

- P1 · GD8 (tuần 11) · `POST /stocktakes`, `GET /stocktakes`, `GET /stocktakes/{id}`, `PUT /stocktakes/{id}/items`
- AC: mỗi phiếu kiểm kê thuộc đúng một kho; mặc định liệt kê toàn bộ SP của kho, hoặc chỉ các SP được chọn (**kiểm kê đột xuất** — thay cho phiếu điều chỉnh thủ công); `System_Qty` lấy theo tồn của kho; nhập SL thực tế → tự tính `Difference`; ghi lý do chênh lệch từng SP; SL âm/lớn bất thường → chặn, yêu cầu nhập lại (TC-STK-02).

**TK-14 — ~~Tạo phiếu điều chỉnh thủ công~~ (đã gộp vào TK-13 — G-14)**

- ERD v6: `Stock_Adjustments.Stocktake_ID` bắt buộc, không có trạng thái duyệt riêng. Điều chỉnh đột xuất = lập phiếu kiểm kê cho các SP cần chỉnh (`TK-13`), Quản lý chốt (`QL-03`). Endpoint `POST /stock-adjustments` bỏ. Không tính vào tổng story.

**TK-15 — Xem báo cáo tồn kho**

- P1 · GD9 (tuần 12) · `GET /reports/inventory`
- AC: báo cáo theo thời điểm/danh mục/sản phẩm/kho (từng kho hoặc toàn công ty). _Dùng chung: Kế toán, Quản lý._

### Epic TK-F — Biểu mẫu in (G-9)

**TK-16 — In chứng từ kho: phiếu thuê/mượn, biên bản trả, kiểm kê, điều chỉnh**

- P1 · GD7–GD8 (tuần 11–12) · `GET /rental-receipts/{id}/print`, `GET /return-receipts/{id}/print`, `GET /stocktakes/{id}/print`, `GET /stocktakes/{id}/print?template=adjustment` (`?format=excel|pdf`, ngoài 112 endpoint)
- AC:
  1. Đúng bố cục biểu mẫu 4–7 trong `BIEU_MAU.md`; tiêu đề đổi theo `Rental_Type` (PHIẾU CHO THUÊ / PHIẾU CHO MƯỢN); đầu phiếu có mã kho + địa chỉ kho.
  2. Người nhận lấy từ snapshot trên phiếu, khớp DB; phiếu mượn ẩn giá/tiền thuê.
  3. Phiếu kiểm kê `DRAFT` in dấu "BẢN NHÁP"; mỗi lần in ghi audit (`PRINT`).
  4. File mở được ở cả PDF và Excel (TC-REP-01 áp dụng tương tự). _Dùng chung: Bán hàng (mẫu 4, 5), Quản lý (mẫu 6, 7)._

### Epic TK-G — Chuyển kho (P2)

**TK-17 — Chuyển hàng giữa các kho (G-15)**

- P2 · `GET /stock-transfers`, `POST /stock-transfers`, `GET /stock-transfers/{id}`, `PUT /stock-transfers/{id}`, `PUT /stock-transfers/{id}/approve` **[v7]**, `PUT /stock-transfers/{id}/ship`, `PUT /stock-transfers/{id}/receive`, `DELETE /stock-transfers/{id}`, `GET /stock-transfers/{id}/print` (ngoài 112 endpoint)
- AC:
  1. `DRAFT` → `APPROVED` **[v7]** (Quản lý duyệt): ghi `Approved_By`, `Approved_At`; chưa động vào tồn.
  2. `APPROVED` → `IN_TRANSIT` (kho nguồn xác nhận xuất, ghi `Shipped_At`): đủ tồn `IN_STOCK` tại kho nguồn; serial chuyển `IN_TRANSIT`, hàng không serial trừ ở kho nguồn; kho đích tăng `Qty_In_Transit`; ghi `Stock_Movements` OUT ở kho nguồn.
  3. `IN_TRANSIT` → `RECEIVED` (kho đích xác nhận, ghi `Received_By`, `Received_At`): nhập `Qty_Received` từng dòng; hàng sang kho đích `IN_STOCK`; ghi `Stock_Movements` IN ở kho đích; thiếu hụt ghi `DAMAGED` + cảnh báo `TRANSFER_PENDING`.
  4. Hủy (`CANCELLED`) chỉ khi `DRAFT`/`APPROVED`; kho nguồn ≠ kho đích; người lập/người nhận phải được phân công kho tương ứng; in phiếu theo biểu mẫu 11; ghi audit.

## 2. KẾ TOÁN (KT) — tiền, công nợ, đối soát

**KT-01 — Xem & in phiếu nhập**

- P1 · GD5 (tuần 9) · `GET /import-receipts` (lọc NCC, ngày, trạng thái), `GET /import-receipts/{id}`, `GET /import-receipts/{id}/print`
- AC: lọc theo NCC/kỳ; in excel|pdf khớp dữ liệu phiếu. _Lịch sử nhập theo NCC phục vụ đối soát._

**KT-02 — Xem tồn kho phục vụ đối soát**

- P1 · GD4 · `GET /inventory`, `GET /reports/inventory`
- AC: chỉ đọc, không chỉnh sửa tồn. _Dùng chung: Thủ kho, Quản lý._

**KT-03 — Ghi nhận thanh toán & công nợ**

- P1 · GD6 (tuần 10) · `POST /export-receipts/{id}/payments`
- AC:
  1. Nhập số tiền thu → cộng vào `Export_Receipts.Paid`, tính lại `Remain` = Tổng tiền (đã gồm VAT) − Đã trả, cập nhật `Payment_Status_ID` (UNPAID / PARTIAL / PAID) (TC-EXP-02).
  2. Không cho số tiền vượt `Remain`; ghi sai thì ghi một lần điều chỉnh âm kèm lý do (không sửa số cũ, `Paid` không âm).
  3. Mỗi lần thu ghi 1 dòng `Payments` (`Direction = IN`, `Reference_Type = Export_Receipts`, số phiếu `THU-{kho}-{năm}-{số}`, `Method` CASH/TRANSFER) và 1 dòng `Audit_Logs` (`Action = PAYMENT`) — `Payments` là lịch sử thanh toán (G-13); in phiếu thu/chi → `KT-10` (P2).

**KT-04 — In phiếu xuất**

- P1 · GD6 · `GET /export-receipts/{id}/print?format=excel|pdf`
- AC: số liệu khớp phiếu, hiển thị VAT & công nợ còn lại.

**KT-05 — Báo cáo nhập/xuất theo NCC, khách hàng, sản phẩm**

- P1 · GD9 (tuần 12) · `GET /reports/import-export?groupBy=supplier|customer|product`
- AC: tổng hợp theo kỳ; số khớp chi tiết phiếu. _Dùng chung: Quản lý._

**KT-06 — Báo cáo công nợ, tiền bán/thuê chưa thu**

- P1 · GD9 (tuần 12–13) · `GET /reports/receivables`
- AC: liệt kê KH còn công nợ, phân biệt bán/thuê; dữ liệu nguồn: `Remain`/`Payment_Status_ID` của phiếu xuất; `Total_Rent`, `Penalty_Amount`, `Deposit`, `Payment_Status_ID` của phiếu thuê; không có công nợ phải trả NCC (G-13); lọc theo kho. _Dùng chung: Quản lý._ Lưu ý: Thủ kho gọi endpoint này phải 403 (TC-RBAC-01).

**KT-07 — Xem phiếu điều chỉnh tồn**

- P1 · GD8 · `GET /stock-adjustments`, `GET /stock-adjustments/{id}`
- AC: xem các dòng điều chỉnh sinh từ phiếu kiểm kê đã chốt (`Stock_Adjustments`: SP, `Adjust_Qty`, `Reason`, kho theo phiếu kiểm kê) để hạch toán; chỉ đọc (chốt thuộc `QL-03`).

**KT-08 — Quản lý tài khoản ngân hàng của NCC**

- P2 · `GET /suppliers/{id}/bank-accounts`, `POST /suppliers/{id}/bank-accounts`, `DELETE /suppliers/{id}/bank-accounts/{bankId}`
- AC: nhiều TK/NCC; xoá chỉ bởi Quản lý (xem `QL-07`).

**KT-09 — In biên bản đối chiếu công nợ (G-9)**

- P1 · GD9 (tuần 12–13) · `GET /reports/reconciliation?customerId=&from=&to=` (`?format=excel|pdf`, ngoài 112 endpoint)
- AC:
  1. Theo biểu mẫu 8 (`BIEU_MAU.md`): liệt kê phiếu xuất đã xuất và phiếu thuê/mượn của khách trong kỳ; mỗi phiếu: tổng tiền, đã trả, còn lại; tổng còn phải thu khớp `KT-06`.
  2. Số dư đầu kỳ và phát sinh theo từng lần thu lấy được từ `Payments` (G-13); P1 vẫn chỉ đối chiếu phải thu khách hàng, công nợ phải trả NCC sang `KT-10`.
  3. Chỉ Kế toán, Quản lý (403 vai trò khác); mỗi lần in ghi audit.

**KT-10 — Phiếu thu/chi & lịch sử thanh toán (P2 — G-13)**

- P2 · `GET /export-receipts/{id}/payments`, `GET /payments/{id}/print` (ngoài 112 endpoint)
- Điều kiện: bảng `Payments` đã có trong ERD v1.2; còn thiếu API đọc/in và luồng chi (phiếu nhập, hoàn cọc).
- AC: mỗi lần thu/chi là một chứng từ có mã; in phiếu thu/chi theo biểu mẫu 9 (`BIEU_MAU.md`); ghi sai thì huỷ, không xoá; theo dõi công nợ phải trả NCC theo phiếu nhập; biên bản đối chiếu có số dư đầu kỳ và phát sinh theo ngày.

## 3. BÁN HÀNG (BH) — khách hàng, xuất bán, cho thuê

### Epic BH-A — Khách hàng & người nhận

**BH-01 — CRUD khách hàng**

- P1 · GD4 (tuần 7–8) · `GET /customers`, `POST /customers`, `GET /customers/{id}`, `PUT /customers/{id}`
- AC: KH = `Parties` + hồ sơ vai trò `Customers` (cá nhân có CCCD / tổ chức có MST); search, phân trang; validate SĐT/email.

**BH-02 — CRUD người nhận hàng**

- P1 · GD4 · `GET /customers/{id}/receivers`, `POST /customers/{id}/receivers`, `PUT /receivers/{receiverId}`, `DELETE /receivers/{receiverId}`
- AC: 1 KH nhiều người nhận (`Receivers`); xoá mềm `Is_Deleted` (xoá bởi Quản lý → `QL-07`).

### Epic BH-B — Xuất kho & bán hàng (GD6)

**BH-03 — Tạo phiếu xuất khi bán hàng**

- P1 · GD6 (tuần 9–10) · `POST /export-receipts`
- AC: chọn kho xuất (kho được phân công), KH/người nhận (chụp tên/địa chỉ/SĐT vào `Receiver_*_Snapshot`), dòng SP + serial (serial phải đang ở kho xuất); chọn mức VAT (`VAT_ID` → `VAT_Options`); bảo hành theo tháng (`Warranty_Months` → `Warranty_Expiry`); trạng thái khởi tạo `PENDING`; gọi `stock-check` trước (→ `TK-10`).

**BH-04 — Sửa / huỷ phiếu xuất chưa xác nhận**

- P1 · GD6 (tuần 10) · `PUT /export-receipts/{id}`, `DELETE /export-receipts/{id}`
- AC: chỉ tác động được phiếu `PENDING`; huỷ = chuyển `CANCELLED` + `Cancelled_By/At/Reason` (`DELETE /export-receipts/{id}?reason=`), ghi audit `CANCEL` (G-14); đã xác nhận → 409; không thu thêm tiền được vào phiếu đã hủy.

**BH-05 — In phiếu xuất**

- P1 · GD6 · `GET /export-receipts/{id}/print?format=excel|pdf`
- AC: bản in đủ KH, SP, VAT, đã trả/còn lại. _Dùng chung: Kế toán._

**BH-06 — Duyệt & luồng trạng thái phiếu xuất** _(vai trò chính: Quản lý)_

- P1 · `PUT /export-receipts/{id}/confirm` → xem `QL-01`. Bán hàng theo dõi trạng thái qua `GET /export-receipts` (lọc trạng thái/KH/ngày) và `GET /export-receipts/{id}`.

### Epic BH-C — Cho thuê & cho mượn (GD7)

> Thuê và mượn dùng chung bảng `Rental_Receipts`, phân biệt bằng `Rental_Type`: `RENT` = cho thuê, `LEND` = cho mượn (G-8). Phiếu có `Status` + `Approved_By/At` (ERD v1.2); hiện tự duyệt khi lập nên có hiệu lực ngay, bước duyệt của Quản lý (`PENDING` → `APPROVED`/`REJECTED`) là P2 (G-14).

**BH-07 — Tạo phiếu cho thuê / cho mượn**

- P1 · GD7 (tuần 10–11) · `POST /rental-receipts` (body có `rentalType=RENT|LEND`)
- AC:
  1. Chọn kho, serial còn tồn tại kho đó (TC-RENT-01), hạn trả `Due_Date`; **RENT:** giá thuê/ngày, tiền cọc; **LEND:** nhập giá thuê/ngày (chỉ làm cơ sở tính phạt quá hạn, **không tính vào `Total_Rent`**), không cọc.
  2. Khi lưu phiếu: serial chuyển `RENTED` (thuê) / `LENT` (mượn), loại khỏi tồn khả dụng ngay; `Payment_Status = RENTING`.
  3. RENT: `Total_Rent` tính theo số ngày; LEND: `Total_Rent = 0`; cả hai: `Due_Date` > `Date_Out`.
  4. Ghi audit.

**BH-08 — Sửa / huỷ phiếu thuê/mượn chưa xác nhận**

- P1 · GD7 · `PUT /rental-receipts/{id}`, `DELETE /rental-receipts/{id}`
- AC: chỉ sửa/huỷ được khi chưa có dòng nào được trả; huỷ → `Payment_Status = CANCELLED`, hàng về `IN_STOCK`; không cho đổi `Rental_Type` sau khi tạo.

**BH-09 — Theo dõi hàng đang thuê/mượn & hạn trả**

- P1 · GD7 (tuần 11) · `GET /rental-receipts` (lọc `type=RENT|LEND`, kho, trạng thái, KH, hạn trả), `GET /rental-receipts/{id}`, `GET /rental-receipts/expiring?days=7`
- AC: danh sách phiếu thuê và phiếu mượn tách tab/lọc riêng; còn 7 ngày hoặc đã quá hạn → sinh `RENT_DUE` (TC-RENT-03), áp dụng cả thuê lẫn mượn, hiện ở `CHUNG-04`.

**BH-10 — Trả hàng thuê/mượn & hoàn cọc** _(dùng chung)_

- P1 · xem `TK-12` — Bán hàng cùng quyền gọi `PUT /rental-receipts/{id}/return`.

### Epic BH-D — Trả hàng bán (P2)

**BH-11 — Trả hàng / đổi khách & phí hoàn trả**

- P2 · `POST /sales-returns`, `GET /sales-returns`, `GET /sales-returns/{id}`, `GET /return-fee-policies`, `PUT /return-fee-policies/{id}` (PUT: chỉ Quản lý)
- AC: áp phí theo `Return_Fee_Policy` (bậc theo số ngày từ `Delivery_Date`, quá 30 ngày → từ chối); lưu `Original_Value`, `Fee_Percent`, `Fee_Amount`, `Refund_Amount`; kho tiếp nhận `Sales_Returns.Warehouse_ID`; trạng thái `DRAFT` chờ duyệt; in phiếu trả hàng/đổi hàng + phiếu bảo hành theo biểu mẫu 10 (`BIEU_MAU.md`, `GET /sales-returns/{id}/print`).

**BH-12 — Duyệt trả hàng bán** _(vai trò chính: Quản lý)_

- P2 · `PUT /sales-returns/{id}/approve` → xem `QL-05`.

### Epic BH-E — Biểu mẫu in (G-9)

**BH-13 — In phiếu giao hàng**

- P1 · GD6 (tuần 10) · `GET /export-receipts/{id}/print?template=delivery&format=pdf` (ngoài 112 endpoint)
- AC: theo biểu mẫu 3 (`BIEU_MAU.md`) — khổ A5, **không hiện giá**; đủ người nhận (snapshot), SĐT, địa chỉ, SP/serial/số lượng; ô ký người giao/người nhận; ghi audit. _Dùng chung: Thủ kho._

## 4. QUẢN LÝ (QL) — duyệt, kiểm soát, báo cáo

### Epic QL-A — Duyệt & phê quyết

**QL-01 — Duyệt phiếu xuất**

- P1 · GD6 (tuần 10) · `PUT /export-receipts/{id}/confirm`
- AC: `PENDING` → `CONFIRMED`; không nhảy trạng thái sai; Quản lý duyệt được phiếu của mọi kho; mọi bước ghi audit (TC-EXP-03).

**QL-02 — Duyệt / giám sát phiếu nhập**

- P1 · GD5 · `PUT /import-receipts/{id}/confirm`, `DELETE /import-receipts/{id}` (quyền Thủ kho + Quản lý)
- AC: xác nhận `PENDING` → `CONFIRMED` (cùng AC với `TK-07`); có thể giám sát, huỷ phiếu chưa xác nhận; ghi audit.

**QL-03 — Chốt kiểm kê → tự tạo phiếu điều chỉnh**

- P1 · GD8 (tuần 11) · `POST /stocktakes/{id}/approve`
- AC: chốt = duyệt (`DRAFT` → `COMPLETED`). SL thực tế ≠ SL hệ thống → tự sinh `Stock_Adjustments` (`Adjust_Qty` = `Difference`, `Reason`) và áp vào `Inventory`/`Stock_Summary` của kho kiểm kê trong cùng transaction, tồn khớp số thực tế (TC-STK-01); không chênh lệch → chốt không sinh dòng điều chỉnh; ghi audit.

**QL-04 — ~~Duyệt phiếu điều chỉnh tồn~~ (đã gộp vào QL-03 — G-14)**

- ERD v6: phiếu điều chỉnh không có trạng thái duyệt; duyệt = chốt phiếu kiểm kê (`QL-03`). Endpoint `PUT /stock-adjustments/{id}/approve` bỏ. Không tính vào tổng story.

**QL-05 — Duyệt trả hàng bán (P2)**

- P2 · `PUT /sales-returns/{id}/approve`
- AC: `DRAFT` → `CONFIRMED` → `COMPLETED` (hoặc `REJECTED`); hoàn tiền/đổi hàng theo `Return_Fee_Policy`, đổi hàng sinh phiếu xuất mới (`New_Export_Receipt_ID`); hàng nhập lại kho theo `Return_Condition`; ghi audit.

### Epic QL-B — Danh mục: quyền nâng cao

**QL-06 — Quản lý nhóm sản phẩm**

- P1 · GD4 (tuần 6–7) · `GET /product-groups`, `POST /product-groups`, `PUT /product-groups/{id}`, `DELETE /product-groups/{id}`
- AC: tạo/sửa/xoá mềm nhóm; nhóm có sản phẩm không xoá được (409).

**QL-07 — Xoá mềm dữ liệu đối tác**

- P1 · GD4 · `DELETE /products/{id}`, `DELETE /suppliers/{id}`, `DELETE /customers/{id}`, `DELETE /receivers/{receiverId}`, `DELETE /suppliers/{id}/documents/{docId}`
- AC: mọi xoá là `Is_Deleted` (không xoá vật lý); bản ghi còn phát sinh giao dịch → 409; ghi audit.

**QL-08 — Xem người dùng, phân quyền & lịch sử hoạt động**

- P1 · GD3 (tuần 6–7) · `GET /identity/users` (lọc vai trò/trạng thái), `GET /identity/users/{userId}`, `GET /identity/audit-logs`
- AC: lọc theo vai trò/trạng thái; audit log hiện ai – làm gì – khi nào, phân trang; chỉ Quản lý/Quản trị (403 với vai trò khác).

### Epic QL-C — Báo cáo & cấu hình

**QL-09 — Báo cáo cho thuê, cho mượn & tình trạng trả hàng**

- P1 · GD9 (tuần 12–13) · `GET /reports/rental` (lọc `type=RENT|LEND`, kho)
- AC: phiếu đang thuê/mượn, quá hạn, đã trả — **tách riêng thuê và mượn**; tổng tiền thuê, phạt (cả thuê và mượn); hàng trả về hư/mất liệt kê theo `Condition_Note`, kèm `Compensation_Amount` và `Deposit_Refund` của phiếu trả (G-14).

**QL-10 — Báo cáo nhập/xuất & công nợ (xem chung)**

- P1 · `GET /reports/import-export`, `GET /reports/receivables` → xem `KT-05`, `KT-06`.

**QL-11 — Quy tắc thuê (cảnh báo 7 ngày, ngưỡng phạt 10 ngày)**

- P1 · GD7 · `GET /config/rental-rules`
- AC: xem giá trị quy tắc đang áp dụng (đọc từ cấu hình ứng dụng `tigernet.rental.expiry-warning-days` = 7, `tigernet.rental.penalty-min-overdue-days` = 10 — ERD v6 không có bảng cấu hình); **gap G-2:** chưa có API sửa — P2 thêm `PUT` (cần bổ sung nơi lưu).

**QL-12 — Quản lý danh mục kho (G-15)**

- P1 · GD4 (tuần 6–7) · `GET /warehouses`, `POST /warehouses`, `GET /warehouses/{id}`, `PUT /warehouses/{id}`, `DELETE /warehouses/{id}` (ngoài 112 endpoint)
- _Với tư cách Quản lý, tôi muốn khai báo các kho ở các địa chỉ khác nhau để theo dõi tồn và chứng từ theo từng kho._
- AC:
  1. `Warehouse_Code` (tiền tố số phiếu, không sửa khi kho đã có phiếu) và `Warehouse_Name` không trùng; `Address` bắt buộc; chọn người phụ trách `Manager_ID`.
  2. `DELETE` = ngừng hoạt động (`Is_Active = FALSE`), không xoá cứng; kho còn tồn (`Stock_Summary` > 0) → 409; kho ngừng hoạt động không chọn được khi lập phiếu.
  3. Tenant mới có sẵn kho `MAIN` (seed V2). Quản trị hệ thống cùng quyền; mọi thay đổi ghi audit.

## 5. QUẢN TRỊ HỆ THỐNG (QT) — quản trị trong tenant

**QT-01 — Tạo, cập nhật, vô hiệu hoá người dùng & gán vai trò**

- P1 · GD3 (tuần 6–7) · `POST /identity/users`, `PUT /identity/users/{userId}`, `DELETE /identity/users/{userId}`
- AC:
  1. Chỉ Quản trị được tạo/sửa/vô hiệu hoá user (403 vai trò khác).
  2. `PUT` không có field `username` — username chỉ set lúc tạo.
  3. `DELETE` là xoá mềm/khoá tài khoản, không mất lịch sử giao dịch.
  4. Gán đúng 1 trong 5 vai trò (`WAREHOUSE`, `ACCOUNTANT`, `SALES`, `MANAGER`, `ADMIN`); mọi thay đổi ghi audit.

**QT-02 — Ma trận phân quyền (RBAC)**

- P1 · GD3 · `GET /identity/roles`
- AC: trả 5 vai trò + danh sách quyền; dùng làm nguồn duy nhất cho FE render menu lẫn BE enforce.

**QT-03 — Theo dõi lịch sử hoạt động**

- P1 · GD3 · `GET /identity/audit-logs`
- AC: lọc theo người thực hiện, hành động, khoảng thời gian. _Dùng chung: Quản lý (`QL-08`)._

**QT-04 — Cấu hình mẫu VAT**

- P1: `GET /config/vat-options` (mọi vai trò) · P2: `POST /config/vat-options`, `PUT /config/vat-options/{id}` (Quản trị)
- AC: mức VAT (`VAT_Options`: NO, 0%, 5%, 8%, 10%) áp cho phiếu xuất (`TC-EXP-02`); phiếu trỏ `VAT_ID` nên không cho sửa `Rate` của mức đã dùng — đổi thuế suất = thêm mức mới (G-14).

**QT-05 — Danh mục loại giao dịch & trạng thái thanh toán**

- P1 · `GET /config/transaction-types`, `GET /config/payment-statuses`
- AC: `transaction-types` trả **enum trong code** (ERD v6 không có bảng `Transaction_Types`): loại chứng từ (IMPORT, EXPORT, RENTAL, RETURN, SALES_RETURN, STOCKTAKE, TRANSFER) và `Rental_Type` (RENT, LEND); `payment-statuses` đọc bảng `Payment_Statuses` (PAID, UNPAID, PARTIAL, RENTING, RETURNED, CANCELLED); đọc-only.

**QT-06 — Trang admin thống kê truy cập (G-4)**

- P2 · tuần 1 chốt phạm vi · FE + BE (chưa có endpoint)
- AC (đề xuất): thống kê người dùng, thông tin hiển thị theo từng trang, lượt truy cập theo tháng (NOTE UXUI #9).

**QT-07 — Phân công nhân viên vào kho (G-15)**

- P1 · GD3–GD4 (tuần 6–7) · `GET /identity/users/{userId}/warehouses`, `PUT /identity/users/{userId}/warehouses` (ngoài 112 endpoint)
- AC:
  1. Gán một user vào nhiều kho (`User_Warehouses`), đúng một kho mặc định (`Is_Default`).
  2. Thủ kho và Bán hàng phải có ít nhất một kho mới thao tác được; Quản lý, Quản trị, Kế toán xem mọi kho, không cần phân công.
  3. Thay đổi có hiệu lực ở lần gọi API kế tiếp (không cần đăng nhập lại); ghi audit.

## 6. ADMIN VẬN HÀNH (OPS) — công ty vận hành SaaS (tách khỏi QT)

**OPS-01 — Tự động provisioning tenant**

- P1 · How_To_Sale.md §2.1 · Control Plane
- AC: khách mới → tự tạo database (`utf8mb4_unicode_ci`), chạy Flyway `database/tenant/`: V1 (bảng ERD v6) + V2 (Roles, Product_Groups, VAT_Options, Payment_Statuses, Return_Fee_Policy, kho `MAIN`) + V3 (view); tạo tài khoản Quản trị đầu tiên với mật khẩu ngẫu nhiên gửi qua email (không seed mật khẩu mặc định); ghi `tenant_operations` (PROVISION).

**OPS-02 — Quản lý danh sách tenant & gói dịch vụ**

- P1 (tối thiểu — G-11) · Control Plane · `GET /tenant/*` (nội bộ)
- AC: bảng `tenants`: công ty thuê app, `plan_code`, trạng thái PROVISIONING/TRIAL/ACTIVE/SUSPENDED/CANCELLED, `subdomain`; tách biệt khỏi KH mua hàng trong nghiệp vụ kho. `tenant_branding`, `licenses` để P2.

**OPS-03 — Bảo mật kết nối DB của tenant**

- P1 · How_To_Sale.md §2.1 · Control Plane
- AC: bảng `tenant_databases`, mật khẩu lưu `db_password_encrypted` (AES-GCM tầng ứng dụng hoặc Vault) kèm `encryption_key_id` để xoay khoá; backend định tuyến bằng `AbstractRoutingDataSource` có cache; không log ra console.

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

| ID     | Việc                                                                                  | Ưu tiên | GĐ / Tuần    | Deliverable            |
| ------ | ------------------------------------------------------------------------------------- | ------- | ------------ | ---------------------- |
| DEV-01 | Chốt kiến trúc Flutter + Spring Boot + MySQL database-per-tenant                      | P1      | GD1 / 2      | Architecture decision  |
| DEV-02 | Thiết kế ERD & schema: ERD v6 (`TIGERNET_ERD_v6_dark.png`) → `database/tenant/` — **xong 06/10/2026** | P1      | GD1 / 2–3    | ERD + schema           |
| DEV-03 | Quy chuẩn code, Git flow, môi trường Dev/Test                                         | P1      | GD1 / 2–3    | Development guideline  |
| DEV-04 | UI Dashboard/đăng nhập/SP/nhập/xuất/báo cáo (baseline)                                | P1      | GD2 / 1–4    | UI screens baseline    |
| DEV-05 | Khởi tạo Flutter project, kiến trúc module                                            | P1      | GD2 / 2–3    | Flutter skeleton       |
| DEV-06 | Khởi tạo Backend: auth, catalog, inventory, import, export, rental, stocktake, report | P1      | GD2 / 3–4    | API + DB skeleton      |
| DEV-07 | Logging, cấu hình môi trường, quản lý secret                                          | P1      | GD2 / 4      | Base configuration     |
| DEV-08 | API contract & xử lý lỗi chuẩn hóa (mã lỗi, format JSON)                              | P1      | GD2 / 4–5    | API contract           |
| DEV-09 | Fix 5 mục NOTE UXUI còn lại (#8, #9, #12, #13, #14)                                   | P1/P2   | GD2–GD10     | UX log cập nhật        |
| DEV-10 | Responsive Desktop/Tablet/Mobile + dark/light (NFR-4)                                 | P1      | GD10 / 13    | UI test report         |
| DEV-11 | Kiểm thử hiệu năng, bảo mật, cách ly multi-tenant                                     | P1      | GD10 / 12–13 | Perf + security report |
| DEV-12 | Tài liệu hướng dẫn sử dụng, kỹ thuật, vận hành                                        | P1      | GD12 / 13    | User + technical docs  |

---

# PHẦN 3 — TRUY VẾN (TRACEABILITY)

## 3.1 Map 112 endpoint → story

| Nhóm API (sheet API & Testing)       | STT     | Story                                                    |
| ------------------------------------ | ------- | -------------------------------------------------------- |
| A. Xác thực, người dùng & phân quyền | 1–6     | PUB-02, PUB-03, CHUNG-02, PUB-04                         |
| A. (me, users, roles, audit)         | 7–14    | CHUNG-01, QT-01, QL-08, QT-02, QT-03                     |
| B. Sản phẩm & nhóm sản phẩm          | 15–21   | TK-01, TK-04                                             |
| B. Nhóm sản phẩm                     | 22–25   | QL-06                                                    |
| C. Tồn kho                           | 26–29   | TK-02, TK-15                                             |
| D. Nhà cung cấp                      | 30–34   | TK-03, QL-07                                             |
| D. Bank & documents (P2)             | 35–40   | KT-08, TK-04, QL-07                                      |
| E. Khách hàng & người nhận           | 41–49   | BH-01, BH-02, QL-07                                      |
| F. Nhập kho                          | 50–56   | TK-05 → TK-09, QL-02, KT-01                              |
| G. Xuất kho & bán hàng               | 57–66   | TK-10, TK-11, BH-03 → BH-06, QL-01, KT-03, KT-04         |
| H. Cho thuê, mượn, hoàn trả          | 67–76   | BH-07 → BH-10, TK-12                                     |
| H. Sales returns & fee policy (P2)   | 77–82   | BH-11, BH-12, QL-05                                      |
| I. Kiểm kê & điều chỉnh              | 83–91   | TK-13, QL-03, KT-07 (TK-14, QL-04 đã gộp — 2 endpoint bỏ) |
| J. Dashboard, báo cáo, cảnh báo      | 92–101  | CHUNG-03, CHUNG-04, CHUNG-05, TK-15, KT-05, KT-06, QL-09 |
| K. Cấu hình & hệ thống               | 102–112 | QT-04, QT-05, QL-11, PUB-05, PUB-06, OPS-07, OPS-05      |

## 3.1.1 Kết quả đối soát (bản rà soát)

- **112/112 endpoint đã có story nhận** — P1: 93 endpoint, P2: 19 endpoint.
- **20 endpoint list/detail từng không có story nhận** → đã bổ sung vào story chủ quản:

| STT    | Endpoint bổ sung                                                  | Story nhận |
| ------ | ----------------------------------------------------------------- | ---------- |
| 15, 17 | `GET /products`, `GET /products/{id}`                             | TK-01      |
| 22     | `GET /product-groups`                                             | QL-06      |
| 28     | `GET /stock-summary`                                              | TK-02      |
| 30, 32 | `GET /suppliers`, `GET /suppliers/{id}`                           | TK-03      |
| 38     | `GET /suppliers/{id}/documents`                                   | TK-04      |
| 40     | `DELETE /suppliers/{id}/documents/{docId}`                        | QL-07      |
| 41, 43 | `GET /customers`, `GET /customers/{id}`                           | BH-01      |
| 46, 49 | `GET /customers/{id}/receivers`, `DELETE /receivers/{receiverId}` | BH-02      |
| 59     | `GET /export-receipts`                                            | BH-06      |
| 69     | `GET /rental-receipts/{id}`                                       | BH-09      |
| 75, 76 | `GET /return-receipts`, `GET /return-receipts/{id}`               | TK-12      |
| 81, 82 | `GET /return-fee-policies`, `PUT /return-fee-policies/{id}`       | BH-11      |
| 84, 85 | `GET /stocktakes`, `GET /stocktakes/{id}`                         | TK-13      |

- **4 tham chiếu chéo sai đã sửa:** `KT-08` và `BH-02` trỏ `QL-10` (báo cáo) → đúng `QL-07` (xoá mềm); `BH-12` trỏ `QL-07` → đúng `QL-05` (duyệt trả hàng bán); mục `G-2` trỏ `QL-09` → đúng `QL-11` (quy tắc thuê).
- **Bản rà soát 3 (ERD v6):** bỏ 2 endpoint `POST /stock-adjustments`, `PUT /stock-adjustments/{id}/approve` (G-14) → 110/112 endpoint gốc còn dùng, đều có story. `GET /alerts/unread-count`, `PUT /alerts/{id}/read` giữ đường dẫn nhưng làm việc trên `Alerts.Is_Resolved`.
- **Endpoint ngoài 112:** `POST /identity/auth/register` (chưa chốt — G-1), `PUT /config/rental-rules` (P2 — G-2), endpoint trang admin thống kê (P2 — G-4).
- **Endpoint ngoài 112 bổ sung ở bản rà soát 2:**

| Endpoint / tham số                                                                                                         | Story               | Ưu tiên | Gap |
| -------------------------------------------------------------------------------------------------------------------------- | ------------------- | ------- | --- |
| `rentalType=RENT\|LEND` (body `POST /rental-receipts`), `?type=RENT\|LEND` (`GET /rental-receipts`, `GET /reports/rental`) | BH-07, BH-09, QL-09 | P1      | G-8 |
| `GET /export-receipts/{id}/print?template=delivery`                                                                        | BH-13               | P1      | G-9 |
| `GET /rental-receipts/{id}/print`                                                                                          | TK-16               | P1      | G-9 |
| `GET /return-receipts/{id}/print`                                                                                          | TK-16               | P1      | G-9 |
| `GET /stocktakes/{id}/print`                                                                                               | TK-16               | P1      | G-9 |
| `GET /stocktakes/{id}/print?template=adjustment`                                                                           | TK-16               | P1      | G-9 |
| `GET /reports/reconciliation?customerId=&from=&to=`                                                                        | KT-09               | P1      | G-9 |
| `GET /export-receipts/{id}/payments`, `GET /payments/{id}/print`                                                           | KT-10               | P2      | G-13 |
| `GET /sales-returns/{id}/print`                                                                                            | BH-11               | P2      | G-9 |
| Tham số `warehouseId` trên API danh sách/báo cáo/cảnh báo; `GET /identity/me` trả danh sách kho                            | CHUNG-07            | P1      | G-15 |
| `GET/POST /warehouses`, `GET/PUT/DELETE /warehouses/{id}`                                                                  | QL-12               | P1      | G-15 |
| `GET/PUT /identity/users/{userId}/warehouses`                                                                              | QT-07               | P1      | G-15 |
| `/stock-transfers` (list, create, detail, update, `approve`, `ship`, `receive`, delete, `print`)                                      | TK-17               | P2      | G-15 |

> Cần thêm các dòng này vào sheet _API & Testing_ của `Kế hoạch TigerNet.xlsx` khi khoá endpoint ở M1.

## 3.2 Story → test case trọng tâm

| Story                          | Test case                                 |
| ------------------------------ | ----------------------------------------- |
| PUB-02                         | TC-AUTH-01, TC-SEC-01                     |
| PUB-03                         | TC-AUTH-02                                |
| QT-02 / mọi story có quyền     | TC-RBAC-01, TC-SEC-01                     |
| TK-01                          | TC-PROD-01                                |
| TK-05, TK-07                   | TC-IMP-01                                 |
| TK-06                          | TC-IMP-02                                 |
| TK-10                          | TC-EXP-01                                 |
| KT-03, BH-03                   | TC-EXP-02                                 |
| QL-01                          | TC-EXP-03                                 |
| BH-07                          | TC-RENT-01                                |
| TK-12                          | TC-RENT-02                                |
| BH-09                          | TC-RENT-03                                |
| TK-13, QL-03                   | TC-STK-01, TC-STK-02                      |
| CHUNG-07, QT-07, QL-12         | TC-RBAC-01 (ngoài kho được phân công → 403) |
| TK-02 / CHUNG-04               | TC-ALERT-01                               |
| CHUNG-05, TK-16, KT-09, BH-13  | TC-REP-01 (file mở được, số liệu khớp DB) |
| OPS-01…03, database-per-tenant | TC-TENANT-01                              |
| DEV-11                         | TC-PERF-01                                |
| OPS-05                         | TC-BAK-01                                 |

> Các story còn lại (CRUD danh mục, FE/UI, OPS-04/06/07/08, DEV-01→DEV-09) không có TC riêng trong sheet _API & Testing_ → dùng ca kiểm thử chung: validation 400, RBAC 403 (`TC-RBAC-01`), ghi `Audit_Logs`, CI xanh.

## 3.3 Tổng kết backlog

| Nhóm                   | Số story | P1     | P2     | Story tính P2  |
| ---------------------- | -------- | ------ | ------ | -------------- |
| PUB & CHUNG            | 13       | 12     | 1      | PUB-05         |
| Thủ kho (TK)           | 16       | 14     | 2      | TK-04, TK-17   |
| Kế toán (KT)           | 10       | 8      | 2      | KT-08, KT-10   |
| Bán hàng (BH)          | 13       | 11     | 2      | BH-11, BH-12   |
| Quản lý (QL)           | 11       | 10     | 1      | QL-05          |
| Quản trị hệ thống (QT) | 7        | 6      | 1      | QT-06          |
| Admin vận hành (OPS)   | 8        | 6      | 2      | OPS-07, OPS-08 |
| DEV (nền tảng)         | 12       | 11     | 1      | DEV-09 (P1/P2) |
| **Tổng**               | **90**   | **78** | **12** |                |

> **Ghi chú tính điểm:** `PUB-04` tính vào P1, `DEV-09` tính vào P2. Nếu chốt bỏ self-register (G-1) thì `PUB-04` chuyển sang P2 → **P1 = 77, P2 = 13**. Bản rà soát 3: `TK-14`, `QL-04` đã gộp (không tính); thêm `CHUNG-07`, `QL-12`, `QT-07` (P1), `TK-17`, `KT-10` (P2).

**Exit criteria của backlog này:** 112 endpoint đều có story nhận (xem 3.1 – 3.1.1) · mọi story P1 có AC · 20 test case trọng tâm đối chiếu đủ 11 nhóm test (3.2) · 15 gap G-1→G-15 được chốt (G-1→G-12 trong tuần 1, G-13→G-15 ngày 06/10/2026).

---

# PHẦN 4 — GÓI PHÁT HÀNH THEO TUẦN (MILESTONE)

> Cắt 90 story thành 6 lát theo đúng lịch GD1→GD12 trong `Kế hoạch TigerNet.xlsx`. M1–M5 là Phase 1 (nghiệm thu 27/12/2026); M6 là Phase 2, **không chặn nghiệm thu**.

| Milestone                                | Tuần           | Giai đoạn | Nội dung (story)                                                                                                                                            | Exit condition                                                                                                                                                                                                                                                 |
| ---------------------------------------- | -------------- | --------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **M1 — Nền tảng & xác thực**             | 1–5            | GD1–GD3   | `PUB-01`, `PUB-02`, `PUB-04` (chốt G-1), `PUB-06`, `CHUNG-01`, `CHUNG-02`, `CHUNG-06`, `DEV-01`→`DEV-08`                                                    | Chốt 15 gap; đăng nhập JWT + refresh + khóa 15’ sau 5 lần sai; menu render theo vai trò qua `/identity/me`; API contract + format lỗi chuẩn hoá; `/system/health` xanh; **khoá danh sách endpoint** (110 gốc + endpoint bổ sung ở 3.1.1)                                                                  |
| **M2 — Danh mục, phân quyền & quản trị** | 6–8            | GD3–GD4   | `PUB-03`, `QT-01`, `QT-02`, `QT-03`, `QL-06`, `QL-07`, `QL-08`, `QL-12`, `QT-07`, `CHUNG-07`, `TK-01`, `TK-02`, `TK-03`, `KT-02`, `BH-01`, `BH-02`                                        | CRUD SP/nhóm/NCC/KH/người nhận + serial; danh mục kho + phân công, dữ liệu lọc theo kho; tồn kho theo kho & cảnh báo low-stock; màn hình user/role/audit-log; `TC-PROD-01`, `TC-AUTH-02`, `TC-RBAC-01`, `TC-SEC-01` xanh                                                                                                |
| **M3 — Nhập & xuất kho**                 | 8–10           | GD5–GD6   | `TK-05`→`TK-11`, `KT-01`, `KT-03`, `KT-04`, `BH-03`→`BH-06`, `BH-13`, `QL-01`, `QL-02`                                                                      | Phiếu nhập/xuất đủ luồng xác nhận-huỷ-in, in phiếu giao hàng; tồn tăng/trừ đúng theo kho; VAT/đã trả/còn lại tính đúng; `TC-IMP-01`, `TC-IMP-02`, `TC-EXP-01`, `TC-EXP-02`, `TC-EXP-03` xanh                                                                                   |
| **M4 — Cho thuê, kiểm kê & điều chỉnh**  | 10–12          | GD7–GD8   | `BH-07`→`BH-10`, `TK-12`, `TK-13`, `TK-16`, `QL-03`, `KT-07`, `QL-11`                                                                     | Thuê và mượn (`RENT`/`LEND`) đủ luồng; serial chuyển `RENTED`/`LENT`; in phiếu thuê/mượn, biên bản trả, kiểm kê, điều chỉnh; cảnh báo trước 7 ngày; phạt ≥10 ngày đúng công thức; kiểm kê → Quản lý chốt → tự sinh điều chỉnh; `TC-RENT-01/02/03`, `TC-STK-01/02` xanh |
| **M5 — Dashboard, báo cáo & nghiệm thu** | 11–13          | GD9–GD12  | `CHUNG-03`, `CHUNG-04`, `CHUNG-05`, `TK-15`, `KT-05`, `KT-06`, `KT-09`, `QL-09`, `QL-10`, `QT-04`, `QT-05`, `DEV-10`, `DEV-11`, `DEV-12`, `OPS-01`→`OPS-06` | Dashboard + 4 nhóm báo cáo + xuất Excel/PDF; biên bản đối chiếu công nợ; trung tâm cảnh báo 3 loại; backup/restore chạy được; perf + security + cách ly tenant đạt; UAT 5 vai trò sign-off **27/12/2026**                                               |
| **M6 — Phase 2**                         | Sau nghiệm thu | —         | `PUB-05`, `TK-04`, `TK-17`, `KT-08`, `KT-10`, `BH-11`, `BH-12`, `QL-05`, `QT-06`, `OPS-07`, `OPS-08`, `DEV-09` (+ `PUB-04` nếu giữ, `PUT /config/rental-rules` của `QL-11`)   | Branding tenant, tài liệu/TK ngân hàng NCC, chuyển kho, phiếu thu/chi + lịch sử thanh toán, trả hàng bán + phí hoàn, trang admin thống kê, License Server, đóng gói On-Premise                                                                                                                                 |

**Ràng buộc thứ tự:** M2 chặn M3 (không có danh mục + tồn thì không nhập/xuất) · M3 chặn M4 (không có phiếu xuất thì không có luồng thuê/trả) · M4 chặn M5 (báo cáo cần dữ liệu nghiệp vụ) · `OPS-01`→`OPS-06` chạy **song song từ tuần 11** (backup + CI/CD không phụ thuộc chức năng).

---

# PHẦN 5 — QUY ƯỚC HOÀN THÀNH

## 5.1 Definition of Ready (kéo story vào tuần làm việc)

1. Endpoint, method, query param đã chốt theo sheet _API & Testing_ (112 endpoint khoá ở M1).
2. AC ghi rõ điều kiện đạt; có test case đối chiếu (TC riêng hoặc ca kiểm thử chung ở 3.2).
3. Schema/ERD tương ứng đã map (ERD v1.2 `tigernet_erd_v1_2.drawio.png`, `Database_Design_v7.md`).
4. Story Phase 2 chỉ mở khi story P1 cùng chuỗi đã done.

## 5.2 Definition of Done (story "Hoàn thành")

1. Đủ AC, chạy tay trên staging đúng kết quả.
2. Unit test service layer coverage ≥70%, 0 test fail (`TC` nhóm Unit).
3. API test: happy case + validation 400 + phân quyền 403 đúng chỗ.
4. Mọi thao tác ghi/bấm quan trọng đều ghi `Audit_Logs` (NFR-5).
5. API contract & mã lỗi cập nhật (theo `DEV-08`); response JSON đúng format lỗi chuẩn hoá.
6. CI xanh: lint + unit + API test trước khi merge (`DEV-03` Git flow).
7. FE: đúng NOTE UXUI tương ứng, responsive Desktop/Tablet/Mobile, dark/light (NFR-4).

## 5.3 Checklist chốt ở tuần 1 (28/09–04/10/2026)

- [ ] G-1 — self-register: bỏ hay bổ sung `POST /identity/auth/register`
- [ ] G-2 — giá trị thuê (7 ngày / 10 ngày phạt) hard-code P1
- [x] G-3 — bảo hành/hư hỏng gộp qua `POST /return-receipts` + `Inventory.Status = DAMAGED`
- [ ] G-4 — trang admin thống kê (NOTE UXUI #9) xác nhận P2
- [ ] G-5 — 5 mục NOTE UXUI chưa fixed gán rõ story + tuần
- [x] G-6 — xoá/bỏ dùng `Kế hoạch TigerNet.backup.xlsx` (đã xoá)
- [ ] G-7 — điều chỉnh tải tuần 11–13 (kéo perf/security về tuần 11–12) — 1 người
- [x] G-8 — cho mượn: đã chốt — hàng mượn quá hạn tính phạt như hàng thuê (≥10 ngày)
- [ ] G-9 — duyệt danh sách biểu mẫu `BIEU_MAU.md` + thêm endpoint in vào sheet _API & Testing_; kế toán xác nhận chế độ kế toán (mẫu số 01-VT, 02-VT, 05-VT, 01-TT, 02-TT)
- [x] G-10 — chốt ERD v6 là nguồn chuẩn; `database/tenant/` viết lại theo v6 (06/10)
- [x] G-11 — Control Plane P1 tối thiểu (đã chốt)
- [ ] G-12 — bổ sung hoặc bỏ tham chiếu `Database_Design.docx`, SRS
- [x] G-13 — giữ đúng ERD v6: không bảng thanh toán, phiếu thu/chi sang P2 (`KT-10`)
- [x] G-14 — xử lý các chỗ ERD v6 thiếu theo `Database_Design_v6.md` mục "Phạm vi triển khai"
- [x] G-15 — đa kho P1 (`CHUNG-07`, `QL-12`, `QT-07`), chuyển kho P2 (`TK-17`)
- [ ] Duyệt milestone M1–M6 và thứ tự chặn ở Phần 4
