-- =====================================================================
-- TIGERNET — Tenant database v7.2 — Flyway V5: khớp schema với ERD v1.2
-- Nguồn chuẩn: docs/tigernet_erd_v1_2.drawio.png (xem docs/Database_Design_v7.md,
-- mục "Đối chiếu ERD v1.2 và schema"). Không sửa V1..V4 để giữ checksum Flyway.
--
-- Thay đổi:
--   1. User_Warehouses : khóa chính riêng ID (ERD), cặp (User_ID, Warehouse_ID) thành UNIQUE
--   2. Warranty_Claims : bỏ Created_At (ERD không có; thời điểm lập phiếu = Received_At)
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- 1. USER_WAREHOUSES: PK ghép -> PK riêng ID
-- Thêm UNIQUE trước để FK_UW_User vẫn có index bắt đầu bằng User_ID khi bỏ PK cũ.
-- =====================================================================
ALTER TABLE User_Warehouses
    ADD CONSTRAINT UQ_UW_User_Warehouse UNIQUE (User_ID, Warehouse_ID);

ALTER TABLE User_Warehouses
    DROP PRIMARY KEY,
    ADD COLUMN ID INT NOT NULL AUTO_INCREMENT FIRST,
    ADD CONSTRAINT PK_User_Warehouses PRIMARY KEY (ID);

-- =====================================================================
-- 2. WARRANTY_CLAIMS: bỏ Created_At
-- =====================================================================
ALTER TABLE Warranty_Claims
    DROP COLUMN Created_At;
