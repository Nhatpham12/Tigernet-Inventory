-- Dữ liệu mẫu nhóm KIỂM KÊ: 1 phiếu kiểm kê kho MAIN (khớp, thừa, thiếu, hàng hư hỏng) + các điều chỉnh tồn kho sinh từ đó.
-- Dải ID 9701–9799. Chạy sau print-demo-data.sql (cần Products 9001–9005 và user print_demo).
SET NAMES utf8mb4;
SET @u := (SELECT ID FROM Users WHERE Username = 'print_demo');
SET @wh := (SELECT ID FROM Warehouses WHERE Warehouse_Code = 'MAIN');

DELETE FROM Stock_Adjustments WHERE Stocktake_ID BETWEEN 9701 AND 9799;
DELETE FROM Stocktake_Details WHERE Stocktake_ID BETWEEN 9701 AND 9799;
DELETE FROM Stocktakes WHERE ID BETWEEN 9701 AND 9799;

INSERT INTO Stocktakes (ID, Stocktake_Code, Warehouse_ID, Stocktake_Date, Status, Created_By) VALUES
 (9701, 'KK-MAIN-2026-9701', @wh, '2026-11-07 08:30:00', 'COMPLETED', @u);

INSERT INTO Stocktake_Details (ID, Stocktake_ID, Product_ID, System_Qty, Actual_Qty, Difference) VALUES
 (9701, 9701, 9001, 2, 2, 0),    -- khớp
 (9702, 9701, 9002, 20, 22, 2),  -- thừa
 (9703, 9701, 9003, 5, 4, -1),   -- thiếu (mất)
 (9704, 9701, 9004, 3, 2, -1),   -- hàng hư hỏng, loại khỏi tồn
 (9705, 9701, 9005, 40, 40, 0);  -- khớp

INSERT INTO Stock_Adjustments (ID, Stocktake_ID, Product_ID, Adjust_Qty, Reason, Created_At) VALUES
 (9701, 9701, 9002, 2, 'Thừa khi kiểm kê, chờ đối chiếu chứng từ nhập', '2026-11-07 11:00:00'),
 (9702, 9701, 9003, -1, 'Thiếu 1 thùng, không rõ nguyên nhân, lập biên bản xử lý', '2026-11-07 11:00:00'),
 (9703, 9701, 9004, -1, 'Hàng hư hỏng do ẩm, không còn sử dụng được, đề nghị thanh lý', '2026-11-07 11:00:00');
