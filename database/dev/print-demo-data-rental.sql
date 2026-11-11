-- =====================================================================
-- Dữ liệu mẫu nhóm CHO THUÊ / CHO MƯỢN (biên bản bàn giao thuê/mượn, biên bản trả hàng).
-- ID dải 9601–9699. Chạy sau database/dev/print-demo-data.sql. Chạy lại được nhiều lần.
--   Rental_Receipts 9601 = RENT (khách Customers 9001), 9602 = LEND (khách Customers 9002)
--   Return_Receipts 9601 = trả phiếu thuê 9601 (có 1 dòng hư hỏng, bồi thường, hoàn cọc)
-- =====================================================================
SET NAMES utf8mb4;
SET @u := (SELECT ID FROM Users WHERE Username = 'print_demo');
SET @wh := (SELECT ID FROM Warehouses WHERE Warehouse_Code = 'MAIN');
SET @renting := (SELECT ID FROM Payment_Statuses WHERE Payment_Status = 'RENTING');
SET @returned := (SELECT ID FROM Payment_Statuses WHERE Payment_Status = 'RETURNED');

DELETE FROM Return_Receipt_Details WHERE ID BETWEEN 9601 AND 9699;
DELETE FROM Return_Receipts WHERE ID BETWEEN 9601 AND 9699;
DELETE FROM Rental_Receipt_Details WHERE ID BETWEEN 9601 AND 9699;
DELETE FROM Rental_Receipts WHERE ID BETWEEN 9601 AND 9699;

-- phiếu cho thuê: 14 ngày, 150.000đ/ngày/bộ, cọc 5.000.000đ
INSERT INTO Rental_Receipts (ID, Receipt_Code, Customer_ID, Warehouse_ID, Receiver_ID, Receiver_Name_Snapshot,
                             Receiver_Address_Snapshot, Receiver_Phone_Snapshot, Rental_Type, Date_Out, Due_Date,
                             Deposit, Rental_Price_Per_Day, Total_Rent, Penalty_Amount, Return_Date, Status,
                             Payment_Status_ID, Created_By, Created_At) VALUES
 (9601, 'CT-MAIN-2026-9601', 9001, @wh, 9001, 'Chị Trần Thị Nhận',
        'VP 19, tầng 3, Tòa nhà Demo Garden, 35 Phố Mẫu, Phường Thanh Xuân, Hà Nội', '0900 000 555',
        'RENT', '2026-10-20 09:00:00', '2026-11-03 17:00:00', 5000000, 150000, 6300000, 450000,
        '2026-11-06 10:30:00', 'APPROVED', @returned, @u, '2026-10-20 08:50:00'),
 (9602, 'CM-MAIN-2026-9602', 9002, @wh, NULL, 'Nguyễn Văn Khách',
        '7 Hẻm Thử Nghiệm, Phường 3, Quận Gò Vấp, TP. Hồ Chí Minh', '0900 000 444',
        'LEND', '2026-11-05 15:00:00', '2026-11-12 17:00:00', 0, 0, 0, 0,
        NULL, 'APPROVED', @renting, @u, '2026-11-05 14:50:00');

INSERT INTO Rental_Receipt_Details (ID, Rental_Receipt_ID, Product_ID, Inventory_ID, Quantity, Rental_Price, Serial, Is_Returned) VALUES
 (9601, 9601, 9001, NULL, 1, 150000, 'FOC2601D0B1', TRUE),
 (9602, 9601, 9001, NULL, 1, 150000, 'FOC2601D0B2', TRUE),
 (9603, 9601, 9004, NULL, 1, 150000, 'Q3AB-DEMO-0101', TRUE),
 (9604, 9602, 9001, NULL, 1, 0, 'FOC2601D0B3', FALSE),
 (9605, 9602, 9002, NULL, 4, 0, NULL, FALSE);

-- phiếu trả: quá hạn 3 ngày (3 x 150.000 = 450.000), 1 thiết bị hư hỏng bồi thường 2.000.000
-- hoàn cọc = 5.000.000 - 450.000 - 2.000.000 = 2.550.000
INSERT INTO Return_Receipts (ID, Receipt_Code, Rental_Receipt_ID, Warehouse_ID, Return_Date, Days_Overdue, Penalty_Amount,
                             Compensation_Amount, Deposit_Refund, Note, Created_By, Created_At) VALUES
 (9601, 'TH-MAIN-2026-9601', 9601, @wh, '2026-11-06 10:30:00', 3, 450000, 2000000, 2550000,
        'Khách trả trễ 3 ngày; 1 thiết bị trầy xước vỏ, hỏng cổng uplink', @u, '2026-11-06 10:30:00');
INSERT INTO Return_Receipt_Details (ID, Return_Receipt_ID, Rental_Detail_ID, Product_ID, Inventory_ID, Quantity, Serial, Condition_Note) VALUES
 (9601, 9601, 9601, 9001, NULL, 1, 'FOC2601D0B1', 'Tốt, đầy đủ phụ kiện'),
 (9602, 9601, 9602, 9001, NULL, 1, 'FOC2601D0B2', 'Hư hỏng: hỏng cổng uplink 1, trầy xước vỏ'),
 (9603, 9601, 9603, 9004, NULL, 1, 'Q3AB-DEMO-0101', 'Tốt');
