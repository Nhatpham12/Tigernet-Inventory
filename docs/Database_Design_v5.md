# THIẾT KẾ CƠ SỞ DỮ LIỆU – HỆ THỐNG QUẢN LÝ KHO HÀNG (TIGERNET)

> Tài liệu này được xây dựng bám sát **SRS_He_Thong_Quan_Ly_Kho.docx** và kế thừa danh mục/dữ liệu mẫu từ **Database_Design.docx**.
> DBMS: **MySQL 5.7+**, charset `utf8mb4`, collation `utf8mb4_unicode_ci` (theo mục 1.4 & 8.4 SRS, và cấu hình trong file phân tích cũ).
> Mọi quyết định thiết kế đều tham chiếu tới mục cụ thể trong SRS (ký hiệu §).

**Quy ước ký hiệu tính chất thuộc tính:**
- **Đơn (simple):** thuộc tính nguyên tử, không tách nhỏ.
- **Kép (composite):** thuộc tính gộp nhiều thành phần con (VD: Địa chỉ = số nhà + đường + tỉnh).
- **Dẫn xuất (derived):** giá trị tính được từ thuộc tính khác, không nhập trực tiếp.
- **Đa trị (multivalued):** một thực thể có nhiều giá trị cho thuộc tính (được tách thành bảng con).

---

# PHẦN 1: THIẾT KẾ CÁC THỰC THỂ CHÍNH

Mỗi thực thể dưới đây kèm **lý do tồn tại (tham chiếu SRS)** và bảng thuộc tính có ghi rõ **tính chất** và **ràng buộc**.

## 1.1. Users – Người dùng hệ thống

**Lý do tồn tại:** §3.1.1 (đăng nhập bằng username/password, khóa tài khoản sau 5 lần sai, OTP email), §3.1.2 (quản trị viên quản lý người dùng, kiểm tra lượng truy cập, kiểm tra người thực hiện hành động), §1.3 (5 vai trò: Thủ kho, Kế toán, NV bán hàng, Nhà quản lý, Quản trị hệ thống), §4.2 (mã hóa mật khẩu bcrypt). Bảng `Users` cũ chỉ có 4 cột nên được mở rộng để phủ đủ các yêu cầu này.

> **Thiết kế quan trọng — dữ liệu ephemeral đẩy sang Redis:** Các thông tin *đếm lần đăng nhập sai*, *thời điểm mở khóa* (§3.1.1 khóa 5 phút sau 5 lần sai) và *đếm lượt truy cập/ngày* (§4.2 chống flooding) **không lưu vào MySQL**. Chúng thay đổi liên tục theo mỗi request, không cần lịch sử, và nếu ghi vào bảng `Users` sẽ tạo áp lực UPDATE lớn ảnh hưởng hiệu năng (§4.1). Thay vào đó dùng **Redis với TTL tự hết hạn**:
> - Khóa đăng nhập: key `login_fail:{username}` — tăng đếm mỗi lần sai, TTL 5 phút; khi đạt 5 thì đặt cờ khóa. Redis tự xóa sau 5 phút, không cần job dọn.
> - Rate limit: key `rate_limit:{user_id}:{yyyymmdd}` — tăng theo mỗi request, TTL hết ngày (§4.2, §3.1.2 kiểm tra lượng truy cập).
>
> Nếu không dùng Redis, có thể tách một bảng phụ `Login_Attempts` riêng — nhưng Redis là lựa chọn tối ưu và được ưu tiên. Cột trong `Users` dưới đây đã loại bỏ 3 thuộc tính ephemeral.

| Thuộc tính | Tính chất | Ràng buộc | Lý do (tham chiếu SRS) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT, NOT NULL | Khóa chính chuẩn (§6.1.1) |
| Username | Đơn | UNIQUE, NOT NULL | Tên đăng nhập không trùng (§3.1.1, §6.1.1) |
| Password_Hash | Đơn | NOT NULL | Mật khẩu mã hóa bcrypt, không lưu plaintext (§3.1.1, §4.2) |
| Full_Name | Đơn | NULL | Tên hiển thị người thực hiện hành động (§3.1.2) |
| Email | Đơn | UNIQUE, NULL | Xác thực & OTP qua Gmail/mail công ty (§3.1.1) |
| Role_ID | Đơn, FK | FK→Roles.ID, NOT NULL | Điều hướng theo vai trò sau đăng nhập (§3.1.1, §1.3) |
| Is_Active | Đơn | NOT NULL, DEFAULT TRUE | Vô hiệu hóa tài khoản thay vì xóa cứng |
| Created_At | Đơn | NOT NULL, DEFAULT NOW | Nhật ký tạo |

## 1.2. Roles – Vai trò (phân quyền)

**Lý do tồn tại:** §1.3 định nghĩa 5 vai trò với quyền hạn khác nhau; §3.1.1 “điều hướng theo vai trò”; §1.2 Phase 2 “hệ thống phân quyền”. File cũ ghi chú không tách bảng Roles vì hiện chỉ 1 loại user, nhưng SRS yêu cầu rõ nhiều vai trò → **tách bảng Roles** để chuẩn hóa.

> **Về đề xuất dùng ENUM cho `Role_Name`:** khuyến nghị **không dùng ENUM**, giữ bảng `Roles`. Lý do: (1) §1.2 Phase 2 nói rõ sẽ mở rộng phân quyền, mà sửa ENUM phải chạy `ALTER TABLE` (khóa bảng, tốn kém) mỗi lần thêm/đổi vai trò; (2) bảng `Roles` cho phép gắn thêm mô tả và về sau nối bảng `Permissions` (quan hệ N–N) dễ dàng, ENUM thì không; (3) ENUM chỉ hơn ở việc bớt 1 phép join, nhưng bảng `Roles` chỉ ~5 dòng nên được MySQL cache toàn bộ, chi phí gần như bằng 0. ENUM chỉ nên dùng cho danh sách thực sự bất biến (VD: giới tính) — vai trò không thuộc nhóm đó.

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| Role_Name | Đơn | UNIQUE, NOT NULL | Tên vai trò (WAREHOUSE, ACCOUNTANT, SALES, MANAGER, ADMIN) (§1.3) |
| Description | Đơn | NULL | Mô tả quyền hạn (§1.3) |

## 1.3. Product_Groups – Nhóm sản phẩm

**Lý do tồn tại:** §3.2.1 (lọc sản phẩm theo danh mục), §6.1.2. Kế thừa 12 nhóm mẫu từ Database_Design.docx.

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính (§6.1.2) |
| Group_Name | Đơn | UNIQUE, NOT NULL | Tên nhóm không trùng (§6.1.2) |
| Is_Deleted | Đơn | NOT NULL, DEFAULT FALSE | Hỗ trợ xóa mềm danh mục (§3.2.1) |

## 1.4. Products – Danh mục sản phẩm

**Lý do tồn tại:** §3.2.1 (thêm/sửa/xóa mềm; thông tin bắt buộc: Tên, Đơn vị tính, Giá nhập, Giá bán, Danh mục; thông tin tùy chọn: Ảnh, Serial, Người bán; cảnh báo tồn dưới ngưỡng tối thiểu; bảo hành theo mã SP với hàng không serial §2.3/§3.2.1). Bảng `Products` cũ chỉ có 2 cột được mở rộng theo đúng §3.2.1.

> Lưu ý thiết kế: Ngày nhập/xuất và trạng thái **của từng đơn vị hàng** thuộc về vòng đời tồn kho (bảng `Inventory`), không thuộc danh mục sản phẩm. `Products` mô tả *loại* sản phẩm; giá ở đây là giá niêm yết mặc định.

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính (§6.1.3) |
| Product_Name | Đơn | NOT NULL | Tên SP – bắt buộc (§3.2.1) |
| Group_ID | Đơn, FK | FK→Product_Groups.ID, NOT NULL | Danh mục – bắt buộc (§3.2.1, §6.1.3) |
| Unit | Đơn | NOT NULL | Đơn vị tính – bắt buộc (§3.2.1) |
| Default_Price_In | Đơn | NOT NULL, DEFAULT 0, CHECK ≥0 | Giá nhập niêm yết – bắt buộc (§3.2.1) |
| Default_Price_Out | Đơn | NOT NULL, DEFAULT 0, CHECK ≥0 | Giá bán biểu kiến – bắt buộc (§3.2.1, §1.3) |
| Is_Serialized | Đơn | NOT NULL, DEFAULT TRUE | Phân biệt hàng có/không serial để tra bảo hành theo serial hoặc mã SP (§2.3, §3.2.1) |
| Min_Stock_Level | Đơn | NOT NULL, DEFAULT 0 | Ngưỡng tồn tối thiểu để cảnh báo (§3.2.1, §3.8) |
| Image_Path | Đơn, tham chiếu | NULL | Đường dẫn/khóa ảnh SP (1 ảnh) – tùy chọn (§3.2.1) |
| Is_Deleted | Đơn | NOT NULL, DEFAULT FALSE | Xóa mềm (§3.2.1) |

*Ràng buộc bảng:* `UNIQUE (Product_Name, Group_ID)` – kế thừa từ schema cũ (không trùng tên SP trong cùng nhóm).

> **Về thuộc tính "Người bán" (Seller) — đã chuyển khỏi `Products`:** theo yêu cầu mới, "người bán hàng vào kho" có thể là nhà cung cấp, khách bán lẻ (có thể cần Căn cước), hoặc khách đã mua nay đổi/trả. Vì là một trong ba loại đối tác khác nhau, nó được mô hình hóa bằng khóa ngoại **trỏ về `Parties`** (không trỏ riêng `Suppliers`). Vì mỗi phiếu nhập chỉ từ một người bán, thuộc tính này rời `Products` và được đặt ở **`Import_Receipts.Source_Party_ID`** (header phiếu nhập) — đây là *nguồn nhập của cả phiếu*. Cách này thay cho `Supplier_ID` cũ (vốn NOT NULL và chỉ nhận NCC có hồ sơ, nên không ghi được người bán lẻ/khách đổi trả). Lịch sử nguồn nhập của từng loại SP truy được qua chuỗi `Import_Receipts.Source_Party_ID → Import_Receipt_Details → Inventory`.

> **Lưu trữ file tham chiếu (ảnh SP):** không lưu binary ảnh vào MySQL. **File ảnh** đặt trên object storage (MinIO/S3/GCS) hoặc file server; DB chỉ giữ **đường dẫn/object key** trong `Image_Path`. Mỗi SP chỉ 1 ảnh nên giữ 1 cột path là đủ.

## 1.5. Parties – Đối tác (thực thể cha hợp nhất con người/tổ chức)

**Lý do tồn tại:** yêu cầu mới xác định "người bán hàng vào kho" có thể là một trong ba loại đối tượng — nhà cung cấp, khách bán lẻ (có thể cần Căn cước), hoặc khách đã mua nay đổi/trả. Ba loại này thực chất là các **vai trò** của cùng một khái niệm gốc: "con người/tổ chức mà công ty giao dịch". Vì vậy đưa vào thực thể cha **Parties** để mọi nơi cần trỏ tới "một đối tác bất kỳ" (đặc biệt là *người bán*) chỉ cần một khóa ngoại thật tới `Parties`, thay cho quan hệ đa hình. `Suppliers` và `Customers` trở thành hồ sơ vai trò gắn vào một Party (§3.3.2, §3.4.1). Một Party có thể vừa là NCC vừa là khách, hoặc chỉ là khách vãng lai chưa gắn vai trò.

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS / yêu cầu mới) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| Party_Type | Đơn | NOT NULL | INDIVIDUAL (cá nhân) / ORGANIZATION (tổ chức) – phân biệt để biết khi nào cần Căn cước |
| Full_Name | Đơn | NOT NULL | Tên đối tác/cá nhân/cơ sở |
| Tax_Code | Đơn | UNIQUE, NULL | Mã số thuế (tổ chức) |
| National_ID | Đơn | NULL | Số Căn cước (áp dụng khách bán lẻ cá nhân, theo yêu cầu mới) |
| Address | Kép | NULL | Địa chỉ (số nhà/đường/tỉnh) |
| Phone | Đơn | NULL | Điện thoại |
| Email | Đơn | NULL | Email |
| Is_Deleted | Đơn | NOT NULL, DEFAULT FALSE | Xóa mềm |

## 1.6. Suppliers – Vai trò Nhà cung cấp của một Party

**Lý do tồn tại:** §3.3.2 (thông tin nhà cung cấp), §3.7.2 (nhập theo NCC). Sau khi đưa vào `Parties`, `Suppliers` giữ **các thuộc tính chỉ có ở vai trò nhà cung cấp** (hợp đồng, mã vận đơn); danh tính chung (tên, MST, địa chỉ, liên hệ) nằm ở `Parties`. Quan hệ 1–1 với `Parties`.

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS §3.3.2) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| Party_ID | Đơn, FK | FK→Parties.ID, UNIQUE, NOT NULL | Hồ sơ NCC của Party nào (1–1) |
| Contract_No | Đơn | NULL | Số hợp đồng/đơn đặt hàng |
| Tracking_Code | Đơn | NULL | Mã vận đơn |
| Is_Deleted | Đơn | NOT NULL, DEFAULT FALSE | Xóa mềm (§3.3.2) |

> **Thay đổi so với bản trước:** danh tính chung (tên, MST, địa chỉ, phone, email) chuyển lên `Parties`. Thông tin thanh toán vẫn ở bảng `Supplier_Bank_Accounts` (mục 1.6.1), chứng từ ở `Supplier_Documents` (mục 1.6.2).

## 1.6.1. Supplier_Bank_Accounts – Phương thức thanh toán của NCC

**Lý do tồn tại:** §3.3.2 (số tài khoản ngân hàng, mã thanh toán ngân hàng của NCC). Một NCC quản lý nhiều tài khoản. Thực thể yếu phụ thuộc `Suppliers`.

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS §3.3.2) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| Supplier_ID | Đơn, FK | FK→Suppliers.ID, NOT NULL | Thuộc NCC nào |
| Account_Name | Đơn | NOT NULL | Tên tài khoản (chủ TK) |
| Account_Number | Đơn | NOT NULL | Số tài khoản |
| Bank_Name | Đơn | NOT NULL | Tên ngân hàng |
| Bank_Payment_Code | Đơn | NULL | Mã thanh toán ngân hàng |

## 1.6.2. Supplier_Documents – Chứng từ đính kèm của NCC

**Lý do tồn tại:** §3.3.2 (phiếu xuất kho/biên bản bàn giao của NCC). File tham chiếu, nhiều bản → tách bảng, lưu path lên object storage.

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS §3.3.2) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| Supplier_ID | Đơn, FK | FK→Suppliers.ID, NOT NULL | Thuộc NCC nào |
| Doc_Type | Đơn | NULL | Loại chứng từ (biên bản bàn giao, phiếu xuất...) |
| File_Path | Đơn, tham chiếu | NOT NULL | Đường dẫn/object key tới file trên object storage |
| Uploaded_At | Đơn | NOT NULL, DEFAULT NOW | Thời điểm tải lên |

> **Lưu trữ file tham chiếu (chứng từ NCC):** file thật đặt trên object storage (MinIO/S3/GCS) hoặc file server, DB chỉ lưu path.

## 1.7. Customers – Vai trò Khách hàng của một Party

**Lý do tồn tại:** §3.4.1, §3.5.1 (khách hàng trên phiếu xuất & thuê); §3.7.2 (xuất theo KH); §3.7.4 (công nợ theo KH). Sau khi đưa vào `Parties`, `Customers` là hồ sơ vai trò khách hàng; danh tính chung nằm ở `Parties`. Quan hệ 1–1 với `Parties`.

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính (§6.1.4) |
| Party_ID | Đơn, FK | FK→Parties.ID, UNIQUE, NOT NULL | Hồ sơ khách hàng của Party nào (1–1) |
| Is_Deleted | Đơn | NOT NULL, DEFAULT FALSE | Xóa mềm |

> **Thay đổi so với bản trước:** tên/MST/địa chỉ/phone chuyển lên `Parties`; cột `Receiver` đã bỏ từ trước, người nhận ở bảng `Receivers` (mục 1.7.1).

## 1.7.1. Receivers – Người nhận hàng (thực thể yếu của Customers)

**Lý do tồn tại:** §3.4.1, §3.5.1 (thông tin người nhận trên phiếu). Chọn **Cách 2** (bảng người nhận 1–N) vì khách TIGERNET là doanh nghiệp giao dịch lặp lại, cần gợi ý và chọn nhanh, giảm nhập lại và trùng lặp.

**Kết hợp snapshot:** ngoài `Receiver_ID`, tại thời điểm tạo phiếu hệ thống **sao chép (snapshot)** tên/địa chỉ/điện thoại người nhận vào phiếu, để phiếu cũ giữ đúng dữ liệu lúc giao hàng (§1.3, §3.4.1).

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS §3.4.1) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| Customer_ID | Đơn, FK | FK→Customers.ID, NOT NULL | Thuộc khách hàng nào (1–N) |
| Receiver_Name | Đơn | NOT NULL | Tên người nhận |
| Address | Kép | NULL | Địa chỉ nhận hàng |
| Phone | Đơn | NULL | Điện thoại người nhận |
| Is_Deleted | Đơn | NOT NULL, DEFAULT FALSE | Xóa mềm |

## 1.8. VAT_Options – Tùy chọn VAT

**Lý do tồn tại:** §6.1.6, §2.2 (báo cáo thuế), phiếu xuất có thuế. Kế thừa: NO, 0%, 5%, 8%, 10%.

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính (§6.1.6) |
| VAT | Đơn | UNIQUE, NOT NULL | Giá trị VAT không trùng (§6.1.6) |
| Rate | Đơn, dẫn xuất | NULL | Giá trị số (%) suy ra từ VAT để tính tiền |

## 1.9. Payment_Statuses – Trạng thái thanh toán

**Lý do tồn tại:** §6.1.7, §3.7.4 (công nợ). Kế thừa: PAID, UNPAID, PARTIAL, RENTING, RETURNED, CANCELLED.

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính (§6.1.7) |
| Payment_Status | Đơn | UNIQUE, NOT NULL | Tên trạng thái không trùng (§6.1.7) |

## 1.10. Import_Receipts – Phiếu nhập (header)

**Lý do tồn tại:** §3.3.1 (tạo phiếu nhập từ NCC, tự tính tổng, lưu trạng thái, lưu theo thời gian, cho sửa có ánh xạ ngược tồn); §5.1 (UC-001); §7.1.4. Một phiếu có nhiều dòng SP → tách header/detail (chuẩn hóa 1-N).

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS §3.3.1) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính phiếu |
| Receipt_Code | Đơn | UNIQUE, NOT NULL | Mã phiếu để in/tra cứu |
| Source_Party_ID | Đơn, FK | FK→Parties.ID, NOT NULL | Nguồn nhập / "người bán" cả phiếu — trỏ về Party nên phủ NCC, khách bán lẻ, khách đổi-trả (yêu cầu mới) |
| Date_In | Đơn | NOT NULL | Ngày nhập |
| Total_Amount | Đơn, dẫn xuất | NOT NULL, DEFAULT 0 | Tự động tính tổng giá trị nhập |
| Status | Đơn | NOT NULL, DEFAULT 'DRAFT' | Lưu trạng thái: DRAFT/PENDING/CONFIRMED (§3.3.1, §7.1.4 “Lưu & Gửi duyệt”) |
| Note | Đơn | NULL | Ghi chú phiếu |
| Created_By | Đơn, FK | FK→Users.ID, NOT NULL | Người tạo (audit §3.1.2) |
| Created_At | Đơn | NOT NULL, DEFAULT NOW | Lưu theo thời gian |

## 1.11. Import_Receipt_Details – Chi tiết phiếu nhập

**Lý do tồn tại:** §3.3.1 “Danh sách sản phẩm (Tên SP, Danh mục, Số lượng, Giá nhập, Serial, Mã vận đơn)”. Đây là thực thể **yếu** phụ thuộc phiếu nhập.

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS §3.3.1) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính dòng |
| Import_Receipt_ID | Đơn, FK | FK→Import_Receipts.ID, NOT NULL | Thuộc phiếu nhập nào |
| Product_ID | Đơn, FK | FK→Products.ID, NOT NULL | Sản phẩm |
| Quantity | Đơn | NOT NULL, CHECK >0 | Số lượng nhập |
| Price_In | Đơn | NOT NULL, CHECK ≥0 | Giá nhập |
| Serial | Đơn | NULL | Serial (chỉ hàng serialized) |
| Tracking_Code | Đơn | NULL | Mã vận đơn |
| Line_Total | Đơn, dẫn xuất | NOT NULL | = Quantity × Price_In |

## 1.12. Export_Receipts – Phiếu xuất (header)

**Lý do tồn tại:** §3.4.1 (phiếu xuất khi bán/xuất, kiểm tra tồn, tự tính tổng, trạng thái Chờ xác nhận/Đã xác nhận/Đã xuất, in phiếu, cho sửa nếu chưa xuất); §5.2 (UC-002); §7.1.5.

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS §3.4.1) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| Receipt_Code | Đơn | UNIQUE, NOT NULL | Mã phiếu |
| Customer_ID | Đơn, FK | FK→Customers.ID, NOT NULL | Khách hàng |
| Receiver_ID | Đơn, FK | FK→Receivers.ID, NULL | Người nhận đã lưu được chọn (§3.4.1) |
| Receiver_Name_Snapshot | Đơn | NULL | Snapshot tên người nhận lúc tạo phiếu |
| Receiver_Address_Snapshot | Kép | NULL | Snapshot địa chỉ nhận lúc tạo phiếu |
| Receiver_Phone_Snapshot | Đơn | NULL | Snapshot điện thoại người nhận lúc tạo phiếu |
| Date_Out | Đơn | NOT NULL | Ngày xuất |
| Delivery_Date | Đơn | NULL | Ngày khách thực nhận hàng — mốc gốc tính thời hạn đổi/trả (quy tắc đổi trả) |
| VAT_ID | Đơn, FK | FK→VAT_Options.ID, NULL | Mức VAT áp dụng |
| Deposit | Đơn | NOT NULL, DEFAULT 0 | Số tiền đặt cọc |
| Total_Amount | Đơn, dẫn xuất | NOT NULL, DEFAULT 0 | Tự động tính tổng giá trị xuất |
| Payment_Status_ID | Đơn, FK | FK→Payment_Statuses.ID, NULL | Trạng thái thanh toán (công nợ §3.7.4) |
| Paid | Đơn | NOT NULL, DEFAULT 0 | Đã thanh toán |
| Remain | Đơn, dẫn xuất | NOT NULL, DEFAULT 0 | Còn lại = Total − Paid |
| Status | Đơn | NOT NULL, DEFAULT 'PENDING' | PENDING/CONFIRMED/EXPORTED (§3.4.1) |
| Note | Đơn | NULL | Ghi chú |
| Created_By | Đơn, FK | FK→Users.ID, NOT NULL | NV bán hàng – phục vụ doanh thu theo NV (§1.3, §3.7) |
| Created_At | Đơn | NOT NULL, DEFAULT NOW | Thời gian tạo |

## 1.13. Export_Receipt_Details – Chi tiết phiếu xuất

**Lý do tồn tại:** §3.4.1 (Tên SP, Số lượng, Giá bán, Serial, thời gian bảo hành).

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS §3.4.1) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính dòng |
| Export_Receipt_ID | Đơn, FK | FK→Export_Receipts.ID, NOT NULL | Thuộc phiếu xuất |
| Product_ID | Đơn, FK | FK→Products.ID, NOT NULL | Sản phẩm |
| Inventory_ID | Đơn, FK | FK→Inventory.ID, NULL | Đơn vị hàng cụ thể xuất ra (hàng serialized) |
| Quantity | Đơn | NOT NULL, CHECK >0 | Số lượng xuất |
| Price_Out | Đơn | NOT NULL, CHECK ≥0 | Giá bán |
| Serial | Đơn | NULL | Serial hàng xuất |
| Warranty_Months | Đơn | NULL | Thời gian bảo hành |
| Warranty_Expiry | Đơn, dẫn xuất | NULL | = Date_Out + Warranty_Months (§1.3 “thời gian bảo hành SP bán ra”) |
| Line_Total | Đơn, dẫn xuất | NOT NULL | = Quantity × Price_Out |

## 1.14. Rental_Receipts – Phiếu cho thuê (header)

**Lý do tồn tại:** §3.5.1 (phiếu thuê có thời hạn, cảnh báo trước 7 ngày, hàng thuê không tính vào tồn có sẵn, tạo phiếu trả, phạt quá hạn ≥10 ngày). Cho mượn (§2.3) dùng chung cấu trúc, phân biệt bằng `Rental_Type` (RENT/LEND).

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS §3.5.1) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| Receipt_Code | Đơn | UNIQUE, NOT NULL | Mã phiếu thuê |
| Customer_ID | Đơn, FK | FK→Customers.ID, NOT NULL | Khách thuê |
| Receiver_ID | Đơn, FK | FK→Receivers.ID, NULL | Người nhận đã lưu được chọn (§3.5.1) |
| Receiver_Name_Snapshot | Đơn | NULL | Snapshot tên người nhận lúc tạo phiếu |
| Receiver_Address_Snapshot | Kép | NULL | Snapshot địa chỉ nhận lúc tạo phiếu |
| Receiver_Phone_Snapshot | Đơn | NULL | Snapshot điện thoại người nhận lúc tạo phiếu |
| Rental_Type | Đơn | NOT NULL, DEFAULT 'RENT' | RENT (thuê) / LEND (mượn) (§2.3) |
| Date_Out | Đơn | NOT NULL | Ngày xuất cho thuê |
| Due_Date | Đơn | NOT NULL | Thời hạn thuê (dùng cảnh báo 7 ngày) |
| Deposit | Đơn | NOT NULL, DEFAULT 0 | Tiền đặt cọc (trừ vào công nợ §3.7.4) |
| Rental_Price_Per_Day | Đơn | NOT NULL, DEFAULT 0 | Cơ sở tính phạt quá hạn |
| Total_Rent | Đơn, dẫn xuất | NOT NULL, DEFAULT 0 | Tổng tiền thuê |
| Penalty_Amount | Đơn, dẫn xuất | NOT NULL, DEFAULT 0 | Phạt = số ngày quá hạn × giá thuê/ngày (khi quá hạn ≥10 ngày) |
| Return_Date | Đơn | NULL | Ngày khách trả thực tế |
| Payment_Status_ID | Đơn, FK | FK→Payment_Statuses.ID, NULL | RENTING/RETURNED... |
| Created_By | Đơn, FK | FK→Users.ID, NOT NULL | Người lập |
| Created_At | Đơn | NOT NULL, DEFAULT NOW | Thời gian |

## 1.15. Rental_Receipt_Details – Chi tiết phiếu cho thuê

**Lý do tồn tại:** §3.5.1 (Tên SP, Số lượng, Giá cho thuê, Serial). Serial hàng thuê trả về được ngoại lệ trùng lặp (ghi chú §6.1.8 bảng cũ).

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS §3.5.1) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| Rental_Receipt_ID | Đơn, FK | FK→Rental_Receipts.ID, NOT NULL | Thuộc phiếu thuê |
| Product_ID | Đơn, FK | FK→Products.ID, NOT NULL | Sản phẩm |
| Inventory_ID | Đơn, FK | FK→Inventory.ID, NULL | Đơn vị hàng cho thuê |
| Quantity | Đơn | NOT NULL, CHECK >0 | Số lượng thuê |
| Rental_Price | Đơn | NOT NULL, CHECK ≥0 | Giá cho thuê |
| Serial | Đơn | NULL | Serial |
| Is_Returned | Đơn | NOT NULL, DEFAULT FALSE | Đã trả hay chưa (§3.7.3) |

## 1.15.1. Return_Receipts – Phiếu trả hàng (thuê/mượn) (header)

**Lý do tồn tại:** §3.5.1 “tạo phiếu trả hàng thuê khi khách trả hàng”; §2.3 hàng cho mượn cũng cần nhận lại. Bổ sung theo yêu cầu. Một phiếu trả gắn với một phiếu thuê/mượn gốc, có thể trả nhiều dòng SP → tách header/detail. Khi trả, hàng được đưa lại vào tồn (§3.5.1 cập nhật tồn) và tính phạt nếu quá hạn.

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS §3.5.1) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| Receipt_Code | Đơn | UNIQUE, NOT NULL | Số phiếu trả |
| Rental_Receipt_ID | Đơn, FK | FK→Rental_Receipts.ID, NOT NULL | Phiếu thuê/mượn gốc |
| Return_Date | Đơn | NOT NULL | Ngày trả thực tế |
| Days_Overdue | Đơn, dẫn xuất | NOT NULL, DEFAULT 0 | Số ngày quá hạn (so Due_Date) |
| Penalty_Amount | Đơn, dẫn xuất | NOT NULL, DEFAULT 0 | Phạt = ngày quá hạn × giá thuê/ngày (khi ≥10 ngày, §3.5.1) |
| Note | Đơn | NULL | Ghi chú tình trạng hàng trả |
| Created_By | Đơn, FK | FK→Users.ID, NOT NULL | Người lập phiếu trả |
| Created_At | Đơn | NOT NULL, DEFAULT NOW | Thời gian |

## 1.15.2. Return_Receipt_Details – Chi tiết phiếu trả hàng

**Lý do tồn tại:** §3.5.1. Ghi từng SP/serial được khách trả về; dùng để cập nhật `Inventory.Status` về IN_STOCK.

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS §3.5.1) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| Return_Receipt_ID | Đơn, FK | FK→Return_Receipts.ID, NOT NULL | Thuộc phiếu trả |
| Rental_Detail_ID | Đơn, FK | FK→Rental_Receipt_Details.ID, NOT NULL | Dòng thuê gốc được trả |
| Product_ID | Đơn, FK | FK→Products.ID, NOT NULL | Sản phẩm |
| Inventory_ID | Đơn, FK | FK→Inventory.ID, NULL | Đơn vị hàng trả về kho |
| Quantity | Đơn | NOT NULL, CHECK >0 | Số lượng trả |
| Serial | Đơn | NULL | Serial hàng trả |
| Condition_Note | Đơn | NULL | Tình trạng hàng khi trả (nguyên vẹn/hư...) |

## 1.15.3. Sales_Returns – Phiếu đổi/trả hàng đã bán (header) [NGHIỆP VỤ MỚI]

**Lý do tồn tại:** yêu cầu mới bổ sung nghiệp vụ **đổi/trả hàng đã bán đứt** cho khách. Phân biệt rõ với `Return_Receipts` (mục 1.15.1) — cái đó là trả hàng *cho thuê/mượn* (hàng thuộc sở hữu công ty); còn đây là hàng *đã chuyển quyền sở hữu* cho khách, nay khách mang lại để trả (hoàn tiền) hoặc đổi sang SP khác. Nghiệp vụ này khép lại vòng "người bán = khách đã mua yêu cầu đổi/trả": người mang hàng tới trả là một **Party**, truy được về đúng khách đã mua nhờ liên kết phiếu xuất gốc.

**Phạm vi (thiết kế đầy đủ):** phủ cả (a) trả + hoàn tiền/giảm công nợ, (b) đổi sang SP khác có sinh phiếu xuất mới, (c) hàng lỗi chuyển bảo hành/hàng hư. Phí đổi/trả tính theo **chính sách bậc thời gian × loại thao tác** tra từ bảng `Return_Fee_Policy` (mục 1.15.4), mốc thời gian tính từ `Export_Receipts.Delivery_Date`.

| Thuộc tính | Tính chất | Ràng buộc | Lý do (yêu cầu mới) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| Receipt_Code | Đơn | UNIQUE, NOT NULL | Số phiếu đổi/trả |
| Export_Receipt_ID | Đơn, FK | FK→Export_Receipts.ID, NOT NULL | Phiếu xuất/bán gốc (xác minh SP, giá, serial, hạn đổi trả/bảo hành) |
| Party_ID | Đơn, FK | FK→Parties.ID, NOT NULL | Người mang hàng tới trả (khách đã mua) |
| Return_Type | Đơn | NOT NULL | REFUND (trả hẳn) / EXCHANGE (đổi hàng) / WARRANTY (bảo hành) |
| Exchange_Direction | Đơn | NULL | Với EXCHANGE: EQUAL_HIGHER (đổi mã ≥ giá) / LOWER (đổi mã < giá) – quyết định cột phí áp dụng |
| Return_Date | Đơn | NOT NULL | Ngày đổi/trả (dùng cùng Delivery_Date để tính số ngày) |
| Days_Since_Delivery | Đơn, dẫn xuất | NULL | = Return_Date − Delivery_Date; xác định bậc thời gian (1 tuần/2 tuần/1 tháng) |
| Policy_ID | Đơn, FK | FK→Return_Fee_Policy.ID, NULL | Bậc chính sách phí được áp (tra theo bậc thời gian + loại thao tác) |
| Original_Value | Đơn, dẫn xuất | NOT NULL, DEFAULT 0 | Tổng giá bán gốc của hàng trả (cơ sở tính phí) |
| Fee_Percent | Đơn, dẫn xuất | NOT NULL, DEFAULT 0 | Tỷ lệ phí áp dụng (0/10/20/30%) lấy từ Policy |
| Fee_Amount | Đơn, dẫn xuất | NOT NULL, DEFAULT 0 | Tiền phí = Original_Value × Fee_Percent |
| New_Export_Receipt_ID | Đơn, FK | FK→Export_Receipts.ID, NULL | Phiếu xuất mới sinh ra khi đổi hàng (case EXCHANGE) |
| Refund_Amount | Đơn, dẫn xuất | NOT NULL, DEFAULT 0 | Số tiền hoàn/giảm công nợ = Original_Value − Fee_Amount (± chênh lệch khi đổi) |
| Status | Đơn | NOT NULL, DEFAULT 'DRAFT' | DRAFT/CONFIRMED/COMPLETED/REJECTED |
| Note | Đơn | NULL | Ghi chú |
| Created_By | Đơn, FK | FK→Users.ID, NOT NULL | Nhân viên lập phiếu |
| Created_At | Đơn | NOT NULL, DEFAULT NOW | Thời gian |

> **Quy tắc nghiệp vụ (tầng ứng dụng/service):** (1) nếu `Days_Since_Delivery` > 1 tháng → **từ chối** đổi/trả, đặt Status = REJECTED (không có bậc phí). (2) Ngược lại, hệ thống tra `Return_Fee_Policy` theo bậc thời gian và `Return_Type`/`Exchange_Direction` để lấy `Fee_Percent`, rồi tính `Fee_Amount` và `Refund_Amount`. (3) Với đổi hàng, nếu mã mới cao hơn thì khách trả thêm phần chênh; nếu thấp hơn thì hoàn phần chênh sau khi trừ phí.

## 1.15.4. Return_Fee_Policy – Chính sách phí đổi/trả (bảng cấu hình) [NGHIỆP VỤ MỚI]

**Lý do tồn tại:** các con số mốc thời gian (1 tuần/2 tuần/1 tháng) và tỷ lệ phí (0/10/20/30%) là **chính sách kinh doanh có thể thay đổi**. Tách thành bảng cấu hình để khi đổi chính sách chỉ sửa dữ liệu, không sửa code. Mỗi dòng là một ô trong ma trận (bậc thời gian × loại thao tác → tỷ lệ phí).

| Thuộc tính | Tính chất | Ràng buộc | Lý do (yêu cầu mới) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| Tier_Name | Đơn | NOT NULL | Tên bậc (WITHIN_1_WEEK / WITHIN_2_WEEKS / WITHIN_1_MONTH) |
| Max_Days | Đơn | NOT NULL | Giới hạn ngày của bậc (7 / 14 / 30) – so với Days_Since_Delivery |
| Action_Type | Đơn | NOT NULL | REFUND / EXCHANGE_EQUAL_HIGHER / EXCHANGE_LOWER |
| Fee_Percent | Đơn | NOT NULL | Tỷ lệ phí áp dụng cho ô này |
| Is_Allowed | Đơn | NOT NULL, DEFAULT TRUE | Cho phép hay từ chối (dành cho mở rộng, VD sau 1 tháng = FALSE) |
| Effective_From | Đơn | NULL | Ngày chính sách có hiệu lực (hỗ trợ đổi chính sách theo thời gian) |

**Dữ liệu mặc định (đúng quy tắc sếp đưa):**

| Tier_Name | Max_Days | Action_Type | Fee_Percent |
|---|---|---|---|
| WITHIN_1_WEEK | 7 | REFUND | 0% |
| WITHIN_1_WEEK | 7 | EXCHANGE_EQUAL_HIGHER | 0% |
| WITHIN_1_WEEK | 7 | EXCHANGE_LOWER | 10% |
| WITHIN_2_WEEKS | 14 | REFUND | 20% |
| WITHIN_2_WEEKS | 14 | EXCHANGE_EQUAL_HIGHER | 0% |
| WITHIN_2_WEEKS | 14 | EXCHANGE_LOWER | 10% |
| WITHIN_1_MONTH | 30 | REFUND | 30% |
| WITHIN_1_MONTH | 30 | EXCHANGE_EQUAL_HIGHER | 10% |
| WITHIN_1_MONTH | 30 | EXCHANGE_LOWER | 20% |

*Sau 30 ngày: không có dòng nào khớp → ứng dụng từ chối đổi/trả (hoặc dùng Is_Allowed=FALSE nếu muốn ghi tường minh).*

## 1.15.5. Sales_Return_Details – Chi tiết phiếu đổi/trả hàng bán [NGHIỆP VỤ MỚI]

**Lý do tồn tại:** ghi từng SP/serial khách trả lại; quyết định hàng quay lại kho ở trạng thái nào (bán lại được / hàng lỗi / chờ bảo hành).

| Thuộc tính | Tính chất | Ràng buộc | Lý do (yêu cầu mới) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| Sales_Return_ID | Đơn, FK | FK→Sales_Returns.ID, NOT NULL | Thuộc phiếu đổi/trả |
| Export_Detail_ID | Đơn, FK | FK→Export_Receipt_Details.ID, NOT NULL | Dòng bán gốc được trả |
| Product_ID | Đơn, FK | FK→Products.ID, NOT NULL | Sản phẩm trả |
| Inventory_ID | Đơn, FK | FK→Inventory.ID, NULL | Đơn vị hàng nhập lại kho |
| Quantity | Đơn | NOT NULL, CHECK >0 | Số lượng trả |
| Serial | Đơn | NULL | Serial hàng trả |
| Return_Condition | Đơn | NOT NULL | RESELLABLE (bán lại) / DAMAGED (hàng lỗi) / TO_WARRANTY (chờ bảo hành) |
| Restock_Status | Đơn | NULL | Trạng thái tồn kho sau khi nhập lại (IN_STOCK/DAMAGED...) |

## 1.16. Inventory – Tồn kho (vòng đời từng đơn vị/lô hàng)

**Lý do tồn tại:** §2.3, §3.2.1 (hiển thị tồn hiện tại), §3.4.1 (kiểm tra tồn trước xuất), §3.5.1 (hàng thuê không tính tồn có sẵn). Giữ vai trò **bảng trung tâm** như file cũ nhưng gắn với chứng từ nhập/xuất thay vì gộp tất cả vào 1 dòng.

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính (§6.1.8) |
| Product_ID | Đơn, FK | FK→Products.ID, NOT NULL | Sản phẩm (§6.1.8) |
| Serial | Đơn | NULL (UNIQUE có điều kiện) | Serial – không trùng, ngoại lệ hàng thuê trả về (§6.1.8) |
| Import_Detail_ID | Đơn, FK | FK→Import_Receipt_Details.ID, NULL | Nguồn nhập (ánh xạ ngược tồn khi sửa phiếu §3.3.1) |
| Quantity_On_Hand | Đơn, dẫn xuất | NOT NULL, DEFAULT 0 | Tồn thực có (cho hàng non-serialized) |
| Status | Đơn | NOT NULL, DEFAULT 'IN_STOCK' | IN_STOCK/SOLD/RENTED/LENT/DAMAGED/RESERVED – trạng thái hàng (§3.2.1, §1.3 “hold”) |
| Price_In | Đơn | NULL | Giá nhập của đơn vị (§6.1.8) |
| Note | Đơn | NULL | Ghi chú tình trạng hàng (§6.1.8 Note_In) |
| Date_In | Đơn | NULL | Ngày nhập (§6.1.8) |
| Date_Out | Đơn | NULL | Ngày xuất (§6.1.8) |

## 1.17. Stocktakes – Phiếu kiểm kê (header)

**Lý do tồn tại:** §3.6.1 (tạo phiếu kiểm kê, nhập số thực tế, so sánh lý thuyết/thực tế, tính chênh lệch, tự tạo phiếu điều chỉnh).

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS §3.6.1) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| Stocktake_Code | Đơn | UNIQUE, NOT NULL | Mã phiếu kiểm kê |
| Stocktake_Date | Đơn | NOT NULL | Ngày kiểm kê |
| Status | Đơn | NOT NULL, DEFAULT 'DRAFT' | DRAFT/COMPLETED |
| Created_By | Đơn, FK | FK→Users.ID, NOT NULL | Người kiểm kê |

## 1.18. Stocktake_Details – Chi tiết kiểm kê

**Lý do tồn tại:** §3.6.1 (từng SP: số lý thuyết, số thực tế, chênh lệch).

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS §3.6.1) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| Stocktake_ID | Đơn, FK | FK→Stocktakes.ID, NOT NULL | Thuộc phiếu kiểm kê |
| Product_ID | Đơn, FK | FK→Products.ID, NOT NULL | Sản phẩm |
| System_Qty | Đơn | NOT NULL | Số lượng lý thuyết trong hệ thống |
| Actual_Qty | Đơn | NOT NULL | Số lượng thực tế nhập tay |
| Difference | Đơn, dẫn xuất | NOT NULL | = Actual − System (hụt/thừa) |

## 1.19. Stock_Adjustments – Phiếu điều chỉnh tồn

**Lý do tồn tại:** §3.6.1 (tự tạo phiếu điều chỉnh khi có chênh lệch, cập nhật tồn theo điều chỉnh).

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS §3.6.1) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| Stocktake_ID | Đơn, FK | FK→Stocktakes.ID, NOT NULL | Sinh từ phiếu kiểm kê nào |
| Product_ID | Đơn, FK | FK→Products.ID, NOT NULL | Sản phẩm điều chỉnh |
| Adjust_Qty | Đơn | NOT NULL | Lượng điều chỉnh (±) |
| Reason | Đơn | NULL | Lý do chênh lệch |
| Created_At | Đơn | NOT NULL, DEFAULT NOW | Thời gian |

## 1.20. Alerts – Cảnh báo & thông báo

**Lý do tồn tại:** §3.8 (cảnh báo tồn dưới ngưỡng, sắp hết hạn thuê, phiếu chờ duyệt quá lâu, đếm cảnh báo chưa xử lý); §7.1.2 (danh sách cảnh báo trên dashboard).

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS §3.8) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| Alert_Type | Đơn | NOT NULL | LOW_STOCK / RENT_DUE / PENDING_APPROVAL / DEBT_OVERDUE |
| Message | Đơn | NOT NULL | Nội dung cảnh báo |
| Ref_Table | Đơn | NULL | Bảng liên quan (Products/Rental...) |
| Ref_ID | Đơn | NULL | ID bản ghi liên quan |
| Is_Resolved | Đơn | NOT NULL, DEFAULT FALSE | Đếm cảnh báo chưa xử lý (§3.8) |
| Created_At | Đơn | NOT NULL, DEFAULT NOW | Thời gian phát sinh |

## 1.21. Audit_Logs – Nhật ký hoạt động

**Lý do tồn tại:** §3.1.2 (kiểm tra người thực hiện hành động), §4.2 (ghi log mọi hoạt động quan trọng), §4.4 (log lỗi để debug), §1.2 Phase 2 (backup theo từng giao dịch).

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS) |
|---|---|---|---|
| ID | Đơn, định danh | PK, AUTO_INCREMENT | Khóa chính |
| User_ID | Đơn, FK | FK→Users.ID, NULL | Ai thực hiện (§3.1.2) |
| Action | Đơn | NOT NULL | Hành động (LOGIN/CREATE_IMPORT...) |
| Target_Table | Đơn | NULL | Đối tượng tác động |
| Target_ID | Đơn | NULL | ID bản ghi tác động |
| Detail | Đơn | NULL | Chi tiết (JSON) |
| Created_At | Đơn | NOT NULL, DEFAULT NOW | Thời điểm |

## 1.22. Stock_Summary – Bảng tổng hợp tồn kho (cache đọc nhanh)

**Lý do tồn tại:** §3.2.1 (hiển thị tồn hiện tại mỗi SP), §7.1.2 (dashboard tổng tồn), §4.1 (tải trang ≤2.5s, phản hồi API ≤500ms). Do `Inventory` lưu theo từng đơn vị/lô, mỗi lần hiển thị tồn phải `SUM` toàn bộ — chậm khi dữ liệu lớn. Bảng này là **cache phi chuẩn hóa (denormalized)** lưu sẵn tổng số lượng theo sản phẩm để đọc tức thì.

> **Đánh đổi cần lưu ý:** đây là dữ liệu **dẫn xuất trùng lặp** — phải được cập nhật đồng bộ mỗi khi có nhập/xuất/thuê/trả/điều chỉnh, tốt nhất trong **cùng transaction** với nghiệp vụ đó (hoặc bằng trigger). Rủi ro là lệch số nếu có luồng quên cập nhật → cần một **job đối soát định kỳ** tính lại từ `Inventory` để tự chữa. Chỉ nên coi đây là cache đọc, nguồn sự thật vẫn là `Inventory`.

| Thuộc tính | Tính chất | Ràng buộc | Lý do (SRS) |
|---|---|---|---|
| Product_ID | Đơn, FK | PK, FK→Products.ID | Khóa chính đồng thời tham chiếu SP (1 dòng/SP) |
| Qty_On_Hand | Đơn, dẫn xuất | NOT NULL, DEFAULT 0 | Tồn khả dụng (đang IN_STOCK) (§3.2.1) |
| Qty_Reserved | Đơn, dẫn xuất | NOT NULL, DEFAULT 0 | Hàng đang hold/chờ xuất (§1.3) |
| Qty_Rented | Đơn, dẫn xuất | NOT NULL, DEFAULT 0 | Hàng đang cho thuê/mượn (§3.5.1) |
| Qty_Damaged | Đơn, dẫn xuất | NOT NULL, DEFAULT 0 | Hàng hư hỏng (§2.3) |
| Last_Updated | Đơn | NOT NULL, DEFAULT NOW | Lần cập nhật cuối (phục vụ đối soát) |

---

# PHẦN 2: QUAN HỆ GIỮA CÁC THỰC THỂ

| # | Quan hệ | Bản số | Bắt buộc | Diễn giải & tham chiếu SRS |
|---|---|---|---|---|
| R1 | Roles — Users | 1 : N | Bắt buộc phía Users | Một vai trò có nhiều người dùng; mỗi user có đúng 1 vai trò (§1.3, §3.1.1) |
| R2 | Product_Groups — Products | 1 : N | Bắt buộc phía Products | Một nhóm có nhiều SP (§6.3, §3.2.1) |
| R3 | Products — Inventory | 1 : N | Bắt buộc | Một SP có nhiều dòng tồn (§6.3, §2.3) |
| R4 | Parties — Import_Receipts (nguồn nhập) | 1 : N | Bắt buộc phía phiếu | Nguồn nhập/"người bán" cả phiếu là một Party (NCC/khách lẻ/khách đổi-trả) (§3.3.2, yêu cầu mới) |
| R5 | Import_Receipts — Import_Receipt_Details | 1 : N | Bắt buộc (composition) | Phiếu nhập gồm nhiều dòng SP (§3.3.1) |
| R6 | Products — Import_Receipt_Details | 1 : N | Bắt buộc | Mỗi dòng nhập ứng 1 SP (§3.3.1) |
| R7 | Import_Receipt_Details — Inventory | 1 : N | Tùy chọn | Dòng nhập sinh ra bản ghi tồn (ánh xạ ngược khi sửa phiếu §3.3.1) |
| R8 | Customers — Export_Receipts | 1 : N | Bắt buộc phía phiếu | Một KH có nhiều phiếu xuất (§3.4.1, §3.7.2) |
| R9 | Export_Receipts — Export_Receipt_Details | 1 : N | Bắt buộc (composition) | Phiếu xuất gồm nhiều dòng (§3.4.1) |
| R10 | Products — Export_Receipt_Details | 1 : N | Bắt buộc | Mỗi dòng xuất ứng 1 SP (§3.4.1) |
| R11 | Inventory — Export_Receipt_Details | 1 : N | Tùy chọn | Đơn vị hàng serialized được xuất (§3.4.1 kiểm tra tồn) |
| R12 | VAT_Options — Export_Receipts | 1 : N | Tùy chọn | Mức VAT cho phiếu xuất (§6.1.6, §2.2) |
| R13 | Payment_Statuses — Export_Receipts | 1 : N | Tùy chọn | Trạng thái công nợ phiếu xuất (§3.7.4) |
| R14 | Customers — Rental_Receipts | 1 : N | Bắt buộc phía phiếu | KH thuê nhiều lần (§3.5.1) |
| R15 | Rental_Receipts — Rental_Receipt_Details | 1 : N | Bắt buộc (composition) | Phiếu thuê gồm nhiều dòng (§3.5.1) |
| R16 | Products — Rental_Receipt_Details | 1 : N | Bắt buộc | Mỗi dòng thuê ứng 1 SP (§3.5.1) |
| R17 | Inventory — Rental_Receipt_Details | 1 : N | Tùy chọn | Đơn vị hàng cho thuê, không tính vào tồn có sẵn (§3.5.1) |
| R18 | Payment_Statuses — Rental_Receipts | 1 : N | Tùy chọn | RENTING/RETURNED (§3.7.3) |
| R19 | Stocktakes — Stocktake_Details | 1 : N | Bắt buộc (composition) | Phiếu kiểm kê gồm nhiều dòng SP (§3.6.1) |
| R20 | Products — Stocktake_Details | 1 : N | Bắt buộc | Kiểm kê từng SP (§3.6.1) |
| R21 | Stocktakes — Stock_Adjustments | 1 : N | Tùy chọn | Chênh lệch sinh phiếu điều chỉnh (§3.6.1) |
| R22 | Products — Stock_Adjustments | 1 : N | Bắt buộc | Điều chỉnh theo SP (§3.6.1) |
| R23 | Users — (Import/Export/Rental/Stocktake) | 1 : N | Bắt buộc phía phiếu | Người tạo phiếu, phục vụ doanh thu theo NV & audit (§1.3, §3.1.2, §3.7) |
| R24 | Users — Audit_Logs | 1 : N | Tùy chọn | Ghi hành động của user (§3.1.2, §4.2) |
| R25 | *(đã loại bỏ — Inventory không còn Type_In/Type_Out; loại giao dịch nằm ở tầng chứng từ)* | — | — | Xem R47 (thuê/mượn) và các phiếu nhập/xuất |
| R26 | Suppliers — Supplier_Bank_Accounts | 1 : N | Tùy chọn (composition) | Một NCC có nhiều tài khoản thanh toán (§3.3.2) |
| R27 | Suppliers — Supplier_Documents | 1 : N | Tùy chọn (composition) | Một NCC có nhiều chứng từ đính kèm (§3.3.2) |
| R28 | Customers — Receivers | 1 : N | Tùy chọn (composition) | Một KH có nhiều người nhận (§3.4.1, Cách 2) |
| R29 | Receivers — Export_Receipts | 1 : N | Tùy chọn | Người nhận được chọn cho phiếu xuất (§3.4.1) |
| R30 | Receivers — Rental_Receipts | 1 : N | Tùy chọn | Người nhận được chọn cho phiếu thuê (§3.5.1) |
| R31 | Rental_Receipts — Return_Receipts | 1 : N | Tùy chọn | Một phiếu thuê/mượn có thể có phiếu trả (§3.5.1) |
| R32 | Return_Receipts — Return_Receipt_Details | 1 : N | Bắt buộc (composition) | Phiếu trả gồm nhiều dòng SP (§3.5.1) |
| R33 | Rental_Receipt_Details — Return_Receipt_Details | 1 : N | Bắt buộc | Mỗi dòng trả ứng dòng thuê gốc (§3.5.1) |
| R34 | Products — Stock_Summary | 1 : 1 | Bắt buộc | Mỗi SP có đúng 1 dòng tổng hợp tồn (§3.2.1, §7.1.2) |
| R35 | Parties — Suppliers | 1 : 1 | Bắt buộc phía Suppliers | Suppliers là vai trò NCC của một Party (yêu cầu mới) |
| R36 | Parties — Customers | 1 : 1 | Bắt buộc phía Customers | Customers là vai trò KH của một Party (yêu cầu mới) |
| R37 | *(đã gộp vào R4 — nguồn nhập chuyển lên header Import_Receipts.Source_Party_ID)* | — | — | Xem R4 |
| R38 | Export_Receipts — Sales_Returns | 1 : N | Bắt buộc phía phiếu trả | Phiếu đổi/trả tham chiếu phiếu bán gốc (nghiệp vụ mới) |
| R39 | Parties — Sales_Returns | 1 : N | Bắt buộc phía phiếu trả | Người mang hàng tới trả (khách đã mua) (nghiệp vụ mới) |
| R40 | Sales_Returns — Sales_Return_Details | 1 : N | Bắt buộc (composition) | Phiếu đổi/trả gồm nhiều dòng SP (nghiệp vụ mới) |
| R41 | Export_Receipt_Details — Sales_Return_Details | 1 : N | Bắt buộc | Mỗi dòng trả ứng dòng bán gốc (nghiệp vụ mới) |
| R42 | Products — Sales_Return_Details | 1 : N | Bắt buộc | Mỗi dòng trả ứng 1 SP (nghiệp vụ mới) |
| R43 | Inventory — Sales_Return_Details | 1 : N | Tùy chọn | Hàng trả nhập lại 1 đơn vị tồn (nghiệp vụ mới) |
| R44 | Sales_Returns — Export_Receipts (đổi hàng) | 1 : N | Tùy chọn | Phiếu xuất mới sinh khi đổi hàng, qua New_Export_Receipt_ID (nghiệp vụ mới) |
| R45 | Users — Sales_Returns | 1 : N | Bắt buộc phía phiếu | Nhân viên lập phiếu đổi/trả (§3.1.2 audit) |
| R46 | Return_Fee_Policy — Sales_Returns | 1 : N | Tùy chọn | Bậc chính sách phí được áp cho phiếu đổi/trả (quy tắc đổi trả) |
| R47 | *(đã loại bỏ — Transaction_Types bị xóa; loại thuê/mượn ghi trực tiếp bằng Rental_Type)* | — | — | Xem Rental_Receipts.Rental_Type |

---

# PHẦN 3: MÔ TẢ THIẾT KẾ ERD

Sơ đồ ERD được tổ chức quanh **hai trục trung tâm**: nhóm **Chứng từ giao dịch** và bảng **Inventory (tồn kho)**. Dưới đây là mô tả chi tiết (không vẽ hình) để có thể dựng ERD.

**Nhóm danh mục nền (lookup):** `Roles`, `Product_Groups`, `VAT_Options`, `Payment_Statuses`, `Return_Fee_Policy`. Đây là các thực thể độc lập, nằm ở rìa sơ đồ, tỏa quan hệ 1–N vào các thực thể nghiệp vụ. Chúng chỉ là bên “một” và không phụ thuộc bảng nào.

**Nhóm thực thể chủ (master):** `Users`, `Products`, `Parties` (với hai vai trò `Suppliers`, `Customers`).
- `Users` nhận khóa ngoại `Role_ID` từ `Roles` (R1) và là bên “một” cho toàn bộ các phiếu (người tạo) và `Audit_Logs`.
- `Products` nhận `Group_ID` từ `Product_Groups` (R2), là trung tâm phụ vì gần như mọi bảng chi tiết và `Inventory` đều trỏ về nó.
- `Parties` là **thực thể cha hợp nhất** mọi con người/tổ chức bên ngoài. `Suppliers` (R35) và `Customers` (R36) là hai hồ sơ *vai trò* gắn 1–1 vào một Party; một Party có thể mang cả hai vai trò hoặc là khách vãng lai chưa gắn vai trò. `Parties` còn là bên “một” cho *người bán* ở dòng phiếu nhập (R37) và cho *người trả* ở phiếu đổi/trả (R39) — đây là điểm mấu chốt khép lại yêu cầu "người bán đa loại".
- `Suppliers` giữ vai trò hồ sơ NCC của Party, mang hai bảng con vệ tinh: `Supplier_Bank_Accounts` (R26) và `Supplier_Documents` (R27). Nguồn nhập của phiếu nay trỏ trực tiếp về `Parties` (R4) chứ không qua `Suppliers`, để phủ được cả người bán không có hồ sơ NCC.
- `Customers` là bên “một” của `Export_Receipts` và `Rental_Receipts`, đồng thời có bảng con `Receivers` (R28). `Receivers` lại là bên “một” tùy chọn của các phiếu xuất/thuê qua `Receiver_ID` (R29, R30), song song với các cột snapshot người nhận trong phiếu.

**Trục 1 – Chứng từ (header–detail):** Mỗi loại nghiệp vụ tách thành cặp bảng cha–con quan hệ 1–N kiểu *composition* (xóa cha kéo theo con):
- Nhập: `Import_Receipts` → `Import_Receipt_Details`
- Xuất: `Export_Receipts` → `Export_Receipt_Details`
- Thuê/Mượn: `Rental_Receipts` → `Rental_Receipt_Details`
- Trả hàng thuê: `Return_Receipts` → `Return_Receipt_Details`, gắn ngược về `Rental_Receipts` (R31) và dòng thuê gốc (R33)
- Đổi/trả hàng bán: `Sales_Returns` → `Sales_Return_Details`, gắn ngược về `Export_Receipts` (R38) và dòng bán gốc (R41); khi đổi hàng còn nối tới phiếu xuất mới (R44)
- Kiểm kê: `Stocktakes` → `Stocktake_Details` → (sinh ra) `Stock_Adjustments`

Bảng header giữ thông tin chung (ngày, đối tác, tổng tiền, trạng thái, người tạo); bảng detail giữ từng dòng sản phẩm (số lượng, giá, serial). Các thực thể `*_Details` là **thực thể yếu** phụ thuộc tồn tại vào header của chúng.

**Trục 2 – Inventory:** `Inventory` là **bảng trung tâm** mô tả vòng đời từng đơn vị/lô hàng. Nó nhận `Product_ID` từ `Products` (R3) và tùy chọn liên kết ngược `Import_Detail_ID` tới dòng phiếu nhập (R7) để phục vụ “ánh xạ ngược tồn khi sửa phiếu” (§3.3.1). Cột `Status` phản ánh hàng đang IN_STOCK / SOLD / RENTED / LENT / DAMAGED / RESERVED — đáp ứng yêu cầu phân biệt hàng bán, cho thuê, cho mượn, hư hỏng và hàng “hold” (§1.3, §2.3, §3.2.1). Các dòng detail của phiếu xuất/thuê tùy chọn trỏ tới đúng `Inventory.ID` (R11, R17) khi hàng có serial.

**Nhóm hỗ trợ vận hành:** `Alerts` và `Audit_Logs` đứng tách, không tham gia khóa ngoại bắt buộc với nghiệp vụ; chúng tham chiếu mềm qua cặp (`Ref_Table`,`Ref_ID`) / (`Target_Table`,`Target_ID`) để ghi cảnh báo (§3.8) và nhật ký (§3.1.2, §4.2). `Stock_Summary` là bảng cache 1–1 với `Products` (R34), đứng cạnh `Products`, được cập nhật từ các luồng nghiệp vụ chứ không nhập trực tiếp.

**Dữ liệu ephemeral ngoài DB:** thông tin khóa đăng nhập và rate-limit không nằm trong ERD của MySQL mà lưu ở **Redis** (key có TTL) — thể hiện trên sơ đồ kiến trúc như một store phụ, không phải thực thể quan hệ.

**Điểm giao quan trọng cần thể hiện trên ERD:**
1. `Products` và `Users` là hai “hub” có nhiều đường tỏa nhất.
2. Loại giao dịch thuê/mượn được ghi trực tiếp bằng `Rental_Type` (RENT/LEND) trong `Rental_Receipts`; loại giao dịch nhập/xuất nằm ở chính các phiếu tương ứng. Không có bảng `Transaction_Types` riêng.
3. Toàn bộ cạnh header→detail đánh dấu là quan hệ toàn phần (identifying relationship), khóa chính của detail phụ thuộc header.

**Thứ tự vẽ đề xuất:** đặt danh mục lookup ở trái, master (`Products`, `Users`, `Suppliers`, `Customers`) ở giữa-trái, cụm chứng từ ở giữa-phải, `Inventory` ở trung tâm-phải, `Alerts`/`Audit_Logs` ở góc phải.

---

# PHẦN 4: CHI TIẾT TỪNG BẢNG TRONG DATABASE

> Ký hiệu: PK = khóa chính, FK = khóa ngoại, UQ = unique, NN = NOT NULL, AI = AUTO_INCREMENT.
> Hành vi FK mặc định theo file cũ: **ON UPDATE CASCADE**; **ON DELETE RESTRICT** với Products/Groups, **ON DELETE SET NULL** với các danh mục lookup còn lại, **ON DELETE CASCADE** với quan hệ header→detail.

## 4.1. Bảng `Roles`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã vai trò |
| Role_Name | VARCHAR(50) | UQ, NN | — | Tên vai trò |
| Description | VARCHAR(255) | NULL | — | Mô tả quyền |

*Dữ liệu mặc định:* WAREHOUSE, ACCOUNTANT, SALES, MANAGER, ADMIN (§1.3).

## 4.2. Bảng `Users`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã người dùng |
| Username | VARCHAR(50) | UQ, NN | — | Tên đăng nhập |
| Password_Hash | VARCHAR(255) | NN | — | Mật khẩu bcrypt |
| Full_Name | VARCHAR(100) | NULL | — | Họ tên |
| Email | VARCHAR(150) | UQ, NULL | — | Email xác thực/OTP |
| Role_ID | INT | NN, FK | Roles.ID | Vai trò |
| Is_Active | BOOLEAN | NN, DEFAULT TRUE | — | Còn hiệu lực |
| Created_At | DATETIME | NN, DEFAULT NOW | — | Ngày tạo |

*Dữ liệu mặc định:* `admin` (role ADMIN) — kế thừa từ file cũ, mật khẩu cần hash lại (§4.2).
*Redis (ngoài MySQL):* `login_fail:{username}` (TTL 5 phút, khóa sau 5 lần sai §3.1.1) và `rate_limit:{user_id}:{yyyymmdd}` (TTL hết ngày, chống flooding §4.2).

## 4.3. Bảng `Product_Groups`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã nhóm |
| Group_Name | VARCHAR(100) | UQ, NN | — | Tên nhóm |
| Is_Deleted | BOOLEAN | NN, DEFAULT FALSE | — | Xóa mềm |

*Dữ liệu mặc định (12 nhóm, kế thừa):* TOTAL_PRODUCTS_LIST, CISCO_ROUTER, SFP_3RD, CISCO_ARUBA_WIFI, CISCO_CATALYST, CISCO_COMPONENT, CISCO_SFP, CISCO_SMB_CBS, CISCO_VOIP_CONFERENCE, FIREWALL, NEXUS_JUNIPER_OTHER_SWITCH, OTHERS_COMPONENT.

## 4.4. Bảng `Products`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã sản phẩm |
| Product_Name | VARCHAR(255) | NN | — | Tên sản phẩm |
| Group_ID | INT | NN, FK | Product_Groups.ID | Nhóm sản phẩm |
| Unit | VARCHAR(30) | NN | — | Đơn vị tính |
| Default_Price_In | DECIMAL(18,2) | NN, DEFAULT 0 | — | Giá nhập niêm yết |
| Default_Price_Out | DECIMAL(18,2) | NN, DEFAULT 0 | — | Giá bán biểu kiến |
| Is_Serialized | BOOLEAN | NN, DEFAULT TRUE | — | Có serial hay không |
| Min_Stock_Level | INT | NN, DEFAULT 0 | — | Ngưỡng tồn tối thiểu |
| Image_Path | VARCHAR(255) | NULL | — | Đường dẫn ảnh |
| Is_Deleted | BOOLEAN | NN, DEFAULT FALSE | — | Xóa mềm |

*Ràng buộc bảng:* UQ (Product_Name, Group_ID).
*Index:* IX_Products_Name, IX_Products_Group_ID (§3.2.1 tìm kiếm/lọc).
*Dữ liệu mẫu (kế thừa):* Cisco ISR4321 Router, Cisco ISR4331 Router (CISCO_ROUTER); Cisco Catalyst 2960X (CISCO_CATALYST); Cisco SFP GLC-LH-SMD (CISCO_SFP); Firewall ASA5506 (FIREWALL).
*Ghi chú:* thuộc tính "Người bán" đã chuyển sang `Import_Receipts.Source_Party_ID` (nguồn nhập cả phiếu, trỏ Parties).

## 4.5. Bảng `Parties`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã đối tác |
| Party_Type | VARCHAR(20) | NN | — | INDIVIDUAL/ORGANIZATION |
| Full_Name | VARCHAR(255) | NN | — | Tên đối tác/cá nhân/cơ sở |
| Tax_Code | VARCHAR(30) | UQ, NULL | — | Mã số thuế (tổ chức) |
| National_ID | VARCHAR(20) | NULL | — | Số Căn cước (khách bán lẻ cá nhân) |
| Address | VARCHAR(255) | NULL | — | Địa chỉ |
| Phone | VARCHAR(50) | NULL | — | Điện thoại |
| Email | VARCHAR(150) | NULL | — | Email |
| Is_Deleted | BOOLEAN | NN, DEFAULT FALSE | — | Xóa mềm |

## 4.6. Bảng `Suppliers` (vai trò NCC của Party)

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã NCC |
| Party_ID | INT | UQ, NN, FK (CASCADE) | Parties.ID | Hồ sơ NCC của Party (1–1) |
| Contract_No | VARCHAR(100) | NULL | — | Số hợp đồng/đơn đặt hàng |
| Tracking_Code | VARCHAR(100) | NULL | — | Mã vận đơn |
| Is_Deleted | BOOLEAN | NN, DEFAULT FALSE | — | Xóa mềm |

## 4.6.1. Bảng `Supplier_Bank_Accounts`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã tài khoản |
| Supplier_ID | INT | NN, FK (CASCADE) | Suppliers.ID | Thuộc NCC |
| Account_Name | VARCHAR(150) | NN | — | Tên chủ tài khoản |
| Account_Number | VARCHAR(50) | NN | — | Số tài khoản |
| Bank_Name | VARCHAR(150) | NN | — | Tên ngân hàng |
| Bank_Payment_Code | VARCHAR(100) | NULL | — | Mã thanh toán ngân hàng |

## 4.6.2. Bảng `Supplier_Documents`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã chứng từ |
| Supplier_ID | INT | NN, FK (CASCADE) | Suppliers.ID | Thuộc NCC |
| Doc_Type | VARCHAR(100) | NULL | — | Loại chứng từ |
| File_Path | VARCHAR(255) | NN | — | Đường dẫn/object key (object storage) |
| Uploaded_At | DATETIME | NN, DEFAULT NOW | — | Thời điểm tải lên |

## 4.7. Bảng `Customers` (vai trò KH của Party)

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã khách hàng |
| Party_ID | INT | UQ, NN, FK (CASCADE) | Parties.ID | Hồ sơ KH của Party (1–1) |
| Is_Deleted | BOOLEAN | NN, DEFAULT FALSE | — | Xóa mềm |

## 4.7.1. Bảng `Receivers`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã người nhận |
| Customer_ID | INT | NN, FK (CASCADE) | Customers.ID | Thuộc khách hàng |
| Receiver_Name | VARCHAR(150) | NN | — | Tên người nhận |
| Address | VARCHAR(255) | NULL | — | Địa chỉ nhận hàng |
| Phone | VARCHAR(50) | NULL | — | Điện thoại |
| Is_Deleted | BOOLEAN | NN, DEFAULT FALSE | — | Xóa mềm |

## 4.8. Bảng `VAT_Options`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã VAT |
| VAT | VARCHAR(10) | UQ, NN | — | Nhãn VAT |
| Rate | DECIMAL(5,2) | NULL | — | Giá trị % để tính tiền |

*Dữ liệu mặc định:* NO, 0%, 5%, 8%, 10% (kế thừa).

## 4.9. Bảng `Payment_Statuses`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã trạng thái |
| Payment_Status | VARCHAR(50) | UQ, NN | — | Tên trạng thái |

*Dữ liệu mặc định:* PAID, UNPAID, PARTIAL, RENTING, RETURNED, CANCELLED (kế thừa).

## 4.10. Bảng `Import_Receipts`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã phiếu nhập |
| Receipt_Code | VARCHAR(30) | UQ, NN | — | Số phiếu |
| Source_Party_ID | INT | NN, FK | Parties.ID | Nguồn nhập/người bán cả phiếu (NCC/khách lẻ/khách đổi-trả) |
| Date_In | DATETIME | NN | — | Ngày nhập |
| Total_Amount | DECIMAL(18,2) | NN, DEFAULT 0 | — | Tổng giá trị nhập (dẫn xuất) |
| Status | VARCHAR(20) | NN, DEFAULT 'DRAFT' | — | DRAFT/PENDING/CONFIRMED |
| Note | TEXT | NULL | — | Ghi chú |
| Created_By | INT | NN, FK | Users.ID | Người tạo |
| Created_At | DATETIME | NN, DEFAULT NOW | — | Thời gian tạo |

*Index:* IX_Import_Source_Party_ID, IX_Import_Date_In.

## 4.11. Bảng `Import_Receipt_Details`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã dòng |
| Import_Receipt_ID | INT | NN, FK (CASCADE) | Import_Receipts.ID | Phiếu nhập |
| Product_ID | INT | NN, FK | Products.ID | Sản phẩm |
| Quantity | INT | NN, CHECK>0 | — | Số lượng nhập |
| Price_In | DECIMAL(18,2) | NN | — | Giá nhập |
| Serial | VARCHAR(255) | NULL | — | Serial |
| Tracking_Code | VARCHAR(100) | NULL | — | Mã vận đơn |
| Line_Total | DECIMAL(18,2) | NN | — | = Quantity×Price_In (dẫn xuất) |

## 4.12. Bảng `Export_Receipts`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã phiếu xuất |
| Receipt_Code | VARCHAR(30) | UQ, NN | — | Số phiếu |
| Customer_ID | INT | NN, FK | Customers.ID | Khách hàng |
| Receiver_ID | INT | NULL, FK (SET NULL) | Receivers.ID | Người nhận được chọn |
| Receiver_Name_Snapshot | VARCHAR(150) | NULL | — | Snapshot tên người nhận |
| Receiver_Address_Snapshot | VARCHAR(255) | NULL | — | Snapshot địa chỉ nhận |
| Receiver_Phone_Snapshot | VARCHAR(50) | NULL | — | Snapshot điện thoại nhận |
| Date_Out | DATETIME | NN | — | Ngày xuất |
| Delivery_Date | DATETIME | NULL | — | Ngày khách nhận (mốc tính hạn đổi/trả) |
| VAT_ID | INT | NULL, FK (SET NULL) | VAT_Options.ID | Mức VAT |
| Deposit | DECIMAL(18,2) | NN, DEFAULT 0 | — | Tiền đặt cọc |
| Total_Amount | DECIMAL(18,2) | NN, DEFAULT 0 | — | Tổng giá trị xuất (dẫn xuất) |
| Payment_Status_ID | INT | NULL, FK (SET NULL) | Payment_Statuses.ID | Trạng thái thanh toán |
| Paid | DECIMAL(18,2) | NN, DEFAULT 0 | — | Đã thanh toán |
| Remain | DECIMAL(18,2) | NN, DEFAULT 0 | — | Còn lại (dẫn xuất) |
| Status | VARCHAR(20) | NN, DEFAULT 'PENDING' | — | PENDING/CONFIRMED/EXPORTED |
| Note | TEXT | NULL | — | Ghi chú |
| Created_By | INT | NN, FK | Users.ID | NV bán hàng |
| Created_At | DATETIME | NN, DEFAULT NOW | — | Thời gian tạo |

*Index:* IX_Export_Customer_ID, IX_Export_Date_Out, IX_Export_Payment_Status_ID.

## 4.13. Bảng `Export_Receipt_Details`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã dòng |
| Export_Receipt_ID | INT | NN, FK (CASCADE) | Export_Receipts.ID | Phiếu xuất |
| Product_ID | INT | NN, FK | Products.ID | Sản phẩm |
| Inventory_ID | INT | NULL, FK (SET NULL) | Inventory.ID | Đơn vị hàng xuất |
| Quantity | INT | NN, CHECK>0 | — | Số lượng |
| Price_Out | DECIMAL(18,2) | NN | — | Giá bán |
| Serial | VARCHAR(255) | NULL | — | Serial |
| Warranty_Months | INT | NULL | — | Số tháng bảo hành |
| Warranty_Expiry | DATE | NULL | — | Hạn bảo hành (dẫn xuất) |
| Line_Total | DECIMAL(18,2) | NN | — | = Quantity×Price_Out (dẫn xuất) |

## 4.14. Bảng `Rental_Receipts`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã phiếu thuê |
| Receipt_Code | VARCHAR(30) | UQ, NN | — | Số phiếu |
| Customer_ID | INT | NN, FK | Customers.ID | Khách thuê |
| Receiver_ID | INT | NULL, FK (SET NULL) | Receivers.ID | Người nhận được chọn |
| Receiver_Name_Snapshot | VARCHAR(150) | NULL | — | Snapshot tên người nhận |
| Receiver_Address_Snapshot | VARCHAR(255) | NULL | — | Snapshot địa chỉ nhận |
| Receiver_Phone_Snapshot | VARCHAR(50) | NULL | — | Snapshot điện thoại nhận |
| Rental_Type | VARCHAR(10) | NN, DEFAULT 'RENT' | — | RENT/LEND (thuê/mượn) |
| Date_Out | DATETIME | NN | — | Ngày cho thuê |
| Due_Date | DATETIME | NN | — | Hạn thuê |
| Deposit | DECIMAL(18,2) | NN, DEFAULT 0 | — | Tiền cọc |
| Rental_Price_Per_Day | DECIMAL(18,2) | NN, DEFAULT 0 | — | Giá thuê/ngày (tính phạt) |
| Total_Rent | DECIMAL(18,2) | NN, DEFAULT 0 | — | Tổng tiền thuê (dẫn xuất) |
| Penalty_Amount | DECIMAL(18,2) | NN, DEFAULT 0 | — | Tiền phạt quá hạn (dẫn xuất) |
| Return_Date | DATETIME | NULL | — | Ngày trả thực tế |
| Payment_Status_ID | INT | NULL, FK (SET NULL) | Payment_Statuses.ID | RENTING/RETURNED |
| Created_By | INT | NN, FK | Users.ID | Người lập |
| Created_At | DATETIME | NN, DEFAULT NOW | — | Thời gian |

*Index:* IX_Rental_Customer_ID, IX_Rental_Due_Date (cảnh báo 7 ngày §3.8).

## 4.15. Bảng `Rental_Receipt_Details`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã dòng |
| Rental_Receipt_ID | INT | NN, FK (CASCADE) | Rental_Receipts.ID | Phiếu thuê |
| Product_ID | INT | NN, FK | Products.ID | Sản phẩm |
| Inventory_ID | INT | NULL, FK (SET NULL) | Inventory.ID | Đơn vị hàng thuê |
| Quantity | INT | NN, CHECK>0 | — | Số lượng |
| Rental_Price | DECIMAL(18,2) | NN | — | Giá cho thuê |
| Serial | VARCHAR(255) | NULL | — | Serial |
| Is_Returned | BOOLEAN | NN, DEFAULT FALSE | — | Đã trả chưa |

## 4.16. Bảng `Inventory`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã dòng tồn kho |
| Product_ID | INT | NN, FK (RESTRICT) | Products.ID | Sản phẩm |
| Serial | VARCHAR(255) | UQ (điều kiện), NULL | — | Serial thiết bị |
| Import_Detail_ID | INT | NULL, FK (SET NULL) | Import_Receipt_Details.ID | Nguồn nhập |
| Quantity_On_Hand | INT | NN, DEFAULT 0 | — | Tồn thực (hàng không serial) |
| Status | VARCHAR(20) | NN, DEFAULT 'IN_STOCK' | — | IN_STOCK/SOLD/RENTED/LENT/DAMAGED/RESERVED |
| Price_In | DECIMAL(18,2) | NULL | — | Giá nhập đơn vị |
| Note | TEXT | NULL | — | Ghi chú tình trạng |
| Date_In | DATETIME | NULL | — | Ngày nhập |
| Date_Out | DATETIME | NULL | — | Ngày xuất |

*Index (kế thừa file cũ):* IX_Inventory_Product_ID, IX_Inventory_Serial, IX_Inventory_Date_In, IX_Inventory_Date_Out.

## 4.17. Bảng `Stocktakes`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã phiếu kiểm kê |
| Stocktake_Code | VARCHAR(30) | UQ, NN | — | Số phiếu |
| Stocktake_Date | DATETIME | NN | — | Ngày kiểm kê |
| Status | VARCHAR(20) | NN, DEFAULT 'DRAFT' | — | DRAFT/COMPLETED |
| Created_By | INT | NN, FK | Users.ID | Người kiểm kê |

## 4.18. Bảng `Stocktake_Details`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã dòng |
| Stocktake_ID | INT | NN, FK (CASCADE) | Stocktakes.ID | Phiếu kiểm kê |
| Product_ID | INT | NN, FK | Products.ID | Sản phẩm |
| System_Qty | INT | NN | — | Số lý thuyết |
| Actual_Qty | INT | NN | — | Số thực tế |
| Difference | INT | NN | — | = Actual−System (dẫn xuất) |

## 4.19. Bảng `Stock_Adjustments`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã phiếu điều chỉnh |
| Stocktake_ID | INT | NN, FK (CASCADE) | Stocktakes.ID | Nguồn kiểm kê |
| Product_ID | INT | NN, FK | Products.ID | Sản phẩm |
| Adjust_Qty | INT | NN | — | Lượng điều chỉnh ± |
| Reason | VARCHAR(255) | NULL | — | Lý do |
| Created_At | DATETIME | NN, DEFAULT NOW | — | Thời gian |

## 4.20. Bảng `Alerts`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã cảnh báo |
| Alert_Type | VARCHAR(30) | NN | — | LOW_STOCK/RENT_DUE/PENDING_APPROVAL/DEBT_OVERDUE |
| Message | VARCHAR(255) | NN | — | Nội dung |
| Ref_Table | VARCHAR(50) | NULL | — | Bảng liên quan |
| Ref_ID | INT | NULL | — | ID liên quan |
| Is_Resolved | BOOLEAN | NN, DEFAULT FALSE | — | Đã xử lý chưa |
| Created_At | DATETIME | NN, DEFAULT NOW | — | Thời gian |

## 4.21. Bảng `Audit_Logs`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | BIGINT | PK, AI, NN | — | Mã log |
| User_ID | INT | NULL, FK (SET NULL) | Users.ID | Người thực hiện |
| Action | VARCHAR(50) | NN | — | Hành động |
| Target_Table | VARCHAR(50) | NULL | — | Bảng tác động |
| Target_ID | INT | NULL | — | ID bản ghi |
| Detail | JSON | NULL | — | Chi tiết |
| Created_At | DATETIME | NN, DEFAULT NOW | — | Thời điểm |

## 4.22. Bảng `Return_Receipts`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã phiếu trả |
| Receipt_Code | VARCHAR(30) | UQ, NN | — | Số phiếu trả |
| Rental_Receipt_ID | INT | NN, FK | Rental_Receipts.ID | Phiếu thuê/mượn gốc |
| Return_Date | DATETIME | NN | — | Ngày trả thực tế |
| Days_Overdue | INT | NN, DEFAULT 0 | — | Số ngày quá hạn (dẫn xuất) |
| Penalty_Amount | DECIMAL(18,2) | NN, DEFAULT 0 | — | Tiền phạt quá hạn (dẫn xuất, §3.5.1) |
| Note | TEXT | NULL | — | Ghi chú |
| Created_By | INT | NN, FK | Users.ID | Người lập |
| Created_At | DATETIME | NN, DEFAULT NOW | — | Thời gian |

## 4.23. Bảng `Return_Receipt_Details`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã dòng |
| Return_Receipt_ID | INT | NN, FK (CASCADE) | Return_Receipts.ID | Phiếu trả |
| Rental_Detail_ID | INT | NN, FK | Rental_Receipt_Details.ID | Dòng thuê gốc |
| Product_ID | INT | NN, FK | Products.ID | Sản phẩm |
| Inventory_ID | INT | NULL, FK (SET NULL) | Inventory.ID | Đơn vị hàng trả về kho |
| Quantity | INT | NN, CHECK>0 | — | Số lượng trả |
| Serial | VARCHAR(255) | NULL | — | Serial |
| Condition_Note | VARCHAR(255) | NULL | — | Tình trạng hàng khi trả |

## 4.23.1. Bảng `Sales_Returns` (đổi/trả hàng bán — nghiệp vụ mới)

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã phiếu đổi/trả |
| Receipt_Code | VARCHAR(30) | UQ, NN | — | Số phiếu |
| Export_Receipt_ID | INT | NN, FK | Export_Receipts.ID | Phiếu bán gốc |
| Party_ID | INT | NN, FK | Parties.ID | Người trả (khách đã mua) |
| Return_Type | VARCHAR(20) | NN | — | REFUND/EXCHANGE/WARRANTY |
| Exchange_Direction | VARCHAR(20) | NULL | — | EQUAL_HIGHER/LOWER (khi EXCHANGE) |
| Return_Date | DATETIME | NN | — | Ngày đổi/trả |
| Days_Since_Delivery | INT | NULL | — | Số ngày từ Delivery_Date (dẫn xuất) |
| Policy_ID | INT | NULL, FK (SET NULL) | Return_Fee_Policy.ID | Bậc chính sách phí áp dụng |
| Original_Value | DECIMAL(18,2) | NN, DEFAULT 0 | — | Giá bán gốc hàng trả (dẫn xuất) |
| Fee_Percent | DECIMAL(5,2) | NN, DEFAULT 0 | — | Tỷ lệ phí (dẫn xuất từ Policy) |
| Fee_Amount | DECIMAL(18,2) | NN, DEFAULT 0 | — | Tiền phí = Original_Value×Fee_Percent (dẫn xuất) |
| New_Export_Receipt_ID | INT | NULL, FK (SET NULL) | Export_Receipts.ID | Phiếu xuất mới khi đổi hàng |
| Refund_Amount | DECIMAL(18,2) | NN, DEFAULT 0 | — | Tiền hoàn = Original_Value−Fee_Amount±chênh (dẫn xuất) |
| Status | VARCHAR(20) | NN, DEFAULT 'DRAFT' | — | DRAFT/CONFIRMED/COMPLETED/REJECTED |
| Note | TEXT | NULL | — | Ghi chú |
| Created_By | INT | NN, FK | Users.ID | Nhân viên lập |
| Created_At | DATETIME | NN, DEFAULT NOW | — | Thời gian |

*Index:* IX_SalesReturn_Export, IX_SalesReturn_Party.

## 4.23.1a. Bảng `Return_Fee_Policy` (cấu hình phí đổi/trả — nghiệp vụ mới)

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã dòng chính sách |
| Tier_Name | VARCHAR(30) | NN | — | WITHIN_1_WEEK/WITHIN_2_WEEKS/WITHIN_1_MONTH |
| Max_Days | INT | NN | — | Giới hạn ngày của bậc (7/14/30) |
| Action_Type | VARCHAR(30) | NN | — | REFUND/EXCHANGE_EQUAL_HIGHER/EXCHANGE_LOWER |
| Fee_Percent | DECIMAL(5,2) | NN | — | Tỷ lệ phí áp dụng |
| Is_Allowed | BOOLEAN | NN, DEFAULT TRUE | — | Cho phép/từ chối |
| Effective_From | DATE | NULL | — | Ngày hiệu lực chính sách |

*Ràng buộc bảng:* UQ (Tier_Name, Action_Type, Effective_From).
*Dữ liệu mặc định:* 9 dòng theo ma trận quy tắc (1 tuần: 0/0/10%; 2 tuần: 20/0/10%; 1 tháng: 30/10/20%). Sau 30 ngày không có dòng khớp → từ chối.

## 4.23.2. Bảng `Sales_Return_Details` (nghiệp vụ mới)

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| ID | INT | PK, AI, NN | — | Mã dòng |
| Sales_Return_ID | INT | NN, FK (CASCADE) | Sales_Returns.ID | Phiếu đổi/trả |
| Export_Detail_ID | INT | NN, FK | Export_Receipt_Details.ID | Dòng bán gốc |
| Product_ID | INT | NN, FK | Products.ID | Sản phẩm trả |
| Inventory_ID | INT | NULL, FK (SET NULL) | Inventory.ID | Đơn vị hàng nhập lại kho |
| Quantity | INT | NN, CHECK>0 | — | Số lượng trả |
| Serial | VARCHAR(255) | NULL | — | Serial |
| Return_Condition | VARCHAR(20) | NN | — | RESELLABLE/DAMAGED/TO_WARRANTY |
| Restock_Status | VARCHAR(20) | NULL | — | Trạng thái tồn sau nhập lại |

## 4.24. Bảng `Stock_Summary`

| Tên cột | Kiểu dữ liệu | Ràng buộc | Tham chiếu | Ý nghĩa |
|---|---|---|---|---|
| Product_ID | INT | PK, FK (CASCADE) | Products.ID | Khóa chính + tham chiếu SP (1 dòng/SP) |
| Qty_On_Hand | INT | NN, DEFAULT 0 | — | Tồn khả dụng (dẫn xuất) |
| Qty_Reserved | INT | NN, DEFAULT 0 | — | Đang hold/chờ xuất (dẫn xuất) |
| Qty_Rented | INT | NN, DEFAULT 0 | — | Đang cho thuê/mượn (dẫn xuất) |
| Qty_Damaged | INT | NN, DEFAULT 0 | — | Hàng hư hỏng (dẫn xuất) |
| Last_Updated | DATETIME | NN, DEFAULT NOW | — | Lần cập nhật cuối |

*Ghi chú:* cập nhật trong cùng transaction nghiệp vụ hoặc bằng trigger; có job đối soát định kỳ tính lại từ `Inventory`.

---

## Phụ lục: Các VIEW kế thừa (hỗ trợ báo cáo §3.7, dashboard §7.1.2)

- **View_Inventory_Full** — join `Inventory` với `Products` và `Product_Groups` để hiển thị tên sản phẩm/nhóm thay cho ID, kèm `Serial`, `Status`, `Price_In`, `Date_In`, `Date_Out` (§6.2.1). *(Sau chuẩn hóa, `Inventory` chỉ giữ thông tin tồn kho/vòng đời đơn vị hàng; các thông tin khách hàng, VAT, thanh toán, loại giao dịch đã chuyển sang tầng chứng từ — muốn xem đầy đủ thì join tiếp qua `Export_Receipt_Details`/`Rental_Receipt_Details` tới phiếu tương ứng.)*
- **View_Product_Count_By_Group** — đếm số sản phẩm theo nhóm (§6.2.2).
- **View_Inventory_Summary** — tổng hợp Total_Items, Total_Price_In/Out, Total_Paid/Remain, Total_Rent/In theo nhóm (§6.2.3).

## Ghi chú tuân thủ SRS
- Xóa mềm áp dụng cho danh mục (`Parties`, `Products`, `Product_Groups`, `Suppliers`, `Customers`, `Receivers`) theo §3.2.1.
- Mật khẩu chỉ lưu dạng hash; dữ liệu nhạy cảm (giá, NCC) khuyến nghị mã hóa ở tầng ứng dụng (§4.2).
- Tổng tiền, thành tiền, còn lại, chênh lệch, hạn bảo hành, tiền phạt là thuộc tính **dẫn xuất** — tính bằng trigger/tầng service, không cho nhập tay để tránh sai lệch (§3.3.1, §3.4.1, §3.5.1, §3.6.1).
- Dữ liệu ephemeral (khóa đăng nhập, rate limit) lưu ở **Redis** với TTL, không lưu MySQL (§3.1.1, §4.1, §4.2).
- File tham chiếu (ảnh SP, chứng từ NCC) lưu trên **object storage** (MinIO/S3/GCS); DB chỉ giữ đường dẫn/object key (§3.2.1, §3.3.2).
- `Stock_Summary` là cache đọc nhanh phi chuẩn hóa; nguồn sự thật vẫn là `Inventory`, cần đối soát định kỳ (§3.2.1, §4.1, §7.1.2).

## Nguyên tắc thiết kế các bảng chi tiết (`*_Details`)
- **`Product_ID` NOT NULL ở mọi bảng detail** (Import/Export/Rental/Return/Sales_Return/Stocktake). Ở các bảng có con trỏ dòng gốc (`Return_Receipt_Details.Rental_Detail_ID`, `Sales_Return_Details.Export_Detail_ID`), `Product_ID` về lý thuyết suy được qua dòng gốc, nhưng vẫn **giữ trực tiếp** vì hai lý do: (1) *bất biến chứng từ* — dòng chi tiết là bản ghi lịch sử, phải khẳng định cố định "sản phẩm gì" độc lập với việc dòng gốc bị sửa/xóa mềm; (2) *nhất quán cấu trúc* — mọi bảng detail cùng một khuôn, đơn giản hóa code xử lý. Đây là **phi chuẩn hóa có chủ đích**.
- **`Inventory_ID` NULL-able** ở các bảng detail có xuất/nhập hàng vật lý: có giá trị khi hàng **serialized** (trỏ đúng đơn vị vật lý), NULL khi hàng **không serial** (bán/trả theo số lượng). Cột này luôn mang thông tin mới (đơn vị cụ thể được xuất/trả/nhập lại), **không dư thừa**.
- **Ràng buộc toàn vẹn (tầng ứng dụng):** khi `Inventory_ID` có giá trị thì `Inventory.Product_ID` của nó phải **khớp** `Product_ID` trên cùng dòng detail — tránh trỏ nhầm đơn vị hàng thuộc sản phẩm khác. Áp cho tất cả `*_Details` có `Inventory_ID`.
