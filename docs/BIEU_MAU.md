# TIGERNET — DANH SÁCH BIỂU MẪU IN

Danh sách chứng từ/biểu mẫu hệ thống cần in (PDF/Excel), đối chiếu với ERD v7 (`Database_Design_v7.md`, `database/tenant/V1__init_schema.sql`) và backlog `BACKLOG.md`.

- **Nguồn:** số phiếu `Receipt_Code` / `Stocktake_Code` / `Transfer_Code` (mã PN, PX, PT, TT, TH, KK, CK), 112 endpoint sheet _API & Testing_, việc #23, #28, #33 trong `Kế hoạch TigerNet.xlsx`.
- **Vị trí mẫu trong mã nguồn:** `backend/src/main/resources/templates/print/` (xem `Backend_Structure_v6.md` mục 3).
- **Ưu tiên:** **P1** = Phase 1 (nghiệm thu 27/12/2026), **P2** = Phase 2.

---

## 1. Nguyên tắc

1. **Không tự in hoá đơn GTGT.** Hoá đơn VAT là hoá đơn điện tử, xuất ở phần mềm hoá đơn riêng. ERD v7 không lưu số hoá đơn điện tử; hệ thống chỉ in chứng từ nội bộ.
2. **Mẫu số tham chiếu** (01-VT, 02-VT, 05-VT, 01-TT, 02-TT) theo chế độ kế toán doanh nghiệp. Chế độ cụ thể (TT200 / TT133 / TT99-2025) **phải được kế toán công ty xác nhận** trước khi chốt bố cục — xem mục 5.
3. **Người nhận in từ snapshot trên phiếu** (`Receiver_Name_Snapshot`, `Receiver_Phone_Snapshot`, `Receiver_Address_Snapshot`), giá in từ dòng phiếu (`Price_In`, `Price_Out`, `Rental_Price`), không lấy lại từ danh mục. Riêng VAT: phiếu trỏ `VAT_ID` nên **không được sửa `Rate` của mức VAT đã dùng** (thêm mức mới thay vì sửa) để bản in phiếu cũ không đổi.
4. **Mỗi phiếu thuộc một kho:** đầu phiếu luôn in mã kho, tên kho, địa chỉ kho (`Warehouses`). Số phiếu có mã kho: `PN-HN01-2026-0001`.
5. **In lại được nhiều lần**, mỗi lần in ghi `Audit_Logs` (`Action = PRINT`).
6. **Phiếu chưa có hiệu lực** (`DRAFT` / `PENDING`) in kèm dấu mờ "BẢN NHÁP".

---

## 2. Tổng hợp biểu mẫu

| #   | Biểu mẫu                                                 | Mẫu số tham chiếu | Mã  | Bảng nguồn                                                       | Vai trò in        | Endpoint in                                                 | Story        | Ưu tiên |
| --- | -------------------------------------------------------- | ----------------- | --- | ---------------------------------------------------------------- | ----------------- | ----------------------------------------------------------- | ------------ | ------- |
| 1   | Phiếu nhập kho                                           | 01-VT             | PN  | `Import_Receipts`, `Import_Receipt_Details`                      | Thủ kho, Kế toán  | `GET /import-receipts/{id}/print`                           | TK-09, KT-01 | P1      |
| 2   | Phiếu xuất kho (bán hàng)                                | 02-VT             | PX  | `Export_Receipts`, `Export_Receipt_Details`                      | Bán hàng, Kế toán | `GET /export-receipts/{id}/print`                           | BH-05, KT-04 | P1      |
| 3   | Phiếu giao hàng                                          | nội bộ            | PX  | `Export_Receipts`, `Export_Receipt_Details`                      | Thủ kho, Bán hàng | `GET /export-receipts/{id}/print?template=delivery` _(mới)_ | BH-13        | P1      |
| 4   | Phiếu cho thuê / Phiếu cho mượn (kèm biên bản giao hàng) | nội bộ            | PT  | `Rental_Receipts`, `Rental_Receipt_Details`                      | Bán hàng          | `GET /rental-receipts/{id}/print` _(mới)_                   | TK-16        | P1      |
| 5   | Biên bản trả hàng thuê / mượn                            | nội bộ            | TT  | `Return_Receipts`, `Return_Receipt_Details`                      | Thủ kho, Bán hàng | `GET /return-receipts/{id}/print` _(mới)_                   | TK-16        | P1      |
| 6   | Biên bản kiểm kê                                         | 05-VT             | KK  | `Stocktakes`, `Stocktake_Details`                                | Thủ kho, Quản lý  | `GET /stocktakes/{id}/print` _(mới)_                        | TK-16        | P1      |
| 7   | Phiếu điều chỉnh tồn kho                                 | nội bộ            | KK  | `Stock_Adjustments` (theo `Stocktakes`)                          | Thủ kho, Quản lý  | `GET /stocktakes/{id}/print?template=adjustment` _(mới)_    | TK-16        | P1      |
| 8   | Biên bản đối chiếu công nợ (phải thu khách hàng)         | nội bộ            | —   | `Export_Receipts`, `Rental_Receipts`, `Parties`                  | Kế toán           | `GET /reports/reconciliation?customerId=&from=&to=` _(mới)_ | KT-09        | P1      |
| 9   | Phiếu thu / Phiếu chi                                    | 01-TT / 02-TT     | —   | _cần bổ sung bảng thanh toán vào ERD (G-13)_                     | Kế toán           | `GET /payments/{id}/print` _(mới)_                          | KT-10        | P2      |
| 10  | Phiếu trả hàng bán / đổi hàng + Phiếu bảo hành           | nội bộ            | TH  | `Sales_Returns`, `Sales_Return_Details`, `Export_Receipt_Details` | Bán hàng, Quản lý | `GET /sales-returns/{id}/print` _(mới)_                     | BH-11        | P2      |
| 11  | Phiếu chuyển kho (kiêm vận chuyển nội bộ)                | nội bộ (xem Q2)   | CK  | `Stock_Transfers`, `Stock_Transfer_Details`                   | Thủ kho, Quản lý  | `GET /stock-transfers/{id}/print` _(mới)_                   | TK-17        | P2      |

---

## 3. Khung chung mọi biểu mẫu

| Phần          | Nội dung                                                                                                         |
| ------------- | ---------------------------------------------------------------------------------------------------------------- |
| Đầu trang     | Logo + tên công ty (tenant), MST, địa chỉ, SĐT — P1 lấy từ cấu hình tenant; P2 lấy từ `tenant_branding`; kèm **mã kho, tên kho, địa chỉ kho** |
| Tiêu đề       | Tên phiếu (IN HOA), mẫu số (nếu có), số phiếu (`Receipt_Code`…, vd `PN-MAIN-2026-0001`), ngày lập                 |
| Đối tác       | Tên, MST/CCCD, địa chỉ, SĐT từ `Parties`; người nhận (snapshot) nếu có                                           |
| Bảng chi tiết | STT, mã SP (`Products.ID`), tên SP, ĐVT (`Products.Unit`), serial, số lượng, đơn giá, thành tiền (`Line_Total`)  |
| Phần tổng     | Tổng tiền, VAT, tổng cộng, **số tiền bằng chữ** (tiếng Việt), đã trả, còn lại (tuỳ mẫu)                          |
| Ghi chú       | `Note` của phiếu                                                                                                 |
| Chữ ký        | Theo từng mẫu (mục 4); mỗi ô: chức danh, "(Ký, họ tên)", tên in sẵn từ `Users.Full_Name` nếu có                  |
| Chân trang    | Người in, thời điểm in, số trang "Trang x/y"                                                                     |
| Khổ giấy      | A4 dọc (mặc định); A5 cho phiếu giao hàng                                                                        |
| Định dạng     | PDF (in, ký); Excel (kế toán đối soát)                                                                           |

---

## 4. Chi tiết từng biểu mẫu

### 4.1 Phiếu nhập kho (PN — 01-VT) · P1

- **Đầu phiếu:** `Receipt_Code`, `Date_In`, kho (`Warehouse_ID` → `Warehouses`), nguồn nhập (`Source_Party_ID` → `Parties`: NCC / khách bán lẻ kèm CCCD / khách đổi-trả), trạng thái `Status`.
- **Chi tiết** (`Import_Receipt_Details`): SP, `Serial`, `Tracking_Code`, `Quantity`, `Price_In`, `Line_Total`.
- **Tổng:** `Total_Amount`, bằng chữ. _(ERD v7 không có VAT đầu vào và số tiền đã trả NCC — G-13.)_
- **Ký:** Người lập (`Created_By`) · Người giao hàng · Thủ kho · Kế toán trưởng.

### 4.2 Phiếu xuất kho — bán hàng (PX — 02-VT) · P1

- **Đầu phiếu:** `Receipt_Code`, `Date_Out`, `Delivery_Date`, kho, khách hàng (`Customer_ID` → `Customers` → `Parties`), người nhận snapshot.
- **Chi tiết** (`Export_Receipt_Details`): SP, `Serial`, `Quantity`, `Price_Out`, `Line_Total`, `Warranty_Months`, `Warranty_Expiry`.
- **Tổng:** tổng trước thuế, VAT (`VAT_ID` → `VAT_Options.VAT`, `Rate`), `Total_Amount`, `Deposit`, `Paid`, `Remain`, trạng thái thanh toán (`Payment_Status_ID`), bằng chữ.
- **Ký:** Người lập (`Created_By`) · Thủ kho · Người nhận hàng · Kế toán · Quản lý duyệt.

### 4.3 Phiếu giao hàng (PX — nội bộ) · P1

- Cùng dữ liệu phiếu xuất nhưng **không hiện giá**; khổ A5.
- **Nội dung:** số phiếu xuất, kho xuất, ngày giao, người nhận + SĐT + địa chỉ (snapshot), danh sách SP/serial/số lượng, ghi chú giao hàng.
- **Ký:** Người giao · Người nhận (kèm ngày giờ nhận).

### 4.4 Phiếu cho thuê / Phiếu cho mượn (PT — nội bộ) · P1

- **Tiêu đề theo `Rental_Type`:** `RENT` → "PHIẾU CHO THUÊ", `LEND` → "PHIẾU CHO MƯỢN".
- **Đầu phiếu:** `Receipt_Code`, kho, khách hàng, người nhận snapshot, `Date_Out`, `Due_Date`, số ngày thuê/mượn.
- **Chi tiết** (`Rental_Receipt_Details`): SP, `Serial`, `Quantity`, `Rental_Price` (ẩn với LEND).
- **Tổng (chỉ RENT):** `Rental_Price_Per_Day`, `Total_Rent`, `Deposit`, bằng chữ.
- **Điều khoản in sẵn:** cảnh báo trước 7 ngày (`tigernet.rental.expiry-warning-days`); quá hạn từ 10 ngày (`tigernet.rental.penalty-min-overdue-days`) tính phạt = số ngày quá hạn × giá thuê/ngày (**cả RENT và LEND** — với LEND, giá/ngày chỉ dùng để tính phạt); hư hỏng/mất bồi thường theo thoả thuận (cả RENT và LEND).
- **Biên bản giao hàng** (trang 2 hoặc phần cuối): tình trạng hàng khi giao, phụ kiện kèm theo.
- **Ký:** Bên cho thuê/mượn (Bán hàng) · Thủ kho · Bên thuê/mượn.

### 4.5 Biên bản trả hàng thuê / mượn (TT — nội bộ) · P1

- **Đầu phiếu:** `Receipt_Code`, kho nhận lại (có thể khác kho cho thuê), phiếu thuê gốc (`Rental_Receipt_ID` → `Receipt_Code`, `Rental_Type`), `Return_Date`, `Due_Date` gốc, `Days_Overdue`.
- **Chi tiết** (`Return_Receipt_Details`): SP, `Serial`, `Quantity`, `Condition_Note` (tình trạng khi trả).
- **Tổng:** `Penalty_Amount` (cả RENT và LEND), bằng chữ. Bồi thường hư hỏng/mất và hoàn cọc in từ `Note` (ERD v7 không có cột tiền riêng — G-14).
- **Ghi rõ** trả đủ hay trả một phần (số dòng `Is_Returned` / tổng số dòng của phiếu thuê).
- **Ký:** Người trả hàng · Thủ kho kiểm tra · Bán hàng.

### 4.6 Biên bản kiểm kê (KK — 05-VT) · P1

- **Đầu phiếu:** `Stocktake_Code`, kho, `Stocktake_Date`, người kiểm kê (`Created_By`), thành phần ban kiểm kê.
- **Chi tiết** (`Stocktake_Details`): SP, ĐVT, `System_Qty`, `Actual_Qty`, `Difference` (thừa +/thiếu −).
- **Tổng:** số mặt hàng khớp / thừa / thiếu; giá trị chênh lệch (theo giá nhập trên `Inventory.Price_In`).
- **Kết luận:** đã chốt (`Status = COMPLETED`) hay chưa; số dòng điều chỉnh đã sinh.
- **Ký:** Thủ kho · Kế toán · Quản lý chốt.

### 4.7 Phiếu điều chỉnh tồn kho (KK — nội bộ) · P1

- Sinh từ phiếu kiểm kê đã chốt (ERD v7: điều chỉnh luôn gắn `Stocktake_ID`, không có phiếu điều chỉnh độc lập). Hàng hư hỏng/mất phát hiện khi kiểm kê ghi vào `Reason`.
- **Đầu phiếu:** số phiếu kiểm kê gốc, kho, ngày chốt.
- **Chi tiết** (`Stock_Adjustments`): SP, `Adjust_Qty` (+/−), `Reason`, `Created_At`.
- **Ký:** Người lập · Thủ kho · Quản lý duyệt.

### 4.8 Biên bản đối chiếu công nợ — phải thu khách hàng (nội bộ) · P1

- **Tham số:** khách hàng (`Customers` → `Parties`), kỳ từ ngày – đến ngày, kho (tuỳ chọn).
- **Bảng chi tiết:** ngày, số phiếu, diễn giải (bán / thuê / phạt quá hạn), tổng tiền, đã trả, còn lại.
  - Phiếu xuất `EXPORTED`: `Total_Amount`, `Paid`, `Remain`.
  - Phiếu thuê/mượn: `Total_Rent` + `Penalty_Amount`, `Deposit`, `Payment_Status_ID`.
- **Tổng:** tổng còn phải thu, bằng chữ; khớp báo cáo công nợ (`KT-06`).
- **Giới hạn P1 (G-13):** không có số dư đầu kỳ và phát sinh theo từng lần thu vì ERD v7 không lưu lịch sử thanh toán; không đối chiếu phải trả NCC.
- **Ký:** Đại diện bên bán · Đại diện bên mua (xác nhận số liệu).

### 4.9 Phiếu thu / Phiếu chi (01-TT / 02-TT) · P2

- **Điều kiện:** cần bổ sung bảng thanh toán vào ERD (G-13, story `KT-10`). Bố cục chốt khi thiết kế bảng.
- **Dự kiến:** tiêu đề PHIẾU THU / PHIẾU CHI; người nộp/nhận tiền (`Parties`); lý do (thu tiền bán, thu tiền thuê, thu cọc, thu phạt, hoàn cọc, hoàn tiền trả hàng, chi trả NCC); chứng từ gốc; số tiền + bằng chữ; hình thức (tiền mặt / chuyển khoản); phiếu đã huỷ in dấu "ĐÃ HUỶ".
- **Ký:** Giám đốc · Kế toán trưởng · Người lập phiếu · Thủ quỹ · Người nộp/nhận tiền.

### 4.10 Phiếu trả hàng bán / đổi hàng + Phiếu bảo hành (TH — nội bộ) · P2

- **Tiêu đề theo `Return_Type`:** `REFUND` → "PHIẾU TRẢ HÀNG", `EXCHANGE` → "PHIẾU ĐỔI HÀNG", `WARRANTY` → "PHIẾU TIẾP NHẬN BẢO HÀNH".
- **Đầu phiếu:** `Receipt_Code`, kho tiếp nhận, phiếu xuất gốc (`Export_Receipt_ID`), người trả (`Party_ID`), `Return_Date`, `Days_Since_Delivery`, bậc phí áp dụng (`Policy_ID` → `Return_Fee_Policy.Tier_Name`, `Fee_Percent`).
- **Chi tiết** (`Sales_Return_Details`): SP, `Serial`, `Quantity`, `Return_Condition` (RESELLABLE / DAMAGED / TO_WARRANTY), `Restock_Status`.
- **Tổng:** `Original_Value`, `Fee_Amount`, `Refund_Amount`; phiếu xuất hàng đổi `New_Export_Receipt_ID` (nếu có).
- **Phiếu bảo hành** (in kèm phiếu xuất hoặc riêng): SP, serial, ngày bán, `Warranty_Months`, `Warranty_Expiry`, điều kiện bảo hành.
- **Ký:** Khách hàng · Bán hàng · Quản lý duyệt.

### 4.11 Phiếu chuyển kho (CK — kiêm vận chuyển nội bộ) · P2

- **Đầu phiếu:** `Transfer_Code`, kho xuất (`From_Warehouse_ID`) + địa chỉ, kho nhận (`To_Warehouse_ID`) + địa chỉ, `Approved_At`, `Shipped_At`, `Received_At`, `Status`, `Note` (phương tiện, lý do).
- **Chi tiết** (`Stock_Transfer_Details`): SP, `Serial`, `Qty_Sent` (xuất), `Qty_Received` (thực nhận), chênh lệch, `Note`.
- **Ký:** Người lập (`Created_By`) · Quản lý duyệt (`Approved_By`) · Thủ kho xuất · Người vận chuyển · Thủ kho nhận (`Received_By`).

---

## 5. Câu hỏi mở (cần chốt)

| #   | Câu hỏi                                                                                   | Ảnh hưởng                                      |
| --- | ----------------------------------------------------------------------------------------- | ---------------------------------------------- |
| Q1  | Doanh nghiệp áp dụng chế độ kế toán nào (TT200 / TT133 / TT99-2025)?                      | Mẫu số và bố cục mẫu 1, 2, 6, 9                |
| Q2  | Phiếu chuyển kho dùng mẫu "Phiếu xuất kho kiêm vận chuyển nội bộ" theo quy định hoá đơn hay mẫu nội bộ? | Mẫu 11 (P2, cùng lúc với `TK-17`) |
