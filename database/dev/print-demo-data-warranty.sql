-- Dữ liệu mẫu BIÊN BẢN TIẾP NHẬN BẢO HÀNH – SỬA CHỮA: 2 phiếu bảo hành của cùng một khách nhận cùng ngày,
-- in chung 1 biên bản bằng GET /warranty-claims/print?ids=9801,9802 (hoặc từng phiếu: /warranty-claims/{id}/print).
-- Dải ID 9801–9899. Dòng bán gốc: Export_Receipt_Details 9001, 9002 (phiếu xuất 9001). Chạy sau print-demo-data.sql.
SET NAMES utf8mb4;
SET @u := (SELECT ID FROM Users WHERE Username = 'print_demo');
SET @wh := (SELECT ID FROM Warehouses WHERE Warehouse_Code = 'MAIN');

DELETE FROM Warranty_Claims WHERE ID BETWEEN 9801 AND 9899;

INSERT INTO Warranty_Claims (ID, Claim_Code, Export_Detail_ID, Received_Warehouse_ID, Status, Received_At, Note,
                             Handled_By) VALUES
 (9801, 'BH-MAIN-2026-9801', 9001, @wh, 'RECEIVED', '2026-11-07 09:15:00',
        'Không lên nguồn, đèn SYS tắt; còn tem bảo hành', @u),
 (9802, 'BH-MAIN-2026-9802', 9002, @wh, 'RECEIVED', '2026-11-07 09:20:00',
        'Mất kết nối Wi-Fi sau vài phút, vỏ trầy nhẹ', @u);
