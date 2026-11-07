-- =====================================================================
-- Dữ liệu mẫu để thử API in biểu mẫu (docs/BIEU_MAU.md).
-- Toàn bộ là dữ liệu giả, ID từ 9001 trở lên để không đụng dữ liệu thật.
-- Chạy lại được nhiều lần: mỗi lần xóa rồi tạo lại các dòng ID >= 9001.
--
-- Điều kiện: đã chạy docs/Database_Script_v7.sql (hoặc Flyway V1–V5) và đã có
-- tài khoản đăng nhập Username = 'print_demo' (tạo qua POST /users), vì file này
-- không chứa mật khẩu.
--   mysql -u root -p tigernet_dev < database/dev/print-demo-data.sql
-- Các file print-demo-data-*.sql bổ sung dữ liệu cho từng nhóm biểu mẫu, chạy sau file này.
-- =====================================================================
SET NAMES utf8mb4;
SET @u := (SELECT ID FROM Users WHERE Username = 'print_demo');
SET @wh := (SELECT ID FROM Warehouses WHERE Warehouse_Code = 'MAIN');
SET @vat10 := (SELECT ID FROM VAT_Options WHERE VAT = '10%');
SET @unpaid := (SELECT ID FROM Payment_Statuses WHERE Payment_Status = 'UNPAID');
SET @partial := (SELECT ID FROM Payment_Statuses WHERE Payment_Status = 'PARTIAL');
SET @grp := (SELECT ID FROM Product_Groups WHERE Group_Name = 'CISCO_CATALYST');

-- dọn dữ liệu mẫu cũ (con trước, cha sau)
DELETE FROM Payments WHERE ID >= 9001;
DELETE FROM Export_Receipt_Details WHERE Export_Receipt_ID >= 9001;
DELETE FROM Export_Receipts WHERE ID >= 9001;
DELETE FROM Import_Receipt_Details WHERE Import_Receipt_ID >= 9001;
DELETE FROM Import_Receipts WHERE ID >= 9001;
DELETE FROM Receivers WHERE ID >= 9001;
DELETE FROM Customers WHERE ID >= 9001;
DELETE FROM Suppliers WHERE ID >= 9001;
DELETE FROM Parties WHERE ID >= 9001;
DELETE FROM Products WHERE ID >= 9001;

-- kho MAIN: địa chỉ và thủ kho để in phiếu
UPDATE Warehouses SET Address = '12 Đường Số 5, KCN Demo, TP. Thủ Đức, TP. Hồ Chí Minh', Phone = '028 0000 1111',
       Manager_ID = @u
 WHERE ID = @wh;

INSERT INTO Products (ID, Product_Name, Group_ID, Unit, Default_Price_In, Default_Price_Out, Is_Serialized, Min_Stock_Level) VALUES
 (9001, 'Switch Cisco Catalyst C9200L-24T-4G-E (24 cổng Gigabit, 4 cổng uplink)', @grp, 'Chiếc', 18500000, 21900000, TRUE, 2),
 (9002, 'Module quang SFP 1000BASE-LX/LH 10km', @grp, 'Chiếc', 650000, 890000, FALSE, 10),
 (9003, 'Cáp mạng Cat6 UTP đóng thùng 305m', @grp, 'Thùng', 2350000, 2900000, FALSE, 5),
 (9004, 'Bộ phát Wi-Fi Cisco Meraki MR36', @grp, 'Chiếc', 9800000, 12500000, TRUE, 1),
 (9005, 'Dây nguồn AC chuẩn US 1,8m', @grp, 'Sợi', 60000, 95000, FALSE, 20);

INSERT INTO Parties (ID, Party_Type, Full_Name, Tax_Code, National_ID, Address, Phone, Email) VALUES
 (9001, 'ORGANIZATION', 'Công ty TNHH Phân phối Thiết bị Mạng Bình Minh', '0300000001', NULL,
        '45 Đường Demo 1, Phường Bến Nghé, TP. Hồ Chí Minh', '028 0000 2222', 'banhang@binhminh-demo.vn'),
 (9002, 'ORGANIZATION', 'Công ty Cổ phần Giải pháp Số An Phát', '0100000002', NULL,
        'Tầng 6, Tòa nhà Demo, 99 Phố Mẫu, Phường Cửa Nam, Hà Nội', '024 0000 3333', 'ketoan@anphat-demo.vn'),
 (9003, 'INDIVIDUAL', 'Nguyễn Văn Khách', NULL, '079000000001',
        '7 Hẻm Thử Nghiệm, Phường 3, Quận Gò Vấp, TP. Hồ Chí Minh', '0900 000 444', NULL);

INSERT INTO Suppliers (ID, Party_ID, Contract_No) VALUES (9001, 9001, 'HĐNT-DEMO-01');
INSERT INTO Customers (ID, Party_ID) VALUES (9001, 9002), (9002, 9003);
INSERT INTO Receivers (ID, Customer_ID, Receiver_Name, Address, Phone) VALUES
 (9001, 9001, 'Chị Trần Thị Nhận', 'VP 19, tầng 3, Tòa nhà Demo Garden, 35 Phố Mẫu, Phường Thanh Xuân, Hà Nội', '0900 000 555');

-- PHIẾU NHẬP KHO (01-VT): mua của NCC Bình Minh, VAT 10%, chưa trả hết
INSERT INTO Import_Receipts (ID, Receipt_Code, Source_Party_ID, Warehouse_ID, Date_In, VAT_ID, Total_Amount,
                             Payment_Status_ID, Paid, Remain, Status, Note, Created_By) VALUES
 (9001, 'PN-MAIN-2026-9001', 9001, @wh, '2026-11-03 09:30:00', @vat10, 59895000, @partial, 30000000, 29895000,
        'CONFIRMED', 'Hóa đơn GTGT số 0001234 ngày 03/11/2026', @u);
INSERT INTO Import_Receipt_Details (ID, Import_Receipt_ID, Product_ID, Quantity, Price_In, Serial, Line_Total) VALUES
 (9001, 9001, 9001, 1, 18500000, 'FOC2601D0A1', 18500000),
 (9002, 9001, 9001, 1, 18500000, 'FOC2601D0A2', 18500000),
 (9003, 9001, 9002, 20, 650000, NULL, 13000000),
 (9004, 9001, 9003, 2, 2350000, NULL, 4700000),
 (9005, 9001, 9005, 2, 60000, NULL, 120000);

-- PHIẾU XUẤT KHO BÁN HÀNG (02-VT) + phiếu giao hàng + biên bản bàn giao/phiếu bảo hành
INSERT INTO Export_Receipts (ID, Receipt_Code, Customer_ID, Warehouse_ID, Receiver_ID, Receiver_Name_Snapshot,
                             Receiver_Address_Snapshot, Receiver_Phone_Snapshot, Date_Out, Delivery_Date, VAT_ID,
                             Deposit, Total_Amount, Payment_Status_ID, Paid, Remain, Status, Note, Created_By) VALUES
 (9001, 'PX-MAIN-2026-9001', 9001, @wh, 9001, 'Chị Trần Thị Nhận',
        'VP 19, tầng 3, Tòa nhà Demo Garden, 35 Phố Mẫu, Phường Thanh Xuân, Hà Nội', '0900 000 555',
        '2026-11-05 14:00:00', '2026-11-07 10:00:00', @vat10, 0, 53130000, @partial, 20000000, 33130000,
        'EXPORTED', 'Giao hàng qua chành xe, hàng dễ vỡ', @u),
 (9002, 'PX-MAIN-2026-9002', 9002, @wh, NULL, 'Nguyễn Văn Khách',
        '7 Hẻm Thử Nghiệm, Phường 3, Quận Gò Vấp, TP. Hồ Chí Minh', '0900 000 444',
        '2026-11-06 08:15:00', '2026-11-06 08:15:00', NULL, 0, 2995000, NULL, 2995000, 0,
        'EXPORTED', NULL, @u);
INSERT INTO Export_Receipt_Details (ID, Export_Receipt_ID, Product_ID, Quantity, Price_Out, Serial, Warranty_Months,
                                    Warranty_Expiry, Line_Total) VALUES
 (9001, 9001, 9001, 1, 21900000, 'FOC2601D0A1', 36, '2029-11-05', 21900000),
 (9002, 9001, 9004, 1, 12500000, 'Q3AB-DEMO-0001', 12, '2027-11-05', 12500000),
 (9003, 9001, 9002, 10, 890000, NULL, 12, '2027-11-05', 8900000),
 (9004, 9001, 9005, 4, 95000, NULL, NULL, NULL, 380000),
 (9005, 9002, 9003, 1, 2900000, NULL, NULL, NULL, 2900000),
 (9006, 9002, 9005, 1, 95000, NULL, NULL, NULL, 95000);

-- PHIẾU THU (01-TT) và PHIẾU CHI (02-TT)
INSERT INTO Payments (ID, Payment_Code, Direction, Party_ID, Reference_Type, Reference_ID, Amount, Method, Paid_At,
                      Status, Note, Recorded_By) VALUES
 (9001, 'PT-2026-9001', 'IN', 9002, 'Export_Receipts', 9001, 20000000, 'TRANSFER', '2026-11-06 15:20:00',
        'COMPLETED', 'Thu tiền hàng đợt 1 theo phiếu xuất PX-MAIN-2026-9001', @u),
 (9002, 'PC-2026-9002', 'OUT', 9001, 'Import_Receipts', 9001, 30000000, 'CASH', '2026-11-04 10:00:00',
        'COMPLETED', 'Trả tiền hàng đợt 1 theo phiếu nhập PN-MAIN-2026-9001', @u),
 (9003, 'PT-2026-9003', 'IN', 9003, 'Export_Receipts', 9002, 2995000, 'CASH', '2026-11-06 08:20:00',
        'COMPLETED', 'Thu tiền bán lẻ', @u);
