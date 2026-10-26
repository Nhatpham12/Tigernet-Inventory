-- =====================================================================
-- TIGERNET — Tenant database v7.1 (ERD v1.2) — Flyway V4
-- Nguồn: docs/tigernet_erd_v1_2.drawio.png + docs/Database_Design_v7.md
-- Cập nhật từ v7 (V1..V3). Không sửa V1..V3 để giữ checksum Flyway.
--
-- Thay đổi:
--   1. Bảng mới   : Payments, Supplier_Returns, Supplier_Return_Details, Warranty_Claims
--   2. Import_Receipts : VAT_ID, Payment_Status_ID, Paid, Remain, Cancelled_By/At, Cancel_Reason, trạng thái CANCELLED
--   3. Export_Receipts : Cancelled_By/At, Cancel_Reason, trạng thái CANCELLED
--   4. Rental_Receipts : Status, Approved_By, Approved_At (bước duyệt phiếu thuê/mượn)
--   5. Return_Receipts : Compensation_Amount, Deposit_Refund
--   6. Stock_Movements : thêm Reference_Type 'Supplier_Returns', 'Warranty_Claims'
-- Dữ liệu cũ: phiếu thuê/mượn đã có coi như APPROVED; phiếu nhập cũ còn nợ toàn bộ (Remain = Total_Amount).
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- 1. PHIẾU NHẬP: VAT, CÔNG NỢ PHẢI TRẢ NCC, HỦY CÓ LƯU VẾT
-- =====================================================================
ALTER TABLE Import_Receipts
    ADD COLUMN VAT_ID            INT           NULL AFTER Date_In,
    ADD COLUMN Payment_Status_ID INT           NULL AFTER Total_Amount,
    ADD COLUMN Paid              DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER Payment_Status_ID,
    ADD COLUMN Remain            DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER Paid,
    ADD COLUMN Cancelled_By      INT           NULL AFTER Created_At,
    ADD COLUMN Cancelled_At      DATETIME      NULL AFTER Cancelled_By,
    ADD COLUMN Cancel_Reason     VARCHAR(255)  NULL AFTER Cancelled_At,
    ADD CONSTRAINT FK_Import_VAT FOREIGN KEY (VAT_ID) REFERENCES VAT_Options(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    ADD CONSTRAINT FK_Import_Payment_Status FOREIGN KEY (Payment_Status_ID) REFERENCES Payment_Statuses(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    ADD CONSTRAINT FK_Import_Cancelled_By FOREIGN KEY (Cancelled_By) REFERENCES Users(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    DROP CHECK CK_Import_Status,
    ADD CONSTRAINT CK_Import_Status CHECK (Status IN ('DRAFT','PENDING','CONFIRMED','CANCELLED')),
    ADD CONSTRAINT CK_Import_Money  CHECK (Paid >= 0 AND Remain >= 0);

-- phiếu nhập cũ: chưa ghi nhận thanh toán nào
UPDATE Import_Receipts SET Remain = Total_Amount WHERE Remain = 0 AND Paid = 0;
UPDATE Import_Receipts
   SET Payment_Status_ID = (SELECT ID FROM Payment_Statuses WHERE Payment_Status = 'UNPAID')
 WHERE Payment_Status_ID IS NULL;

CREATE INDEX IX_Import_Payment_Status_ID ON Import_Receipts (Payment_Status_ID);

-- =====================================================================
-- 2. PHIẾU XUẤT: HỦY CÓ LƯU VẾT
-- =====================================================================
ALTER TABLE Export_Receipts
    ADD COLUMN Cancelled_By  INT          NULL AFTER Created_At,
    ADD COLUMN Cancelled_At  DATETIME     NULL AFTER Cancelled_By,
    ADD COLUMN Cancel_Reason VARCHAR(255) NULL AFTER Cancelled_At,
    ADD CONSTRAINT FK_Export_Cancelled_By FOREIGN KEY (Cancelled_By) REFERENCES Users(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    DROP CHECK CK_Export_Status,
    ADD CONSTRAINT CK_Export_Status CHECK (Status IN ('PENDING','CONFIRMED','EXPORTED','CANCELLED'));

-- =====================================================================
-- 3. PHIẾU THUÊ/MƯỢN: BƯỚC DUYỆT
-- Mặc định APPROVED để luồng hiện tại (có hiệu lực ngay khi tạo) không đổi.
-- =====================================================================
ALTER TABLE Rental_Receipts
    ADD COLUMN Status      VARCHAR(20) NOT NULL DEFAULT 'APPROVED' AFTER Return_Date,
    ADD COLUMN Approved_By INT         NULL AFTER Created_At,
    ADD COLUMN Approved_At DATETIME    NULL AFTER Approved_By,
    ADD CONSTRAINT FK_Rental_Approved_By FOREIGN KEY (Approved_By) REFERENCES Users(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    ADD CONSTRAINT CK_Rental_Status CHECK (Status IN ('PENDING','APPROVED','REJECTED'));

UPDATE Rental_Receipts SET Approved_At = Created_At WHERE Approved_At IS NULL;

CREATE INDEX IX_Rental_Status ON Rental_Receipts (Status);

-- =====================================================================
-- 4. PHIẾU TRẢ HÀNG THUÊ: BỒI THƯỜNG & HOÀN CỌC
-- =====================================================================
ALTER TABLE Return_Receipts
    ADD COLUMN Compensation_Amount DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER Penalty_Amount,
    ADD COLUMN Deposit_Refund      DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER Compensation_Amount,
    ADD CONSTRAINT CK_Return_Money CHECK (Compensation_Amount >= 0 AND Deposit_Refund >= 0);

-- =====================================================================
-- 5. PAYMENTS — phiếu thu/chi (mỗi lần thu/chi một dòng)
-- Direction IN = thu (khách trả tiền), OUT = chi (trả NCC, hoàn cọc/tiền trả hàng).
-- (Reference_Type, Reference_ID) trỏ mềm tới phiếu gốc; Paid/Remain trên phiếu
-- là tổng hợp của các dòng COMPLETED. Hủy = Status CANCELLED, không xóa.
-- =====================================================================
CREATE TABLE Payments (
    ID             INT           NOT NULL AUTO_INCREMENT,
    Payment_Code   VARCHAR(30)   NOT NULL,
    Direction      VARCHAR(3)    NOT NULL,
    Party_ID       INT           NOT NULL,
    Reference_Type VARCHAR(30)   NOT NULL,
    Reference_ID   INT           NOT NULL,
    Amount         DECIMAL(18,2) NOT NULL,
    Method         VARCHAR(20)   NOT NULL DEFAULT 'CASH',
    Paid_At        DATETIME      NOT NULL,
    Status         VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED',
    Note           VARCHAR(255)  NULL,
    Recorded_By    INT           NOT NULL,
    Created_At     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_Payments PRIMARY KEY (ID),
    CONSTRAINT UQ_Payments_Code UNIQUE (Payment_Code),
    CONSTRAINT FK_Payments_Party FOREIGN KEY (Party_ID) REFERENCES Parties(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_Payments_Recorded_By FOREIGN KEY (Recorded_By) REFERENCES Users(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT CK_Payments_Direction CHECK (Direction IN ('IN','OUT')),
    CONSTRAINT CK_Payments_Ref_Type  CHECK (Reference_Type IN
        ('Import_Receipts','Export_Receipts','Rental_Receipts','Return_Receipts','Sales_Returns','Supplier_Returns')),
    CONSTRAINT CK_Payments_Amount    CHECK (Amount > 0),
    CONSTRAINT CK_Payments_Method    CHECK (Method IN ('CASH','TRANSFER')),
    CONSTRAINT CK_Payments_Status    CHECK (Status IN ('COMPLETED','CANCELLED'))
) ENGINE=InnoDB;

CREATE INDEX IX_Payments_Reference ON Payments (Reference_Type, Reference_ID);
CREATE INDEX IX_Payments_Party     ON Payments (Party_ID, Paid_At);
CREATE INDEX IX_Payments_Paid_At   ON Payments (Paid_At);

-- =====================================================================
-- 6. SUPPLIER_RETURNS — trả hàng lỗi / gửi bảo hành lên NCC (RMA)
-- =====================================================================
CREATE TABLE Supplier_Returns (
    ID                  INT           NOT NULL AUTO_INCREMENT,
    Return_Code         VARCHAR(30)   NOT NULL,
    Supplier_ID         INT           NOT NULL,
    Warehouse_ID        INT           NOT NULL,
    Status              VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    Reason              VARCHAR(255)  NULL,
    Sent_At             DATETIME      NULL,
    Received_Back_At    DATETIME      NULL,
    Compensation_Amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    Note                TEXT          NULL,
    Created_By          INT           NOT NULL,
    Created_At          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_Supplier_Returns PRIMARY KEY (ID),
    CONSTRAINT UQ_Supplier_Returns_Code UNIQUE (Return_Code),
    CONSTRAINT FK_SupR_Supplier FOREIGN KEY (Supplier_ID) REFERENCES Suppliers(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_SupR_Warehouse FOREIGN KEY (Warehouse_ID) REFERENCES Warehouses(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_SupR_Created_By FOREIGN KEY (Created_By) REFERENCES Users(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT CK_SupR_Status CHECK (Status IN ('DRAFT','SENT','RECEIVED_BACK','CLOSED','CANCELLED')),
    CONSTRAINT CK_SupR_Compensation CHECK (Compensation_Amount >= 0),
    CONSTRAINT CK_SupR_Dates CHECK (Received_Back_At IS NULL OR Sent_At IS NULL OR Received_Back_At >= Sent_At)
) ENGINE=InnoDB;

CREATE INDEX IX_SupR_Supplier  ON Supplier_Returns (Supplier_ID, Status);
CREATE INDEX IX_SupR_Warehouse ON Supplier_Returns (Warehouse_ID, Status);

CREATE TABLE Supplier_Return_Details (
    ID                 INT          NOT NULL AUTO_INCREMENT,
    Supplier_Return_ID INT          NOT NULL,
    Product_ID         INT          NOT NULL,
    Inventory_ID       INT          NULL,
    Quantity           INT          NOT NULL,
    Serial             VARCHAR(255) NULL,
    Result             VARCHAR(20)  NULL,
    CONSTRAINT PK_Supplier_Return_Details PRIMARY KEY (ID),
    CONSTRAINT FK_SupRD_Return FOREIGN KEY (Supplier_Return_ID) REFERENCES Supplier_Returns(ID)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FK_SupRD_Product FOREIGN KEY (Product_ID) REFERENCES Products(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_SupRD_Inventory FOREIGN KEY (Inventory_ID) REFERENCES Inventory(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT CK_SupRD_Quantity CHECK (Quantity > 0),
    CONSTRAINT CK_SupRD_Result   CHECK (Result IS NULL OR Result IN ('REPAIRED','REPLACED','REFUNDED','REJECTED'))
) ENGINE=InnoDB;

CREATE INDEX IX_SupRD_Inventory ON Supplier_Return_Details (Inventory_ID);

-- =====================================================================
-- 7. WARRANTY_CLAIMS — vòng đời một lần bảo hành của khách
-- RECEIVED (nhận máy) → SENT (gửi đi sửa) → RETURNED (máy về) → CLOSED (trả khách)
-- hoặc REJECTED (từ chối bảo hành). Máy đổi mới ghi Replacement_Serial và
-- New_Warranty_Expiry. Restocked_Warehouse_ID: kho nhận lại hàng sau bảo hành.
-- =====================================================================
CREATE TABLE Warranty_Claims (
    ID                      INT           NOT NULL AUTO_INCREMENT,
    Claim_Code              VARCHAR(30)   NOT NULL,
    Export_Detail_ID        INT           NOT NULL,
    Inventory_ID            INT           NULL,
    Received_Warehouse_ID   INT           NOT NULL,
    Restocked_Warehouse_ID  INT           NULL,
    Status                  VARCHAR(20)   NOT NULL DEFAULT 'RECEIVED',
    Received_At             DATETIME      NOT NULL,
    Sent_At                 DATETIME      NULL,
    Returned_At             DATETIME      NULL,
    Result                  VARCHAR(20)   NULL,
    Cost                    DECIMAL(18,2) NOT NULL DEFAULT 0,
    Replacement_Serial      VARCHAR(255)  NULL,
    New_Warranty_Expiry     DATE          NULL,
    Note                    TEXT          NULL,
    Handled_By              INT           NOT NULL,
    Created_At              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_Warranty_Claims PRIMARY KEY (ID),
    CONSTRAINT UQ_Warranty_Claims_Code UNIQUE (Claim_Code),
    CONSTRAINT FK_WC_Export_Detail FOREIGN KEY (Export_Detail_ID) REFERENCES Export_Receipt_Details(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_WC_Inventory FOREIGN KEY (Inventory_ID) REFERENCES Inventory(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT FK_WC_Received_Warehouse FOREIGN KEY (Received_Warehouse_ID) REFERENCES Warehouses(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_WC_Restocked_Warehouse FOREIGN KEY (Restocked_Warehouse_ID) REFERENCES Warehouses(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT FK_WC_Handled_By FOREIGN KEY (Handled_By) REFERENCES Users(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT CK_WC_Status CHECK (Status IN ('RECEIVED','SENT','RETURNED','CLOSED','REJECTED')),
    CONSTRAINT CK_WC_Result CHECK (Result IS NULL OR Result IN ('REPAIRED','REPLACED','REJECTED')),
    CONSTRAINT CK_WC_Cost   CHECK (Cost >= 0)
) ENGINE=InnoDB;

CREATE INDEX IX_WC_Export_Detail ON Warranty_Claims (Export_Detail_ID);
CREATE INDEX IX_WC_Inventory     ON Warranty_Claims (Inventory_ID);
CREATE INDEX IX_WC_Status        ON Warranty_Claims (Status, Received_Warehouse_ID);

-- =====================================================================
-- 8. SỔ BIẾN ĐỘNG TỒN: thêm 2 loại phiếu nguồn mới
-- =====================================================================
ALTER TABLE Stock_Movements
    DROP CHECK CK_SM_Ref_Type,
    ADD CONSTRAINT CK_SM_Ref_Type CHECK (Reference_Type IN
        ('Import_Receipts','Export_Receipts','Rental_Receipts','Return_Receipts',
         'Sales_Returns','Stock_Transfers','Stock_Adjustments','Supplier_Returns','Warranty_Claims'));
