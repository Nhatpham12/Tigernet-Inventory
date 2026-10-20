-- =====================================================================
-- TIGERNET — Tenant database v7 — Flyway V2: dữ liệu danh mục bắt buộc
-- Nguồn: docs/Database_Design_v7.md (Phần 4 — "Dữ liệu mặc định")
-- Tài khoản quản trị đầu tiên KHÔNG seed ở đây (tránh mật khẩu mặc định);
-- ứng dụng tạo khi provisioning tenant (OPS-01), mật khẩu ngẫu nhiên gửi qua email.
-- =====================================================================

INSERT INTO Roles (Role_Name, Description) VALUES
    ('WAREHOUSE',  'Thủ kho'),
    ('ACCOUNTANT', 'Kế toán'),
    ('SALES',      'Nhân viên bán hàng'),
    ('MANAGER',    'Nhà quản lý'),
    ('ADMIN',      'Quản trị hệ thống');

INSERT INTO Product_Groups (Group_Name) VALUES
    ('TOTAL_PRODUCTS_LIST'),
    ('CISCO_ROUTER'),
    ('SFP_3RD'),
    ('CISCO_ARUBA_WIFI'),
    ('CISCO_CATALYST'),
    ('CISCO_COMPONENT'),
    ('CISCO_SFP'),
    ('CISCO_SMB_CBS'),
    ('CISCO_VOIP_CONFERENCE'),
    ('FIREWALL'),
    ('NEXUS_JUNIPER_OTHER_SWITCH'),
    ('OTHERS_COMPONENT');

INSERT INTO VAT_Options (VAT, Rate) VALUES
    ('NO',  NULL),
    ('0%',  0.00),
    ('5%',  5.00),
    ('8%',  8.00),
    ('10%', 10.00);

INSERT INTO Payment_Statuses (Payment_Status) VALUES
    ('PAID'), ('UNPAID'), ('PARTIAL'), ('RENTING'), ('RETURNED'), ('CANCELLED');

INSERT INTO Return_Fee_Policy (Tier_Name, Max_Days, Action_Type, Fee_Percent) VALUES
    ('WITHIN_1_WEEK',  7,  'REFUND',                 0.00),
    ('WITHIN_1_WEEK',  7,  'EXCHANGE_EQUAL_HIGHER',  0.00),
    ('WITHIN_1_WEEK',  7,  'EXCHANGE_LOWER',        10.00),
    ('WITHIN_2_WEEKS', 14, 'REFUND',                20.00),
    ('WITHIN_2_WEEKS', 14, 'EXCHANGE_EQUAL_HIGHER',  0.00),
    ('WITHIN_2_WEEKS', 14, 'EXCHANGE_LOWER',        10.00),
    ('WITHIN_1_MONTH', 30, 'REFUND',                30.00),
    ('WITHIN_1_MONTH', 30, 'EXCHANGE_EQUAL_HIGHER', 10.00),
    ('WITHIN_1_MONTH', 30, 'EXCHANGE_LOWER',        20.00);

-- Kho mặc định (mục 4.2a). Quản trị sửa địa chỉ thật sau khi nhận tenant.
INSERT INTO Warehouses (Warehouse_Code, Warehouse_Name, Address) VALUES
    ('MAIN', 'Kho tổng', 'Chưa cập nhật địa chỉ');
