-- =====================================================================
-- TIGERNET — Tenant database schema v2 (MySQL 8.0+)
-- Mỗi khách hàng (tenant) có 1 database riêng dùng schema này.
-- Flyway: đặt file này trong src/main/resources/db/migration/tenant
--
-- Quy ước:
--   * PK: BIGINT AUTO_INCREMENT (mỗi tenant 1 DB nên không cần UUID)
--   * Tiền: DECIMAL(18,2) — Số lượng: INT (xem câu hỏi mở Q1 trong ERD)
--   * Trạng thái: VARCHAR + CHECK (enum trong code Java, không dùng bảng tra cứu)
--   * Chứng từ (phiếu) có cột version cho optimistic locking (@Version)
--   * Xoá = xoá mềm (is_deleted) hoặc đổi status CANCELLED, không xoá vật lý
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. ĐỊNH DANH & PHÂN QUYỀN
-- ---------------------------------------------------------------------

CREATE TABLE roles (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(30)  NOT NULL,              -- STOREKEEPER, ACCOUNTANT, SALES, MANAGER, ADMIN
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE users (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    role_id             BIGINT       NOT NULL,       -- mỗi user đúng 1 vai trò
    username            VARCHAR(50)  NOT NULL,       -- chỉ set lúc tạo, không sửa
    password_hash       VARCHAR(100) NOT NULL,       -- bcrypt
    full_name           VARCHAR(150) NOT NULL,
    email               VARCHAR(150) NOT NULL,
    phone               VARCHAR(20)  NULL,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,  -- DELETE /users = vô hiệu hoá
    failed_login_count  INT          NOT NULL DEFAULT 0,
    locked_until        DATETIME     NULL,           -- khoá 15' sau 5 lần sai
    last_login_at       DATETIME     NULL,
    password_changed_at DATETIME     NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE refresh_tokens (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    token_hash CHAR(64)     NOT NULL,               -- SHA-256 của token, không lưu token gốc
    expires_at DATETIME     NOT NULL,
    revoked_at DATETIME     NULL,
    created_ip VARCHAR(45)  NULL,
    user_agent VARCHAR(255) NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_hash (token_hash),
    KEY idx_refresh_tokens_user (user_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE password_reset_otps (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    otp_hash      VARCHAR(100) NOT NULL,             -- bcrypt của OTP
    expires_at    DATETIME     NOT NULL,             -- hiệu lực 10'
    used_at       DATETIME     NULL,
    attempt_count INT          NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_password_reset_otps_user (user_id, created_at),
    CONSTRAINT fk_password_reset_otps_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE audit_logs (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NULL,                   -- NULL = hệ thống / job tự động
    action      VARCHAR(50)  NOT NULL,               -- CREATE, UPDATE, CONFIRM, APPROVE, LOGIN...
    entity_type VARCHAR(50)  NULL,                   -- tên bảng / aggregate
    entity_id   BIGINT       NULL,
    summary     VARCHAR(500) NULL,
    old_data    JSON         NULL,
    new_data    JSON         NULL,
    ip_address  VARCHAR(45)  NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_logs_entity (entity_type, entity_id),
    KEY idx_audit_logs_user_time (user_id, created_at),
    KEY idx_audit_logs_time (created_at),
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 2. CẤU HÌNH
-- ---------------------------------------------------------------------

CREATE TABLE vat_rates (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    code         VARCHAR(20)  NOT NULL,              -- VAT0, VAT5, VAT8, VAT10, KCT
    name         VARCHAR(100) NOT NULL,
    rate_percent DECIMAL(5,2) NOT NULL,
    is_default   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vat_rates_code (code),
    CONSTRAINT ck_vat_rates_rate CHECK (rate_percent >= 0 AND rate_percent <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Quy tắc cho thuê, khoá đăng nhập, OTP... (thay cho giá trị hard-code — đóng gap G-2)
CREATE TABLE system_settings (
    setting_key   VARCHAR(100) NOT NULL,
    setting_value VARCHAR(500) NOT NULL,
    value_type    VARCHAR(10)  NOT NULL DEFAULT 'STRING',
    description   VARCHAR(255) NULL,
    updated_by    BIGINT       NULL,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (setting_key),
    CONSTRAINT ck_system_settings_type CHECK (value_type IN ('STRING','INT','DECIMAL','BOOL')),
    CONSTRAINT fk_system_settings_user FOREIGN KEY (updated_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Sinh mã chứng từ an toàn khi chạy đồng thời: SELECT ... FOR UPDATE rồi tăng next_value
CREATE TABLE document_sequences (
    doc_type   VARCHAR(10) NOT NULL,                 -- PN, PX, PT, TT, TH, KK, DC, TTN
    period     VARCHAR(8)  NOT NULL,                 -- '2026' hoặc '202610'
    next_value INT         NOT NULL DEFAULT 1,
    PRIMARY KEY (doc_type, period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE return_fee_policies (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    tier_name      VARCHAR(100) NOT NULL,
    max_days       INT          NOT NULL,            -- áp dụng khi số ngày kể từ giao hàng <= max_days
    action_type    VARCHAR(20)  NOT NULL,
    fee_percent    DECIMAL(5,2) NOT NULL DEFAULT 0,
    is_allowed     BOOLEAN      NOT NULL DEFAULT TRUE,
    effective_from DATE         NOT NULL,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_return_fee_policies_action CHECK (action_type IN ('REFUND','EXCHANGE','WARRANTY')),
    CONSTRAINT ck_return_fee_policies_fee CHECK (fee_percent >= 0 AND fee_percent <= 100),
    CONSTRAINT ck_return_fee_policies_days CHECK (max_days >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 3. DANH MỤC & ĐỐI TÁC
-- ---------------------------------------------------------------------

CREATE TABLE warehouses (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(20)  NOT NULL,
    name       VARCHAR(150) NOT NULL,
    address    VARCHAR(255) NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouses_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_groups (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(30)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(255) NULL,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_groups_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE products (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    sku                   VARCHAR(50)   NOT NULL,    -- mã sản phẩm (tìm kiếm "theo mã")
    barcode               VARCHAR(64)   NULL,
    name                  VARCHAR(200)  NOT NULL,
    group_id              BIGINT        NULL,
    unit                  VARCHAR(20)   NOT NULL,    -- cái, bộ, hộp...
    default_import_price  DECIMAL(18,2) NOT NULL DEFAULT 0,
    default_sale_price    DECIMAL(18,2) NOT NULL DEFAULT 0,
    default_rental_price  DECIMAL(18,2) NOT NULL DEFAULT 0,  -- giá thuê / ngày
    is_serialized         BOOLEAN       NOT NULL DEFAULT FALSE,
    is_rentable           BOOLEAN       NOT NULL DEFAULT FALSE,
    min_stock_level       INT           NOT NULL DEFAULT 0,
    image_path            VARCHAR(500)  NULL,
    description           TEXT          NULL,
    is_deleted            BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_products_sku (sku),
    UNIQUE KEY uk_products_barcode (barcode),
    KEY idx_products_group (group_id),
    KEY idx_products_name (name),
    CONSTRAINT fk_products_group FOREIGN KEY (group_id) REFERENCES product_groups (id),
    CONSTRAINT ck_products_prices CHECK (default_import_price >= 0 AND default_sale_price >= 0 AND default_rental_price >= 0),
    CONSTRAINT ck_products_min_stock CHECK (min_stock_level >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Gộp Parties + Customers + Suppliers: 1 đối tác có thể vừa là KH vừa là NCC
CREATE TABLE partners (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    code           VARCHAR(30)  NOT NULL,
    is_customer    BOOLEAN      NOT NULL DEFAULT FALSE,
    is_supplier    BOOLEAN      NOT NULL DEFAULT FALSE,
    partner_kind   VARCHAR(20)  NOT NULL DEFAULT 'ORGANIZATION',
    full_name      VARCHAR(200) NOT NULL,
    tax_code       VARCHAR(20)  NULL,                -- MST (doanh nghiệp)
    national_id    VARCHAR(20)  NULL,                -- CCCD (cá nhân)
    contact_person VARCHAR(150) NULL,
    phone          VARCHAR(20)  NULL,
    email          VARCHAR(150) NULL,
    address        VARCHAR(255) NULL,
    contract_no    VARCHAR(50)  NULL,
    note           VARCHAR(500) NULL,
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_partners_code (code),
    KEY idx_partners_name (full_name),
    KEY idx_partners_phone (phone),
    KEY idx_partners_tax_code (tax_code),
    CONSTRAINT ck_partners_role CHECK (is_customer OR is_supplier),
    CONSTRAINT ck_partners_kind CHECK (partner_kind IN ('ORGANIZATION','INDIVIDUAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE receivers (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    partner_id BIGINT       NOT NULL,
    name       VARCHAR(150) NOT NULL,
    phone      VARCHAR(20)  NULL,
    address    VARCHAR(255) NULL,
    is_default BOOLEAN      NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_receivers_partner (partner_id),
    CONSTRAINT fk_receivers_partner FOREIGN KEY (partner_id) REFERENCES partners (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE partner_bank_accounts (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    partner_id     BIGINT       NOT NULL,
    bank_name      VARCHAR(150) NOT NULL,
    bank_bin       VARCHAR(10)  NULL,                -- mã BIN ngân hàng (VietQR / Napas)
    account_number VARCHAR(30)  NOT NULL,
    account_name   VARCHAR(150) NOT NULL,
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_partner_bank_accounts_partner (partner_id),
    CONSTRAINT fk_partner_bank_accounts_partner FOREIGN KEY (partner_id) REFERENCES partners (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE partner_documents (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    partner_id  BIGINT       NOT NULL,
    doc_type    VARCHAR(30)  NOT NULL,               -- CONTRACT, LICENSE, CERTIFICATE, OTHER
    file_name   VARCHAR(255) NOT NULL,
    file_path   VARCHAR(500) NOT NULL,
    file_size   BIGINT       NULL,
    uploaded_by BIGINT       NULL,
    uploaded_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    KEY idx_partner_documents_partner (partner_id),
    CONSTRAINT fk_partner_documents_partner FOREIGN KEY (partner_id) REFERENCES partners (id),
    CONSTRAINT fk_partner_documents_user FOREIGN KEY (uploaded_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 4. NHẬP KHO
-- ---------------------------------------------------------------------

CREATE TABLE import_receipts (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    code                VARCHAR(30)   NOT NULL,      -- PN-2026-00001
    warehouse_id        BIGINT        NOT NULL,
    supplier_id         BIGINT        NOT NULL,
    receipt_date        DATE          NOT NULL,
    supplier_invoice_no VARCHAR(50)   NULL,          -- số hoá đơn của NCC (đối soát kế toán)
    status              VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    subtotal            DECIMAL(18,2) NOT NULL DEFAULT 0,
    vat_amount          DECIMAL(18,2) NOT NULL DEFAULT 0,
    total_amount        DECIMAL(18,2) NOT NULL DEFAULT 0,
    paid_amount         DECIMAL(18,2) NOT NULL DEFAULT 0,   -- = tổng payments chưa huỷ
    note                VARCHAR(500)  NULL,
    created_by          BIGINT        NOT NULL,
    confirmed_by        BIGINT        NULL,
    confirmed_at        DATETIME      NULL,
    cancelled_by        BIGINT        NULL,
    cancelled_at        DATETIME      NULL,
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version             INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_import_receipts_code (code),
    KEY idx_import_receipts_supplier_date (supplier_id, receipt_date),
    KEY idx_import_receipts_status_date (status, receipt_date),
    CONSTRAINT fk_import_receipts_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_import_receipts_supplier FOREIGN KEY (supplier_id) REFERENCES partners (id),
    CONSTRAINT fk_import_receipts_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_import_receipts_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users (id),
    CONSTRAINT fk_import_receipts_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users (id),
    CONSTRAINT ck_import_receipts_status CHECK (status IN ('DRAFT','CONFIRMED','CANCELLED')),
    CONSTRAINT ck_import_receipts_amounts CHECK (subtotal >= 0 AND vat_amount >= 0 AND total_amount >= 0
                                                 AND paid_amount >= 0 AND paid_amount <= total_amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- SP có serial: mỗi dòng = 1 serial, quantity = 1 (ràng buộc ở tầng ứng dụng)
CREATE TABLE import_receipt_lines (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    receipt_id  BIGINT        NOT NULL,
    line_no     INT           NOT NULL,
    product_id  BIGINT        NOT NULL,
    serial_no   VARCHAR(100)  NULL,                  -- serial chưa tồn tại lúc nhập → lưu text, tạo product_serials khi xác nhận
    quantity    INT           NOT NULL,
    unit_price  DECIMAL(18,2) NOT NULL,
    line_total  DECIMAL(18,2) NOT NULL,
    note        VARCHAR(255)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_import_receipt_lines_serial (receipt_id, product_id, serial_no),
    KEY idx_import_receipt_lines_product (product_id),
    CONSTRAINT fk_import_receipt_lines_receipt FOREIGN KEY (receipt_id) REFERENCES import_receipts (id) ON DELETE CASCADE,
    CONSTRAINT fk_import_receipt_lines_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_import_receipt_lines_qty CHECK (quantity > 0),
    CONSTRAINT ck_import_receipt_lines_price CHECK (unit_price >= 0 AND line_total >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 5. TỒN KHO (nguồn sự thật duy nhất: stock_balances + stock_movements)
-- ---------------------------------------------------------------------

-- Từng món hàng có serial (thay cho Inventory cũ)
CREATE TABLE product_serials (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    product_id      BIGINT        NOT NULL,
    serial_no       VARCHAR(100)  NOT NULL,
    warehouse_id    BIGINT        NULL,              -- kho đang chứa; NULL khi đã bán / đang cho thuê
    status          VARCHAR(20)   NOT NULL DEFAULT 'IN_STOCK',
    import_line_id  BIGINT        NULL,              -- nhập từ dòng phiếu nhập nào
    unit_cost       DECIMAL(18,2) NULL,
    warranty_expiry DATE          NULL,              -- hạn bảo hành cho khách sau khi bán
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_serials_product_serial (product_id, serial_no),
    KEY idx_product_serials_serial (serial_no),
    KEY idx_product_serials_status (status),
    CONSTRAINT fk_product_serials_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_product_serials_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_product_serials_import_line FOREIGN KEY (import_line_id) REFERENCES import_receipt_lines (id),
    CONSTRAINT ck_product_serials_status CHECK (status IN
        ('IN_STOCK','RESERVED','RENTED','SOLD','DAMAGED','IN_REPAIR','LOST','RETURNED_TO_SUPPLIER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Số dư hiện tại theo kho × sản phẩm. CHECK chặn tồn âm ngay ở DB.
--   qty_on_hand : hàng dùng được đang nằm trong kho (gồm cả phần đã giữ chỗ)
--   qty_reserved: đã giữ cho phiếu xuất/thuê đã xác nhận nhưng chưa giao
--   qty_rented  : đang ở chỗ khách thuê
--   qty_damaged : hư hỏng / chờ sửa, không bán được
CREATE TABLE stock_balances (
    id            BIGINT   NOT NULL AUTO_INCREMENT,
    warehouse_id  BIGINT   NOT NULL,
    product_id    BIGINT   NOT NULL,
    qty_on_hand   INT      NOT NULL DEFAULT 0,
    qty_reserved  INT      NOT NULL DEFAULT 0,
    qty_rented    INT      NOT NULL DEFAULT 0,
    qty_damaged   INT      NOT NULL DEFAULT 0,
    qty_available INT      AS (qty_on_hand - qty_reserved) STORED,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version       INT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_balances_wh_product (warehouse_id, product_id),
    KEY idx_stock_balances_product (product_id),
    CONSTRAINT fk_stock_balances_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_stock_balances_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_stock_balances_non_negative CHECK (qty_on_hand >= 0 AND qty_reserved >= 0
                                                     AND qty_rented >= 0 AND qty_damaged >= 0),
    CONSTRAINT ck_stock_balances_reserved CHECK (qty_reserved <= qty_on_hand)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Sổ cái biến động tồn (chỉ INSERT, không UPDATE/DELETE).
-- Tồn tại thời điểm T = SUM(qty_change) WHERE created_at <= T  → báo cáo "tồn theo thời điểm".
CREATE TABLE stock_movements (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    warehouse_id  BIGINT        NOT NULL,
    product_id    BIGINT        NOT NULL,
    serial_id     BIGINT        NULL,
    movement_type VARCHAR(30)   NOT NULL,
    qty_change    INT           NOT NULL,            -- thay đổi của qty_on_hand (+ vào, - ra)
    balance_after INT           NOT NULL,            -- qty_on_hand sau biến động
    unit_cost     DECIMAL(18,2) NULL,
    ref_type      VARCHAR(30)   NOT NULL,            -- IMPORT_RECEIPT, EXPORT_RECEIPT, RENTAL_RECEIPT, RENTAL_RETURN, SALES_RETURN, STOCK_ADJUSTMENT
    ref_id        BIGINT        NOT NULL,
    ref_line_id   BIGINT        NULL,
    created_by    BIGINT        NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_stock_movements_product_time (product_id, warehouse_id, created_at),
    KEY idx_stock_movements_ref (ref_type, ref_id),
    KEY idx_stock_movements_serial (serial_id),
    CONSTRAINT fk_stock_movements_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_stock_movements_serial FOREIGN KEY (serial_id) REFERENCES product_serials (id),
    CONSTRAINT fk_stock_movements_user FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_stock_movements_type CHECK (movement_type IN
        ('IMPORT','EXPORT','RENTAL_OUT','RENTAL_RETURN','SALES_RETURN',
         'ADJUSTMENT_IN','ADJUSTMENT_OUT','DAMAGE','REPAIRED','OPENING_BALANCE')),
    CONSTRAINT ck_stock_movements_qty CHECK (qty_change <> 0),
    CONSTRAINT ck_stock_movements_balance CHECK (balance_after >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 6. XUẤT KHO & BÁN HÀNG
-- ---------------------------------------------------------------------

CREATE TABLE export_receipts (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    code              VARCHAR(30)   NOT NULL,        -- PX-2026-00001
    warehouse_id      BIGINT        NOT NULL,
    customer_id       BIGINT        NOT NULL,
    receiver_id       BIGINT        NULL,
    receiver_name     VARCHAR(150)  NULL,            -- snapshot lúc lập phiếu
    receiver_phone    VARCHAR(20)   NULL,
    receiver_address  VARCHAR(255)  NULL,
    export_date       DATE          NOT NULL,
    delivery_date     DATE          NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    vat_rate_id       BIGINT        NULL,
    vat_percent       DECIMAL(5,2)  NOT NULL DEFAULT 0,   -- snapshot: sửa mẫu VAT không ảnh hưởng phiếu cũ
    subtotal          DECIMAL(18,2) NOT NULL DEFAULT 0,
    discount_amount   DECIMAL(18,2) NOT NULL DEFAULT 0,
    vat_amount        DECIMAL(18,2) NOT NULL DEFAULT 0,
    total_amount      DECIMAL(18,2) NOT NULL DEFAULT 0,
    paid_amount       DECIMAL(18,2) NOT NULL DEFAULT 0,   -- = tổng payments chưa huỷ
    remaining_amount  DECIMAL(18,2) AS (total_amount - paid_amount) STORED,
    payment_status    VARCHAR(20)   NOT NULL DEFAULT 'UNPAID',
    e_invoice_no      VARCHAR(50)   NULL,            -- số hoá đơn điện tử (nếu có tích hợp)
    note              VARCHAR(500)  NULL,
    created_by        BIGINT        NOT NULL,
    confirmed_by      BIGINT        NULL,
    confirmed_at      DATETIME      NULL,
    completed_by      BIGINT        NULL,
    completed_at      DATETIME      NULL,
    cancelled_by      BIGINT        NULL,
    cancelled_at      DATETIME      NULL,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version           INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_export_receipts_code (code),
    KEY idx_export_receipts_customer_date (customer_id, export_date),
    KEY idx_export_receipts_status_date (status, export_date),
    KEY idx_export_receipts_payment (payment_status),
    CONSTRAINT fk_export_receipts_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_export_receipts_customer FOREIGN KEY (customer_id) REFERENCES partners (id),
    CONSTRAINT fk_export_receipts_receiver FOREIGN KEY (receiver_id) REFERENCES receivers (id),
    CONSTRAINT fk_export_receipts_vat FOREIGN KEY (vat_rate_id) REFERENCES vat_rates (id),
    CONSTRAINT fk_export_receipts_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_export_receipts_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users (id),
    CONSTRAINT fk_export_receipts_completed_by FOREIGN KEY (completed_by) REFERENCES users (id),
    CONSTRAINT fk_export_receipts_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users (id),
    CONSTRAINT ck_export_receipts_status CHECK (status IN ('PENDING','CONFIRMED','COMPLETED','CANCELLED')),
    CONSTRAINT ck_export_receipts_payment CHECK (payment_status IN ('UNPAID','PARTIAL','PAID')),
    CONSTRAINT ck_export_receipts_amounts CHECK (subtotal >= 0 AND discount_amount >= 0 AND vat_amount >= 0
                                                 AND total_amount >= 0 AND paid_amount >= 0
                                                 AND paid_amount <= total_amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE export_receipt_lines (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    receipt_id      BIGINT        NOT NULL,
    line_no         INT           NOT NULL,
    product_id      BIGINT        NOT NULL,
    serial_id       BIGINT        NULL,
    quantity        INT           NOT NULL,
    unit_price      DECIMAL(18,2) NOT NULL,
    line_total      DECIMAL(18,2) NOT NULL,
    warranty_months INT           NOT NULL DEFAULT 0,
    warranty_expiry DATE          NULL,              -- tính khi hoàn tất xuất
    returned_qty    INT           NOT NULL DEFAULT 0,   -- đã trả lại qua sales_returns
    note            VARCHAR(255)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_export_receipt_lines_serial (receipt_id, serial_id),
    KEY idx_export_receipt_lines_product (product_id),
    CONSTRAINT fk_export_receipt_lines_receipt FOREIGN KEY (receipt_id) REFERENCES export_receipts (id) ON DELETE CASCADE,
    CONSTRAINT fk_export_receipt_lines_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_export_receipt_lines_serial FOREIGN KEY (serial_id) REFERENCES product_serials (id),
    CONSTRAINT ck_export_receipt_lines_qty CHECK (quantity > 0 AND returned_qty >= 0 AND returned_qty <= quantity),
    CONSTRAINT ck_export_receipt_lines_price CHECK (unit_price >= 0 AND line_total >= 0),
    CONSTRAINT ck_export_receipt_lines_warranty CHECK (warranty_months >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 7. CHO THUÊ / MƯỢN & TRẢ HÀNG THUÊ
-- ---------------------------------------------------------------------

CREATE TABLE rental_receipts (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    code             VARCHAR(30)   NOT NULL,         -- PT-2026-00001
    warehouse_id     BIGINT        NOT NULL,
    customer_id      BIGINT        NOT NULL,
    receiver_id      BIGINT        NULL,
    receiver_name    VARCHAR(150)  NULL,
    receiver_phone   VARCHAR(20)   NULL,
    receiver_address VARCHAR(255)  NULL,
    rental_type      VARCHAR(10)   NOT NULL DEFAULT 'RENT',  -- RENT = thuê, LOAN = mượn (giá 0)
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    start_date       DATE          NOT NULL,
    due_date         DATE          NOT NULL,
    deposit_amount   DECIMAL(18,2) NOT NULL DEFAULT 0,
    rent_amount      DECIMAL(18,2) NOT NULL DEFAULT 0,       -- tiền thuê dự kiến / thực tế sau khi trả hết
    penalty_amount   DECIMAL(18,2) NOT NULL DEFAULT 0,       -- cộng dồn từ rental_returns
    damage_amount    DECIMAL(18,2) NOT NULL DEFAULT 0,
    total_amount     DECIMAL(18,2) NOT NULL DEFAULT 0,       -- rent + penalty + damage
    paid_amount      DECIMAL(18,2) NOT NULL DEFAULT 0,
    remaining_amount DECIMAL(18,2) AS (total_amount - paid_amount) STORED,
    payment_status   VARCHAR(20)   NOT NULL DEFAULT 'UNPAID',
    note             VARCHAR(500)  NULL,
    created_by       BIGINT        NOT NULL,
    confirmed_by     BIGINT        NULL,
    confirmed_at     DATETIME      NULL,
    cancelled_by     BIGINT        NULL,
    cancelled_at     DATETIME      NULL,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version          INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rental_receipts_code (code),
    KEY idx_rental_receipts_customer (customer_id),
    KEY idx_rental_receipts_status_due (status, due_date),   -- cảnh báo sắp hết hạn / quá hạn
    CONSTRAINT fk_rental_receipts_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_rental_receipts_customer FOREIGN KEY (customer_id) REFERENCES partners (id),
    CONSTRAINT fk_rental_receipts_receiver FOREIGN KEY (receiver_id) REFERENCES receivers (id),
    CONSTRAINT fk_rental_receipts_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_rental_receipts_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users (id),
    CONSTRAINT fk_rental_receipts_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users (id),
    CONSTRAINT ck_rental_receipts_type CHECK (rental_type IN ('RENT','LOAN')),
    CONSTRAINT ck_rental_receipts_status CHECK (status IN ('PENDING','ACTIVE','PARTIALLY_RETURNED','RETURNED','CANCELLED')),
    CONSTRAINT ck_rental_receipts_payment CHECK (payment_status IN ('UNPAID','PARTIAL','PAID')),
    CONSTRAINT ck_rental_receipts_dates CHECK (due_date > start_date),
    CONSTRAINT ck_rental_receipts_amounts CHECK (deposit_amount >= 0 AND rent_amount >= 0 AND penalty_amount >= 0
                                                 AND damage_amount >= 0 AND total_amount >= 0
                                                 AND paid_amount >= 0 AND paid_amount <= total_amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rental_receipt_lines (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    rental_id     BIGINT        NOT NULL,
    line_no       INT           NOT NULL,
    product_id    BIGINT        NOT NULL,
    serial_id     BIGINT        NULL,
    quantity      INT           NOT NULL,
    price_per_day DECIMAL(18,2) NOT NULL,
    line_total    DECIMAL(18,2) NOT NULL,            -- quantity × price_per_day × số ngày thuê
    returned_qty  INT           NOT NULL DEFAULT 0,
    note          VARCHAR(255)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rental_receipt_lines_serial (rental_id, serial_id),
    KEY idx_rental_receipt_lines_product (product_id),
    CONSTRAINT fk_rental_receipt_lines_rental FOREIGN KEY (rental_id) REFERENCES rental_receipts (id) ON DELETE CASCADE,
    CONSTRAINT fk_rental_receipt_lines_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_rental_receipt_lines_serial FOREIGN KEY (serial_id) REFERENCES product_serials (id),
    CONSTRAINT ck_rental_receipt_lines_qty CHECK (quantity > 0 AND returned_qty >= 0 AND returned_qty <= quantity),
    CONSTRAINT ck_rental_receipt_lines_price CHECK (price_per_day >= 0 AND line_total >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Một phiếu thuê có thể trả nhiều lần (trả từng phần)
CREATE TABLE rental_returns (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    code                  VARCHAR(30)   NOT NULL,    -- TT-2026-00001
    rental_id             BIGINT        NOT NULL,
    return_date           DATE          NOT NULL,
    days_overdue          INT           NOT NULL DEFAULT 0,
    penalty_amount        DECIMAL(18,2) NOT NULL DEFAULT 0,   -- quá hạn >= ngưỡng: số ngày quá hạn × giá thuê/ngày
    damage_amount         DECIMAL(18,2) NOT NULL DEFAULT 0,
    deposit_refund_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    note                  VARCHAR(500)  NULL,
    created_by            BIGINT        NOT NULL,
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rental_returns_code (code),
    KEY idx_rental_returns_rental (rental_id),
    CONSTRAINT fk_rental_returns_rental FOREIGN KEY (rental_id) REFERENCES rental_receipts (id),
    CONSTRAINT fk_rental_returns_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_rental_returns_amounts CHECK (days_overdue >= 0 AND penalty_amount >= 0
                                                AND damage_amount >= 0 AND deposit_refund_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rental_return_lines (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    return_id      BIGINT        NOT NULL,
    rental_line_id BIGINT        NOT NULL,
    serial_id      BIGINT        NULL,
    quantity       INT           NOT NULL,
    item_condition VARCHAR(20)   NOT NULL DEFAULT 'GOOD',
    condition_note VARCHAR(255)  NULL,
    damage_fee     DECIMAL(18,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_rental_return_lines_return (return_id),
    KEY idx_rental_return_lines_rental_line (rental_line_id),
    CONSTRAINT fk_rental_return_lines_return FOREIGN KEY (return_id) REFERENCES rental_returns (id) ON DELETE CASCADE,
    CONSTRAINT fk_rental_return_lines_rental_line FOREIGN KEY (rental_line_id) REFERENCES rental_receipt_lines (id),
    CONSTRAINT fk_rental_return_lines_serial FOREIGN KEY (serial_id) REFERENCES product_serials (id),
    CONSTRAINT ck_rental_return_lines_qty CHECK (quantity > 0),
    CONSTRAINT ck_rental_return_lines_condition CHECK (item_condition IN ('GOOD','DAMAGED','LOST')),
    CONSTRAINT ck_rental_return_lines_fee CHECK (damage_fee >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 8. TRẢ HÀNG BÁN / ĐỔI HÀNG / BẢO HÀNH  (Phase 2 — đóng gap G-3)
-- ---------------------------------------------------------------------

CREATE TABLE sales_returns (
    id                         BIGINT        NOT NULL AUTO_INCREMENT,
    code                       VARCHAR(30)   NOT NULL,   -- TH-2026-00001
    export_receipt_id          BIGINT        NOT NULL,
    customer_id                BIGINT        NOT NULL,
    return_type                VARCHAR(20)   NOT NULL,   -- REFUND, EXCHANGE, WARRANTY
    return_date                DATE          NOT NULL,
    days_since_delivery        INT           NOT NULL DEFAULT 0,
    fee_policy_id              BIGINT        NULL,
    fee_percent                DECIMAL(5,2)  NOT NULL DEFAULT 0,   -- snapshot từ policy
    original_value             DECIMAL(18,2) NOT NULL DEFAULT 0,
    fee_amount                 DECIMAL(18,2) NOT NULL DEFAULT 0,
    refund_amount              DECIMAL(18,2) NOT NULL DEFAULT 0,
    exchange_export_receipt_id BIGINT        NULL,               -- phiếu xuất hàng đổi
    status                     VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    approved_by                BIGINT        NULL,
    approved_at                DATETIME      NULL,
    reject_reason              VARCHAR(255)  NULL,
    note                       VARCHAR(500)  NULL,
    created_by                 BIGINT        NOT NULL,
    created_at                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version                    INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sales_returns_code (code),
    KEY idx_sales_returns_export (export_receipt_id),
    KEY idx_sales_returns_customer (customer_id),
    CONSTRAINT fk_sales_returns_export FOREIGN KEY (export_receipt_id) REFERENCES export_receipts (id),
    CONSTRAINT fk_sales_returns_customer FOREIGN KEY (customer_id) REFERENCES partners (id),
    CONSTRAINT fk_sales_returns_policy FOREIGN KEY (fee_policy_id) REFERENCES return_fee_policies (id),
    CONSTRAINT fk_sales_returns_exchange FOREIGN KEY (exchange_export_receipt_id) REFERENCES export_receipts (id),
    CONSTRAINT fk_sales_returns_approved_by FOREIGN KEY (approved_by) REFERENCES users (id),
    CONSTRAINT fk_sales_returns_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_sales_returns_type CHECK (return_type IN ('REFUND','EXCHANGE','WARRANTY')),
    CONSTRAINT ck_sales_returns_status CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    CONSTRAINT ck_sales_returns_amounts CHECK (original_value >= 0 AND fee_amount >= 0 AND refund_amount >= 0
                                               AND fee_percent >= 0 AND fee_percent <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sales_return_lines (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    sales_return_id BIGINT       NOT NULL,
    export_line_id  BIGINT       NOT NULL,
    serial_id       BIGINT       NULL,
    quantity        INT          NOT NULL,
    item_condition  VARCHAR(20)  NOT NULL DEFAULT 'GOOD',
    disposition     VARCHAR(20)  NOT NULL DEFAULT 'RESTOCK',  -- nhập lại kho / hàng hỏng / gửi sửa bảo hành
    note            VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_sales_return_lines_return (sales_return_id),
    KEY idx_sales_return_lines_export_line (export_line_id),
    CONSTRAINT fk_sales_return_lines_return FOREIGN KEY (sales_return_id) REFERENCES sales_returns (id) ON DELETE CASCADE,
    CONSTRAINT fk_sales_return_lines_export_line FOREIGN KEY (export_line_id) REFERENCES export_receipt_lines (id),
    CONSTRAINT fk_sales_return_lines_serial FOREIGN KEY (serial_id) REFERENCES product_serials (id),
    CONSTRAINT ck_sales_return_lines_qty CHECK (quantity > 0),
    CONSTRAINT ck_sales_return_lines_condition CHECK (item_condition IN ('GOOD','DAMAGED')),
    CONSTRAINT ck_sales_return_lines_disposition CHECK (disposition IN ('RESTOCK','DAMAGED','WARRANTY_REPAIR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 9. THANH TOÁN & CÔNG NỢ  (mới — lịch sử thanh toán cho KT-03)
-- ---------------------------------------------------------------------

-- Mỗi lần thu/chi là 1 dòng. Sai thì huỷ (is_voided), không sửa/xoá.
-- Mỗi payment gắn với đúng 1 chứng từ.
CREATE TABLE payments (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    code              VARCHAR(30)   NOT NULL,        -- TTN-2026-00001
    partner_id        BIGINT        NOT NULL,
    direction         VARCHAR(3)    NOT NULL,        -- IN = thu, OUT = chi
    purpose           VARCHAR(20)   NOT NULL,
    export_receipt_id BIGINT        NULL,
    rental_receipt_id BIGINT        NULL,
    import_receipt_id BIGINT        NULL,
    sales_return_id   BIGINT        NULL,
    amount            DECIMAL(18,2) NOT NULL,
    method            VARCHAR(20)   NOT NULL DEFAULT 'CASH',
    paid_at           DATETIME      NOT NULL,
    reference_no      VARCHAR(100)  NULL,            -- mã giao dịch ngân hàng
    note              VARCHAR(255)  NULL,
    is_voided         BOOLEAN       NOT NULL DEFAULT FALSE,
    voided_by         BIGINT        NULL,
    voided_at         DATETIME      NULL,
    void_reason       VARCHAR(255)  NULL,
    created_by        BIGINT        NOT NULL,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payments_code (code),
    KEY idx_payments_partner_time (partner_id, paid_at),
    KEY idx_payments_export (export_receipt_id),
    KEY idx_payments_rental (rental_receipt_id),
    KEY idx_payments_import (import_receipt_id),
    KEY idx_payments_sales_return (sales_return_id),
    CONSTRAINT fk_payments_partner FOREIGN KEY (partner_id) REFERENCES partners (id),
    CONSTRAINT fk_payments_export FOREIGN KEY (export_receipt_id) REFERENCES export_receipts (id),
    CONSTRAINT fk_payments_rental FOREIGN KEY (rental_receipt_id) REFERENCES rental_receipts (id),
    CONSTRAINT fk_payments_import FOREIGN KEY (import_receipt_id) REFERENCES import_receipts (id),
    CONSTRAINT fk_payments_sales_return FOREIGN KEY (sales_return_id) REFERENCES sales_returns (id),
    CONSTRAINT fk_payments_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_payments_voided_by FOREIGN KEY (voided_by) REFERENCES users (id),
    CONSTRAINT ck_payments_direction CHECK (direction IN ('IN','OUT')),
    CONSTRAINT ck_payments_purpose CHECK (purpose IN
        ('SALE','RENTAL_FEE','DEPOSIT','DEPOSIT_REFUND','PENALTY','SALES_REFUND','PURCHASE')),
    CONSTRAINT ck_payments_method CHECK (method IN ('CASH','BANK_TRANSFER','CARD','OTHER')),
    CONSTRAINT ck_payments_amount CHECK (amount > 0),
    CONSTRAINT ck_payments_one_ref CHECK (
        (export_receipt_id IS NOT NULL) + (rental_receipt_id IS NOT NULL)
      + (import_receipt_id IS NOT NULL) + (sales_return_id IS NOT NULL) = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 10. KIỂM KÊ & ĐIỀU CHỈNH TỒN
-- ---------------------------------------------------------------------

CREATE TABLE stocktakes (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    code           VARCHAR(30)  NOT NULL,            -- KK-2026-00001
    warehouse_id   BIGINT       NOT NULL,
    stocktake_date DATE         NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    note           VARCHAR(500) NULL,
    created_by     BIGINT       NOT NULL,
    submitted_at   DATETIME     NULL,
    approved_by    BIGINT       NULL,
    approved_at    DATETIME     NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version        INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stocktakes_code (code),
    KEY idx_stocktakes_wh_date (warehouse_id, stocktake_date),
    CONSTRAINT fk_stocktakes_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_stocktakes_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_stocktakes_approved_by FOREIGN KEY (approved_by) REFERENCES users (id),
    CONSTRAINT ck_stocktakes_status CHECK (status IN ('DRAFT','COUNTING','SUBMITTED','APPROVED','CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE stocktake_lines (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    stocktake_id BIGINT       NOT NULL,
    product_id   BIGINT       NOT NULL,
    system_qty   INT          NOT NULL,              -- snapshot qty_on_hand lúc tạo phiếu
    actual_qty   INT          NULL,                  -- NULL = chưa đếm
    difference   INT          AS (actual_qty - system_qty) STORED,
    note         VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stocktake_lines_product (stocktake_id, product_id),
    CONSTRAINT fk_stocktake_lines_stocktake FOREIGN KEY (stocktake_id) REFERENCES stocktakes (id) ON DELETE CASCADE,
    CONSTRAINT fk_stocktake_lines_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_stocktake_lines_qty CHECK (system_qty >= 0 AND (actual_qty IS NULL OR actual_qty >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE stock_adjustments (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    code          VARCHAR(30)  NOT NULL,             -- DC-2026-00001
    warehouse_id  BIGINT       NOT NULL,
    stocktake_id  BIGINT       NULL,                 -- sinh tự động từ kiểm kê, hoặc NULL nếu lập tay
    reason_type   VARCHAR(20)  NOT NULL,
    reason        VARCHAR(500) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_by    BIGINT       NOT NULL,
    approved_by   BIGINT       NULL,
    approved_at   DATETIME     NULL,
    reject_reason VARCHAR(255) NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version       INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_adjustments_code (code),
    UNIQUE KEY uk_stock_adjustments_stocktake (stocktake_id),
    KEY idx_stock_adjustments_status (status),
    CONSTRAINT fk_stock_adjustments_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_stock_adjustments_stocktake FOREIGN KEY (stocktake_id) REFERENCES stocktakes (id),
    CONSTRAINT fk_stock_adjustments_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_stock_adjustments_approved_by FOREIGN KEY (approved_by) REFERENCES users (id),
    CONSTRAINT ck_stock_adjustments_reason CHECK (reason_type IN ('STOCKTAKE','DAMAGE','LOSS','FOUND','OTHER')),
    CONSTRAINT ck_stock_adjustments_status CHECK (status IN ('PENDING','APPROVED','REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE stock_adjustment_lines (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    adjustment_id BIGINT       NOT NULL,
    product_id    BIGINT       NOT NULL,
    serial_id     BIGINT       NULL,
    qty_change    INT          NOT NULL,             -- + thừa, - thiếu
    note          VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_stock_adjustment_lines_adjustment (adjustment_id),
    CONSTRAINT fk_stock_adjustment_lines_adjustment FOREIGN KEY (adjustment_id) REFERENCES stock_adjustments (id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_adjustment_lines_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_stock_adjustment_lines_serial FOREIGN KEY (serial_id) REFERENCES product_serials (id),
    CONSTRAINT ck_stock_adjustment_lines_qty CHECK (qty_change <> 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 11. CẢNH BÁO
-- ---------------------------------------------------------------------

CREATE TABLE alerts (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    alert_type     VARCHAR(30)  NOT NULL,
    severity       VARCHAR(10)  NOT NULL DEFAULT 'WARNING',
    title          VARCHAR(200) NOT NULL,
    message        VARCHAR(500) NULL,
    ref_type       VARCHAR(30)  NULL,                -- PRODUCT, RENTAL_RECEIPT, EXPORT_RECEIPT...
    ref_id         BIGINT       NULL,
    target_role_id BIGINT       NULL,                -- NULL = mọi vai trò
    dedupe_key     VARCHAR(150) NULL,                -- vd 'LOW_STOCK:1:25'; đặt NULL khi resolve để lần sau sinh lại được
    is_resolved    BOOLEAN      NOT NULL DEFAULT FALSE,
    resolved_at    DATETIME     NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_alerts_dedupe (dedupe_key),
    KEY idx_alerts_open (is_resolved, created_at),
    KEY idx_alerts_ref (ref_type, ref_id),
    CONSTRAINT fk_alerts_role FOREIGN KEY (target_role_id) REFERENCES roles (id),
    CONSTRAINT ck_alerts_type CHECK (alert_type IN ('LOW_STOCK','RENTAL_EXPIRING','RENTAL_OVERDUE','PENDING_APPROVAL')),
    CONSTRAINT ck_alerts_severity CHECK (severity IN ('INFO','WARNING','CRITICAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Trạng thái "đã đọc" theo từng người (badge chưa đọc trên Navbar)
CREATE TABLE alert_reads (
    alert_id BIGINT   NOT NULL,
    user_id  BIGINT   NOT NULL,
    read_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (alert_id, user_id),
    KEY idx_alert_reads_user (user_id),
    CONSTRAINT fk_alert_reads_alert FOREIGN KEY (alert_id) REFERENCES alerts (id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_reads_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
