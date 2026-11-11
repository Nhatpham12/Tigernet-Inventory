-- Dữ liệu mẫu nhóm ĐỔI TRẢ HÀNG: 1 phiếu REFUND có phí, 1 phiếu EXCHANGE gắn phiếu xuất mới.
-- Dải ID 9701–9799. Phiếu xuất gốc: Export 9002 (bán lẻ). Chạy sau print-demo-data.sql.
SET NAMES utf8mb4;
SET @u := (SELECT ID FROM Users WHERE Username = 'print_demo');
SET @wh := (SELECT ID FROM Warehouses WHERE Warehouse_Code = 'MAIN');

DELETE FROM Sales_Return_Details WHERE Sales_Return_ID BETWEEN 9701 AND 9799;
DELETE FROM Sales_Returns WHERE ID BETWEEN 9701 AND 9799;
DELETE FROM Export_Receipt_Details WHERE Export_Receipt_ID BETWEEN 9701 AND 9799;
DELETE FROM Export_Receipts WHERE ID BETWEEN 9701 AND 9799;

-- Phiếu xuất mới cho đơn đổi hàng: 4 module quang SFP
INSERT INTO Export_Receipts (ID, Receipt_Code, Customer_ID, Warehouse_ID, Receiver_Name_Snapshot, Receiver_Address_Snapshot,
                             Receiver_Phone_Snapshot, Date_Out, Delivery_Date, Deposit, Total_Amount, Paid, Remain,
                             Status, Note, Created_By) VALUES
 (9701, 'PX-MAIN-2026-9701', 9002, @wh, 'Nguyễn Văn Khách', '7 Hẻm Thử Nghiệm, Phường 3, Quận Gò Vấp, TP. Hồ Chí Minh',
        '0900 000 444', '2026-11-07 09:00:00', '2026-11-07 09:00:00', 0, 3560000, 3560000, 0,
        'EXPORTED', 'Xuất đổi hàng theo phiếu đổi trả PTH-MAIN-2026-9702', @u);
INSERT INTO Export_Receipt_Details (ID, Export_Receipt_ID, Product_ID, Quantity, Price_Out, Serial, Line_Total) VALUES
 (9701, 9701, 9002, 4, 890000, NULL, 3560000);

INSERT INTO Sales_Returns (ID, Receipt_Code, Export_Receipt_ID, Party_ID, Warehouse_ID, Return_Type, Exchange_Direction,
                           Return_Date, Days_Since_Delivery, Original_Value, Fee_Percent, Fee_Amount, New_Export_Receipt_ID,
                           Refund_Amount, Status, Note, Created_By, Created_At) VALUES
 (9701, 'PTH-MAIN-2026-9701', 9002, 9003, @wh, 'REFUND', NULL, '2026-11-07 10:00:00', 1,
        2900000, 10, 290000, NULL, 2610000, 'COMPLETED', 'Khách không còn nhu cầu, hàng còn nguyên thùng', @u, '2026-11-07 10:00:00'),
 (9702, 'PTH-MAIN-2026-9702', 9002, 9003, @wh, 'EXCHANGE', 'EQUAL_HIGHER', '2026-11-07 09:00:00', 1,
        2995000, 5, 149750, 9701, -714750, 'COMPLETED', 'Đổi sang module quang SFP, khách bù thêm chênh lệch', @u, '2026-11-07 09:00:00');

INSERT INTO Sales_Return_Details (ID, Sales_Return_ID, Export_Detail_ID, Product_ID, Quantity, Serial, Return_Condition, Restock_Status) VALUES
 (9701, 9701, 9005, 9003, 1, NULL, 'RESELLABLE', NULL),
 (9702, 9702, 9005, 9003, 1, NULL, 'RESELLABLE', NULL),
 (9703, 9702, 9006, 9005, 1, NULL, 'DAMAGED', NULL);
