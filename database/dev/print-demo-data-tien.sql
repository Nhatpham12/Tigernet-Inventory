-- =====================================================================
-- Dữ liệu mẫu nhóm TIỀN (phiếu thu/chi, biên bản đối chiếu công nợ). Dải ID 9501–9599.
-- Chạy SAU database/dev/print-demo-data.sql. Chạy lại được nhiều lần.
-- Khách "Demo Đối Chiếu" (Party 9501, Customer 9501) có kỳ 01–31/10/2026:
--   đầu kỳ 25.000.000 | tăng 15.500.000 | giảm 21.000.000 | cuối kỳ 19.500.000
-- (phiếu xuất hủy 9503 và thanh toán hủy 9503 phải bị bỏ qua)
-- =====================================================================
SET NAMES utf8mb4;
SET @u := (SELECT ID FROM Users WHERE Username = 'print_demo');
SET @wh := (SELECT ID FROM Warehouses WHERE Warehouse_Code = 'MAIN');
SET @partial := (SELECT ID FROM Payment_Statuses WHERE Payment_Status = 'PARTIAL');
SET @renting := (SELECT ID FROM Payment_Statuses WHERE Payment_Status = 'RENTING');

DELETE FROM Payments WHERE ID BETWEEN 9501 AND 9599;
DELETE FROM Rental_Receipt_Details WHERE Rental_Receipt_ID BETWEEN 9501 AND 9599;
DELETE FROM Rental_Receipts WHERE ID BETWEEN 9501 AND 9599;
DELETE FROM Export_Receipt_Details WHERE Export_Receipt_ID BETWEEN 9501 AND 9599;
DELETE FROM Export_Receipts WHERE ID BETWEEN 9501 AND 9599;
DELETE FROM Customers WHERE ID BETWEEN 9501 AND 9599;
DELETE FROM Parties WHERE ID BETWEEN 9501 AND 9599;

INSERT INTO Parties (ID, Party_Type, Full_Name, Tax_Code, National_ID, Address, Phone, Email) VALUES
 (9501, 'ORGANIZATION', 'Công ty TNHH Demo Đối Chiếu', '0300000501', NULL,
        '88 Đường Mẫu, Phường Demo, TP. Hồ Chí Minh', '0900 000 501', 'ketoan@doichieu-demo.vn');
INSERT INTO Customers (ID, Party_ID) VALUES (9501, 9501);

INSERT INTO Export_Receipts (ID, Receipt_Code, Customer_ID, Warehouse_ID, Receiver_Name_Snapshot, Date_Out,
                             Delivery_Date, Deposit, Total_Amount, Payment_Status_ID, Paid, Remain, Status, Note, Created_By) VALUES
 (9501, 'PX-MAIN-2026-9501', 9501, @wh, 'Demo Đối Chiếu', '2026-09-15 09:00:00', '2026-09-15 09:00:00', 0, 40000000, @partial, 15000000, 25000000, 'EXPORTED', NULL, @u),
 (9502, 'PX-MAIN-2026-9502', 9501, @wh, 'Demo Đối Chiếu', '2026-10-12 10:00:00', '2026-10-12 10:00:00', 0, 12000000, @partial, 0, 12000000, 'EXPORTED', NULL, @u),
 (9503, 'PX-MAIN-2026-9503', 9501, @wh, 'Demo Đối Chiếu', '2026-10-20 11:00:00', '2026-10-20 11:00:00', 0, 5000000, NULL, 0, 5000000, 'CANCELLED', NULL, @u);

INSERT INTO Rental_Receipts (ID, Receipt_Code, Customer_ID, Warehouse_ID, Rental_Type, Date_Out, Due_Date, Deposit,
                             Rental_Price_Per_Day, Total_Rent, Penalty_Amount, Status, Payment_Status_ID, Created_By) VALUES
 (9501, 'PT-MAIN-2026-9501', 9501, @wh, 'RENT', '2026-10-25 09:00:00', '2026-11-25 09:00:00', 0, 100000, 3000000, 500000, 'APPROVED', @renting, @u);

INSERT INTO Payments (ID, Payment_Code, Direction, Party_ID, Reference_Type, Reference_ID, Amount, Method, Paid_At,
                      Status, Note, Recorded_By) VALUES
 (9501, 'PT-2026-9501', 'IN', 9501, 'Export_Receipts', 9501, 15000000, 'CASH', '2026-09-30 15:00:00', 'COMPLETED', 'Thu tiền hàng đợt 1', @u),
 (9502, 'PT-2026-9502', 'IN', 9501, 'Export_Receipts', 9502, 20000000, 'TRANSFER', '2026-10-28 09:30:00', 'COMPLETED', NULL, @u),
 (9503, 'PT-2026-9503', 'IN', 9501, 'Export_Receipts', 9502, 1000000, 'CASH', '2026-10-29 09:30:00', 'CANCELLED', 'Phiếu thu nhập nhầm, đã hủy', @u),
 (9504, 'PT-2026-9504', 'IN', 9501, 'Rental_Receipts', 9501, 1000000, 'CASH', '2026-10-30 16:00:00', 'COMPLETED', NULL, @u),
 (9505, 'PC-2026-9505', 'OUT', 9001, 'Import_Receipts', 9001, 5000000, 'TRANSFER', '2026-11-05 11:00:00', 'COMPLETED', NULL, @u),
 (9506, 'PC-2026-9506', 'OUT', 9002, 'Sales_Returns', 1, 1500000, 'CASH', '2026-11-06 11:00:00', 'COMPLETED', 'Hoàn tiền khách trả hàng', @u);
