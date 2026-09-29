# TIGERNET — DANH SÁCH BIỂU MẪU IN

Danh sách chứng từ/biểu mẫu hệ thống cần in (PDF/Excel), đối chiếu với schema `database/tenant/V1__init_schema.sql` và backlog `BACKLOG.md`.

- **Nguồn:** bảng `document_sequences` (mã PN, PX, PT, TT, TH, KK, DC, TTN), 112 endpoint sheet _API & Testing_, việc #23, #28, #33 trong `Kế hoạch TigerNet.xlsx`.
- **Vị trí mẫu trong mã nguồn (đề xuất):** `backend/src/main/resources/templates/print/`.
- **Ưu tiên:** **P1** = Phase 1 (nghiệm thu 27/12/2026), **P2** = Phase 2.

---

## 1. Nguyên tắc

1. **Không tự in hoá đơn GTGT.** Hoá đơn VAT là hoá đơn điện tử. Hệ thống chỉ lưu số hoá đơn vào `export_receipts.e_invoice_no` và in chứng từ nội bộ.
2. **Mẫu số tham chiếu** (01-VT, 02-VT, 05-VT, 01-TT, 02-TT) theo chế độ kế toán doanh nghiệp. Chế độ cụ thể (TT200 / TT133 / TT99-2025) **phải được kế toán công ty xác nhận** trước khi chốt bố cục — xem mục 5.
3. **Số liệu in lấy từ snapshot trên phiếu**, không tính lại từ danh mục: `vat_percent`, `receiver_name/phone/address`, `unit_price`, `price_per_day`, `fee_percent`. Sửa danh mục/mẫu VAT không làm đổi bản in của phiếu cũ.
4. **In lại được nhiều lần**, mỗi lần in ghi `audit_logs` (action `PRINT`).
5. **Phiếu chưa xác nhận** (`DRAFT` / `PENDING`) in kèm dấu mờ "BẢN NHÁP".

---

## 2. Tổng hợp biểu mẫu

| #   | Biểu mẫu                                                 | Mẫu số tham chiếu | Mã  | Bảng nguồn                                                          | Vai trò in        | Endpoint in                                                 | Story        | Ưu tiên |
| --- | -------------------------------------------------------- | ----------------- | --- | ------------------------------------------------------------------- | ----------------- | ----------------------------------------------------------- | ------------ | ------- |
| 1   | Phiếu nhập kho                                           | 01-VT             | PN  | `import_receipts`, `import_receipt_lines`                           | Thủ kho, Kế toán  | `GET /import-receipts/{id}/print`                           | TK-09, KT-01 | P1      |
| 2   | Phiếu xuất kho (bán hàng)                                | 02-VT             | PX  | `export_receipts`, `export_receipt_lines`                           | Bán hàng, Kế toán | `GET /export-receipts/{id}/print`                           | BH-05, KT-04 | P1      |
| 3   | Phiếu giao hàng                                          | nội bộ            | PX  | `export_receipts`, `export_receipt_lines`                           | Thủ kho, Bán hàng | `GET /export-receipts/{id}/print?template=delivery` _(mới)_ | BH-13        | P1      |
| 4   | Phiếu cho thuê / Phiếu cho mượn (kèm biên bản giao hàng) | nội bộ            | PT  | `rental_receipts`, `rental_receipt_lines`                           | Bán hàng          | `GET /rental-receipts/{id}/print` _(mới)_                   | TK-16        | P1      |
| 5   | Biên bản trả hàng thuê / mượn                            | nội bộ            | TT  | `rental_returns`, `rental_return_lines`                             | Thủ kho, Bán hàng | `GET /rental-returns/{id}/print` _(mới)_                    | TK-16        | P1      |
| 6   | Biên bản kiểm kê                                         | 05-VT             | KK  | `stocktakes`, `stocktake_lines`                                     | Thủ kho, Quản lý  | `GET /stocktakes/{id}/print` _(mới)_                        | TK-16        | P1      |
| 7   | Phiếu điều chỉnh tồn / Biên bản xử lý hàng hư hỏng, mất  | nội bộ            | DC  | `stock_adjustments`, `stock_adjustment_lines`                       | Thủ kho, Quản lý  | `GET /stock-adjustments/{id}/print` _(mới)_                 | TK-16        | P1      |
| 8   | Phiếu thu / Phiếu chi                                    | 01-TT / 02-TT     | TTN | `payments`                                                          | Kế toán           | `GET /payments/{id}/print` _(mới)_                          | KT-09        | P1      |
| 9   | Biên bản đối chiếu công nợ                               | nội bộ            | —   | `export_receipts`, `rental_receipts`, `import_receipts`, `payments` | Kế toán           | `GET /reports/reconciliation?partnerId=&from=&to=` _(mới)_  | KT-09        | P1      |
| 10  | Phiếu trả hàng bán / đổi hàng + Phiếu bảo hành           | nội bộ            | TH  | `sales_returns`, `sales_return_lines`, `export_receipt_lines`       | Bán hàng, Quản lý | `GET /sales-returns/{id}/print` _(mới)_                     | BH-11        | P2      |

---

## 3. Khung chung mọi biểu mẫu

| Phần          | Nội dung                                                                                                |
| ------------- | ------------------------------------------------------------------------------------------------------- |
| Đầu trang     | Logo + tên công ty (tenant), MST, địa chỉ, SĐT — P1 lấy từ cấu hình tenant; P2 lấy từ `tenant_branding` |
| Tiêu đề       | Tên phiếu (IN HOA), mẫu số (nếu có), mã phiếu `code` (vd `PN-2026-00001`), ngày lập                     |
| Đối tác       | Tên, mã, MST/CCCD, địa chỉ, SĐT từ `partners`; người nhận (snapshot) nếu có                             |
| Bảng chi tiết | STT, mã SP (`products.sku`), tên SP, ĐVT (`products.unit`), serial, số lượng, đơn giá, thành tiền       |
| Phần tổng     | Tạm tính, chiết khấu, VAT, tổng cộng, **số tiền bằng chữ** (tiếng Việt), đã trả, còn lại                |
| Ghi chú       | `note` của phiếu                                                                                        |
| Chữ ký        | Theo từng mẫu (mục 4); mỗi ô: chức danh, "(Ký, họ tên)", tên in sẵn từ `users.full_name` nếu có         |
| Chân trang    | Người in, thời điểm in, số trang "Trang x/y"                                                            |
| Khổ giấy      | A4 dọc (mặc định); A5 cho phiếu giao hàng, phiếu thu/chi                                                |
| Định dạng     | PDF (in, ký); Excel (kế toán đối soát)                                                                  |

---

## 4. Chi tiết từng biểu mẫu

### 4.1 Phiếu nhập kho (PN — 01-VT) · P1

- **Đầu phiếu:** `code`, `receipt_date`, kho (`warehouses.name`), NCC (`supplier_id` → `partners`), số hoá đơn NCC `supplier_invoice_no`, trạng thái `status`.
- **Chi tiết** (`import_receipt_lines`): `line_no`, SP, `serial_no`, `quantity`, `unit_price`, `line_total`, `note`.
- **Tổng:** `subtotal`, `vat_amount`, `total_amount`, `paid_amount`, còn nợ NCC = `total_amount - paid_amount`, bằng chữ.
- **Ký:** Người lập (`created_by`) · Người giao hàng · Thủ kho (`confirmed_by`) · Kế toán trưởng.

### 4.2 Phiếu xuất kho — bán hàng (PX — 02-VT) · P1

- **Đầu phiếu:** `code`, `export_date`, `delivery_date`, kho, khách hàng (`customer_id`), người nhận snapshot (`receiver_name`, `receiver_phone`, `receiver_address`), số HĐĐT `e_invoice_no`.
- **Chi tiết** (`export_receipt_lines`): SP, serial (`serial_id` → `product_serials.serial_no`), `quantity`, `unit_price`, `line_total`, `warranty_months`.
- **Tổng:** `subtotal`, `discount_amount`, VAT (`vat_percent` %, `vat_amount`), `total_amount`, `paid_amount`, `remaining_amount`, `payment_status`, bằng chữ.
- **Ký:** Người lập · Thủ kho (`completed_by`) · Người nhận hàng · Kế toán · Quản lý duyệt (`confirmed_by`).

### 4.3 Phiếu giao hàng (PX — nội bộ) · P1

- Cùng dữ liệu phiếu xuất nhưng **không hiện giá**; khổ A5.
- **Nội dung:** mã phiếu xuất, ngày giao, người nhận + SĐT + địa chỉ, danh sách SP/serial/số lượng, ghi chú giao hàng.
- **Ký:** Người giao · Người nhận (kèm ngày giờ nhận).

### 4.4 Phiếu cho thuê / Phiếu cho mượn (PT — nội bộ) · P1

- **Tiêu đề theo `rental_type`:** `RENT` → "PHIẾU CHO THUÊ", `LOAN` → "PHIẾU CHO MƯỢN".
- **Đầu phiếu:** `code`, khách hàng, người nhận snapshot, `start_date`, `due_date`, số ngày thuê/mượn.
- **Chi tiết** (`rental_receipt_lines`): SP, serial, `quantity`, `price_per_day` (ẩn với LOAN), `line_total` (ẩn với LOAN).
- **Tổng (chỉ RENT):** `rent_amount`, `deposit_amount`, `total_amount`, `paid_amount`, `remaining_amount`, bằng chữ.
- **Điều khoản in sẵn:** cảnh báo trước `rental.expiry_warning_days` (7) ngày; quá hạn từ `rental.penalty_min_overdue_days` (10) ngày tính phạt = số ngày quá hạn × giá thuê/ngày (**cả RENT và LOAN**); bồi thường hư hỏng/mất (cả RENT và LOAN).
- **Biên bản giao hàng** (trang 2 hoặc phần cuối): tình trạng hàng khi giao, phụ kiện kèm theo.
- **Ký:** Bên cho thuê/mượn (Bán hàng) · Thủ kho · Bên thuê/mượn.

### 4.5 Biên bản trả hàng thuê / mượn (TT — nội bộ) · P1

- **Đầu phiếu:** `code`, phiếu thuê gốc (`rental_id` → `rental_receipts.code`, `rental_type`), `return_date`, `due_date` gốc, `days_overdue`.
- **Chi tiết** (`rental_return_lines`): SP, serial, `quantity`, `item_condition` (GOOD / DAMAGED / LOST), `condition_note`, `damage_fee`.
- **Tổng:** `penalty_amount` (cả RENT và LOAN), `damage_amount`, `deposit_refund_amount`, số tiền khách phải trả thêm / được hoàn, bằng chữ.
- **Ghi rõ** trả đủ hay trả một phần (phiếu thuê chuyển `PARTIALLY_RETURNED` / `RETURNED`).
- **Ký:** Người trả hàng · Thủ kho kiểm tra · Bán hàng.

### 4.6 Biên bản kiểm kê (KK — 05-VT) · P1

- **Đầu phiếu:** `code`, kho, `stocktake_date`, thời điểm chốt số hệ thống, thành phần ban kiểm kê.
- **Chi tiết** (`stocktake_lines`): SP, ĐVT, `system_qty`, `actual_qty`, `difference` (thừa +/thiếu −), `note`.
- **Tổng:** số mặt hàng khớp / thừa / thiếu; giá trị chênh lệch (theo giá vốn).
- **Kết luận:** mã phiếu điều chỉnh sinh tự động (nếu có).
- **Ký:** Thủ kho · Kế toán · Quản lý duyệt (`approved_by`).

### 4.7 Phiếu điều chỉnh tồn / Biên bản xử lý hàng hư hỏng, mất (DC — nội bộ) · P1

- **Tiêu đề theo `reason_type`:** `STOCKTAKE` / `FOUND` / `OTHER` → "PHIẾU ĐIỀU CHỈNH TỒN KHO"; `DAMAGE` / `LOSS` → "BIÊN BẢN XỬ LÝ HÀNG HƯ HỎNG, MẤT".
- **Đầu phiếu:** `code`, kho, phiếu kiểm kê gốc (`stocktake_id`, nếu có), `reason`.
- **Chi tiết** (`stock_adjustment_lines`): SP, serial, `qty_change` (+/−), `note`.
- **Trạng thái:** `status` (PENDING / APPROVED / REJECTED), `reject_reason` nếu bị từ chối.
- **Ký:** Người lập · Thủ kho · Quản lý duyệt (`approved_by`).

### 4.8 Phiếu thu / Phiếu chi (TTN — 01-TT / 02-TT) · P1

- **Tiêu đề theo `direction`:** `IN` → "PHIẾU THU", `OUT` → "PHIẾU CHI".
- **Nội dung:** `code`, `paid_at`, người nộp/nhận tiền (`partner_id`), lý do theo `purpose`:

| `purpose`        | Chiều | Lý do in trên phiếu                  |
| ---------------- | ----- | ------------------------------------ |
| `SALE`           | IN    | Thu tiền bán hàng theo phiếu xuất …  |
| `RENTAL_FEE`     | IN    | Thu tiền thuê theo phiếu thuê …      |
| `DEPOSIT`        | IN    | Thu tiền cọc thuê theo phiếu …       |
| `PENALTY`        | IN    | Thu tiền phạt quá hạn / bồi thường … |
| `DEPOSIT_REFUND` | OUT   | Hoàn tiền cọc theo phiếu …           |
| `SALES_REFUND`   | OUT   | Hoàn tiền trả hàng theo phiếu …      |
| `PURCHASE`       | OUT   | Chi trả NCC theo phiếu nhập …        |

- **Chi tiết:** chứng từ gốc (1 trong `export_receipt_id` / `rental_receipt_id` / `import_receipt_id` / `sales_return_id`), `amount` + bằng chữ, `method` (CASH / BANK_TRANSFER / CARD / OTHER), `reference_no`.
- Phiếu đã huỷ (`is_voided`) in dấu "ĐÃ HUỶ" + `void_reason`.
- **Ký:** Giám đốc · Kế toán trưởng · Người lập phiếu · Thủ quỹ · Người nộp/nhận tiền.

### 4.9 Biên bản đối chiếu công nợ (nội bộ) · P1

- **Tham số:** đối tác (`partners`), kỳ từ ngày – đến ngày.
- **Nội dung:** số dư đầu kỳ; phát sinh tăng (phiếu xuất / phiếu thuê / phiếu nhập đã xác nhận); phát sinh giảm (`payments` chưa huỷ); số dư cuối kỳ.
- **Bảng chi tiết:** ngày, mã chứng từ, diễn giải, phát sinh nợ, phát sinh có, số dư.
- Đối tác vừa là KH vừa là NCC (`is_customer` + `is_supplier`) → tách 2 phần phải thu / phải trả.
- **Ký:** Đại diện bên bán · Đại diện bên mua (xác nhận số liệu).

### 4.10 Phiếu trả hàng bán / đổi hàng + Phiếu bảo hành (TH — nội bộ) · P2

- **Tiêu đề theo `return_type`:** `REFUND` → "PHIẾU TRẢ HÀNG", `EXCHANGE` → "PHIẾU ĐỔI HÀNG", `WARRANTY` → "PHIẾU TIẾP NHẬN BẢO HÀNH".
- **Đầu phiếu:** `code`, phiếu xuất gốc (`export_receipt_id`), `return_date`, `days_since_delivery`, chính sách phí áp dụng (`fee_policy_id`, `fee_percent`).
- **Chi tiết** (`sales_return_lines`): SP, serial, `quantity`, `item_condition`, `disposition` (RESTOCK / DAMAGED / WARRANTY_REPAIR).
- **Tổng:** `original_value`, `fee_amount`, `refund_amount`; phiếu xuất hàng đổi `exchange_export_receipt_id` (nếu có).
- **Phiếu bảo hành** (in kèm phiếu xuất hoặc riêng): SP, serial, ngày bán, `warranty_months`, `warranty_expiry`, điều kiện bảo hành.
- **Ký:** Khách hàng · Bán hàng · Quản lý duyệt (`approved_by`).

---

## 5. Câu hỏi mở (cần chốt)

| #   | Câu hỏi                                                                                | Ảnh hưởng                                       |
| --- | -------------------------------------------------------------------------------------- | ----------------------------------------------- |
| Q1  | Doanh nghiệp áp dụng chế độ kế toán nào (TT200 / TT133 / TT99-2025)?                   | Mẫu số và bố cục mẫu 1, 2, 6, 8                 |
| Q2  | Có cần mẫu "Phiếu xuất kho kiêm vận chuyển nội bộ" khi chuyển hàng giữa các kho không? | Hiện P1 chỉ có 1 kho (`KHO-01`) → đề xuất để P2 |
