-- =====================================================================
-- TIGERNET – HỆ THỐNG QUẢN LÝ KHO HÀNG (ĐA KHO) – SCRIPT MYSQL v6 (BẢN GỘP)
-- Nguồn: docs/Database_Design_v6.md, docs/TIGERNET_ERD_v6_dark.png
-- Bản gộp để chạy tay 1 lần (dev/on-premise init.sql) =
--   database/tenant/V1__init_schema.sql + V2__seed_reference_data.sql + V3__views.sql
-- Khi đổi schema: sửa/thêm file trong database/tenant/ (Flyway) rồi gộp lại file này.
-- DBMS : MySQL 8.0.16+ (CHECK được thực thi). Charset utf8mb4 / utf8mb4_unicode_ci.
-- =====================================================================

-- Bỏ comment dòng dưới nếu muốn tạo lại từ đầu (XÓA TOÀN BỘ dữ liệu cũ):
-- DROP DATABASE IF EXISTS tigernet;
CREATE DATABASE IF NOT EXISTS tigernet
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE tigernet;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- 1. DANH MỤC NỀN (LOOKUP)
-- =====================================================================

-- 4.1 Roles -----------------------------------------------------------
CREATE TABLE Roles (
    ID          INT          NOT NULL AUTO_INCREMENT,
    Role_Name   VARCHAR(50)  NOT NULL,
    Description VARCHAR(255) NULL,
    CONSTRAINT PK_Roles PRIMARY KEY (ID),
    CONSTRAINT UQ_Roles_Role_Name UNIQUE (Role_Name)
) ENGINE=InnoDB;

-- 4.3 Product_Groups --------------------------------------------------
CREATE TABLE Product_Groups (
    ID         INT          NOT NULL AUTO_INCREMENT,
    Group_Name VARCHAR(100) NOT NULL,
    Is_Deleted BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT PK_Product_Groups PRIMARY KEY (ID),
    CONSTRAINT UQ_Product_Groups_Name UNIQUE (Group_Name)
) ENGINE=InnoDB;

-- 4.8 VAT_Options -----------------------------------------------------
CREATE TABLE VAT_Options (
    ID   INT          NOT NULL AUTO_INCREMENT,
    VAT  VARCHAR(10)  NOT NULL,
    Rate DECIMAL(5,2) NULL,
    CONSTRAINT PK_VAT_Options PRIMARY KEY (ID),
    CONSTRAINT UQ_VAT_Options_VAT UNIQUE (VAT),
    CONSTRAINT CK_VAT_Options_Rate CHECK (Rate IS NULL OR Rate >= 0)
) ENGINE=InnoDB;

-- 4.9 Payment_Statuses ------------------------------------------------
CREATE TABLE Payment_Statuses (
    ID             INT         NOT NULL AUTO_INCREMENT,
    Payment_Status VARCHAR(50) NOT NULL,
    CONSTRAINT PK_Payment_Statuses PRIMARY KEY (ID),
    CONSTRAINT UQ_Payment_Statuses_Name UNIQUE (Payment_Status)
) ENGINE=InnoDB;

-- 4.23.1a Return_Fee_Policy -------------------------------------------
CREATE TABLE Return_Fee_Policy (
    ID             INT          NOT NULL AUTO_INCREMENT,
    Tier_Name      VARCHAR(30)  NOT NULL,
    Max_Days       INT          NOT NULL,
    Action_Type    VARCHAR(30)  NOT NULL,
    Fee_Percent    DECIMAL(5,2) NOT NULL,
    Is_Allowed     BOOLEAN      NOT NULL DEFAULT TRUE,
    Effective_From DATE         NULL,
    CONSTRAINT PK_Return_Fee_Policy PRIMARY KEY (ID),
    CONSTRAINT UQ_Return_Fee_Policy UNIQUE (Tier_Name, Action_Type, Effective_From),
    CONSTRAINT CK_RFP_Max_Days    CHECK (Max_Days > 0),
    CONSTRAINT CK_RFP_Fee_Percent CHECK (Fee_Percent BETWEEN 0 AND 100),
    CONSTRAINT CK_RFP_Action_Type CHECK (Action_Type IN ('REFUND','EXCHANGE_EQUAL_HIGHER','EXCHANGE_LOWER'))
) ENGINE=InnoDB;

-- =====================================================================
-- 2. THỰC THỂ CHỦ (MASTER)
-- =====================================================================

-- 4.2 Users -----------------------------------------------------------
CREATE TABLE Users (
    ID            INT          NOT NULL AUTO_INCREMENT,
    Username      VARCHAR(50)  NOT NULL,
    Password_Hash VARCHAR(255) NOT NULL,
    Full_Name     VARCHAR(100) NULL,
    Email         VARCHAR(150) NULL,
    Role_ID       INT          NOT NULL,
    Is_Active     BOOLEAN      NOT NULL DEFAULT TRUE,
    Created_At    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_Users PRIMARY KEY (ID),
    CONSTRAINT UQ_Users_Username UNIQUE (Username),
    CONSTRAINT UQ_Users_Email    UNIQUE (Email),
    CONSTRAINT FK_Users_Role FOREIGN KEY (Role_ID) REFERENCES Roles(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 4.2a Warehouses [v6] ------------------------------------------------
CREATE TABLE Warehouses (
    ID             INT          NOT NULL AUTO_INCREMENT,
    Warehouse_Code VARCHAR(20)  NOT NULL,
    Warehouse_Name VARCHAR(150) NOT NULL,
    Address        VARCHAR(255) NOT NULL,
    Phone          VARCHAR(50)  NULL,
    Manager_ID     INT          NULL,
    Is_Active      BOOLEAN      NOT NULL DEFAULT TRUE,
    Created_At     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_Warehouses PRIMARY KEY (ID),
    CONSTRAINT UQ_Warehouses_Code UNIQUE (Warehouse_Code),
    CONSTRAINT UQ_Warehouses_Name UNIQUE (Warehouse_Name),
    CONSTRAINT FK_Warehouses_Manager FOREIGN KEY (Manager_ID) REFERENCES Users(ID)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

-- 4.2b User_Warehouses [v6] -------------------------------------------
-- Mỗi user tối đa 1 dòng Is_Default = TRUE: kiểm tra ở tầng service.
CREATE TABLE User_Warehouses (
    User_ID      INT      NOT NULL,
    Warehouse_ID INT      NOT NULL,
    Is_Default   BOOLEAN  NOT NULL DEFAULT FALSE,
    Assigned_At  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_User_Warehouses PRIMARY KEY (User_ID, Warehouse_ID),
    CONSTRAINT FK_UW_User FOREIGN KEY (User_ID) REFERENCES Users(ID)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FK_UW_Warehouse FOREIGN KEY (Warehouse_ID) REFERENCES Warehouses(ID)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX IX_UW_Warehouse ON User_Warehouses (Warehouse_ID);

-- 4.4 Products --------------------------------------------------------
CREATE TABLE Products (
    ID                INT           NOT NULL AUTO_INCREMENT,
    Product_Name      VARCHAR(255)  NOT NULL,
    Group_ID          INT           NOT NULL,
    Unit              VARCHAR(30)   NOT NULL,
    Default_Price_In  DECIMAL(18,2) NOT NULL DEFAULT 0,
    Default_Price_Out DECIMAL(18,2) NOT NULL DEFAULT 0,
    Is_Serialized     BOOLEAN       NOT NULL DEFAULT TRUE,
    Min_Stock_Level   INT           NOT NULL DEFAULT 0,
    Image_Path        VARCHAR(255)  NULL,
    Is_Deleted        BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT PK_Products PRIMARY KEY (ID),
    CONSTRAINT UQ_Products_Name_Group UNIQUE (Product_Name, Group_ID),
    CONSTRAINT FK_Products_Group FOREIGN KEY (Group_ID) REFERENCES Product_Groups(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT CK_Products_Price_In  CHECK (Default_Price_In  >= 0),
    CONSTRAINT CK_Products_Price_Out CHECK (Default_Price_Out >= 0),
    CONSTRAINT CK_Products_Min_Stock CHECK (Min_Stock_Level   >= 0)
) ENGINE=InnoDB;

CREATE INDEX IX_Products_Name     ON Products (Product_Name);
CREATE INDEX IX_Products_Group_ID ON Products (Group_ID);

-- 4.5 Parties ---------------------------------------------------------
CREATE TABLE Parties (
    ID          INT          NOT NULL AUTO_INCREMENT,
    Party_Type  VARCHAR(20)  NOT NULL,
    Full_Name   VARCHAR(255) NOT NULL,
    Tax_Code    VARCHAR(30)  NULL,
    National_ID VARCHAR(20)  NULL,
    Address     VARCHAR(255) NULL,
    Phone       VARCHAR(50)  NULL,
    Email       VARCHAR(150) NULL,
    Is_Deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT PK_Parties PRIMARY KEY (ID),
    CONSTRAINT UQ_Parties_Tax_Code UNIQUE (Tax_Code),
    CONSTRAINT CK_Parties_Type CHECK (Party_Type IN ('INDIVIDUAL','ORGANIZATION'))
) ENGINE=InnoDB;

CREATE INDEX IX_Parties_Full_Name   ON Parties (Full_Name);
CREATE INDEX IX_Parties_National_ID ON Parties (National_ID);

-- 4.6 Suppliers -------------------------------------------------------
CREATE TABLE Suppliers (
    ID            INT          NOT NULL AUTO_INCREMENT,
    Party_ID      INT          NOT NULL,
    Contract_No   VARCHAR(100) NULL,
    Tracking_Code VARCHAR(100) NULL,
    Is_Deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT PK_Suppliers PRIMARY KEY (ID),
    CONSTRAINT UQ_Suppliers_Party UNIQUE (Party_ID),
    CONSTRAINT FK_Suppliers_Party FOREIGN KEY (Party_ID) REFERENCES Parties(ID)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4.6.1 Supplier_Bank_Accounts ----------------------------------------
CREATE TABLE Supplier_Bank_Accounts (
    ID                INT          NOT NULL AUTO_INCREMENT,
    Supplier_ID       INT          NOT NULL,
    Account_Name      VARCHAR(150) NOT NULL,
    Account_Number    VARCHAR(50)  NOT NULL,
    Bank_Name         VARCHAR(150) NOT NULL,
    Bank_Payment_Code VARCHAR(100) NULL,
    CONSTRAINT PK_Supplier_Bank_Accounts PRIMARY KEY (ID),
    CONSTRAINT FK_SBA_Supplier FOREIGN KEY (Supplier_ID) REFERENCES Suppliers(ID)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4.6.2 Supplier_Documents --------------------------------------------
CREATE TABLE Supplier_Documents (
    ID          INT          NOT NULL AUTO_INCREMENT,
    Supplier_ID INT          NOT NULL,
    Doc_Type    VARCHAR(100) NULL,
    File_Path   VARCHAR(255) NOT NULL,
    Uploaded_At DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_Supplier_Documents PRIMARY KEY (ID),
    CONSTRAINT FK_SD_Supplier FOREIGN KEY (Supplier_ID) REFERENCES Suppliers(ID)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4.7 Customers -------------------------------------------------------
CREATE TABLE Customers (
    ID         INT     NOT NULL AUTO_INCREMENT,
    Party_ID   INT     NOT NULL,
    Is_Deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT PK_Customers PRIMARY KEY (ID),
    CONSTRAINT UQ_Customers_Party UNIQUE (Party_ID),
    CONSTRAINT FK_Customers_Party FOREIGN KEY (Party_ID) REFERENCES Parties(ID)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4.7.1 Receivers -----------------------------------------------------
CREATE TABLE Receivers (
    ID            INT          NOT NULL AUTO_INCREMENT,
    Customer_ID   INT          NOT NULL,
    Receiver_Name VARCHAR(150) NOT NULL,
    Address       VARCHAR(255) NULL,
    Phone         VARCHAR(50)  NULL,
    Is_Deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT PK_Receivers PRIMARY KEY (ID),
    CONSTRAINT FK_Receivers_Customer FOREIGN KEY (Customer_ID) REFERENCES Customers(ID)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================================
-- 3. NHẬP KHO & TỒN KHO
-- =====================================================================

-- 4.10 Import_Receipts ------------------------------------------------
CREATE TABLE Import_Receipts (
    ID              INT           NOT NULL AUTO_INCREMENT,
    Receipt_Code    VARCHAR(30)   NOT NULL,
    Source_Party_ID INT           NOT NULL,
    Warehouse_ID    INT           NOT NULL,
    Date_In         DATETIME      NOT NULL,
    Total_Amount    DECIMAL(18,2) NOT NULL DEFAULT 0,
    Status          VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    Note            TEXT          NULL,
    Created_By      INT           NOT NULL,
    Created_At      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_Import_Receipts PRIMARY KEY (ID),
    CONSTRAINT UQ_Import_Receipts_Code UNIQUE (Receipt_Code),
    CONSTRAINT FK_Import_Source_Party FOREIGN KEY (Source_Party_ID) REFERENCES Parties(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_Import_Warehouse FOREIGN KEY (Warehouse_ID) REFERENCES Warehouses(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_Import_Created_By FOREIGN KEY (Created_By) REFERENCES Users(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT CK_Import_Total  CHECK (Total_Amount >= 0),
    CONSTRAINT CK_Import_Status CHECK (Status IN ('DRAFT','PENDING','CONFIRMED'))
) ENGINE=InnoDB;

CREATE INDEX IX_Import_Source_Party_ID ON Import_Receipts (Source_Party_ID);
CREATE INDEX IX_Import_Date_In         ON Import_Receipts (Date_In);
CREATE INDEX IX_Import_Warehouse_Date  ON Import_Receipts (Warehouse_ID, Date_In);

-- 4.11 Import_Receipt_Details -----------------------------------------
CREATE TABLE Import_Receipt_Details (
    ID                INT           NOT NULL AUTO_INCREMENT,
    Import_Receipt_ID INT           NOT NULL,
    Product_ID        INT           NOT NULL,
    Quantity          INT           NOT NULL,
    Price_In          DECIMAL(18,2) NOT NULL,
    Serial            VARCHAR(255)  NULL,
    Tracking_Code     VARCHAR(100)  NULL,
    Line_Total        DECIMAL(18,2) NOT NULL,
    CONSTRAINT PK_Import_Receipt_Details PRIMARY KEY (ID),
    CONSTRAINT FK_IRD_Receipt FOREIGN KEY (Import_Receipt_ID) REFERENCES Import_Receipts(ID)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FK_IRD_Product FOREIGN KEY (Product_ID) REFERENCES Products(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT CK_IRD_Quantity CHECK (Quantity > 0),
    CONSTRAINT CK_IRD_Price_In CHECK (Price_In >= 0)
) ENGINE=InnoDB;

-- 4.16 Inventory ------------------------------------------------------
-- Serial UNIQUE toàn hệ thống (NULL được phép lặp cho hàng không serial).
-- Hàng thuê trả về dùng lại chính dòng Inventory cũ (cập nhật Status),
-- nên không sinh serial trùng.
CREATE TABLE Inventory (
    ID               INT           NOT NULL AUTO_INCREMENT,
    Product_ID       INT           NOT NULL,
    Warehouse_ID     INT           NOT NULL,
    Serial           VARCHAR(255)  NULL,
    Import_Detail_ID INT           NULL,
    Quantity_On_Hand INT           NOT NULL DEFAULT 0,
    Status           VARCHAR(20)   NOT NULL DEFAULT 'IN_STOCK',
    Price_In         DECIMAL(18,2) NULL,
    Note             TEXT          NULL,
    Date_In          DATETIME      NULL,
    Date_Out         DATETIME      NULL,
    CONSTRAINT PK_Inventory PRIMARY KEY (ID),
    CONSTRAINT UQ_Inventory_Serial UNIQUE (Serial),
    CONSTRAINT FK_Inventory_Product FOREIGN KEY (Product_ID) REFERENCES Products(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_Inventory_Warehouse FOREIGN KEY (Warehouse_ID) REFERENCES Warehouses(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_Inventory_Import_Detail FOREIGN KEY (Import_Detail_ID) REFERENCES Import_Receipt_Details(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT CK_Inventory_Qty    CHECK (Quantity_On_Hand >= 0),
    CONSTRAINT CK_Inventory_Status CHECK (Status IN ('IN_STOCK','SOLD','RENTED','LENT','DAMAGED','RESERVED','IN_TRANSIT'))
) ENGINE=InnoDB;

-- IX_Inventory_Serial: đã có sẵn qua UQ_Inventory_Serial.
CREATE INDEX IX_Inventory_Product_ID       ON Inventory (Product_ID);
CREATE INDEX IX_Inventory_Date_In          ON Inventory (Date_In);
CREATE INDEX IX_Inventory_Date_Out         ON Inventory (Date_Out);
CREATE INDEX IX_Inventory_Wh_Product_Status ON Inventory (Warehouse_ID, Product_ID, Status);

-- 4.24 Stock_Summary [v6] ---------------------------------------------
CREATE TABLE Stock_Summary (
    Product_ID      INT      NOT NULL,
    Warehouse_ID    INT      NOT NULL,
    Qty_On_Hand     INT      NOT NULL DEFAULT 0,
    Qty_Reserved    INT      NOT NULL DEFAULT 0,
    Qty_Rented      INT      NOT NULL DEFAULT 0,
    Qty_Damaged     INT      NOT NULL DEFAULT 0,
    Qty_In_Transit  INT      NOT NULL DEFAULT 0,
    Min_Stock_Level INT      NULL,
    Last_Updated    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT PK_Stock_Summary PRIMARY KEY (Product_ID, Warehouse_ID),
    CONSTRAINT FK_SS_Product FOREIGN KEY (Product_ID) REFERENCES Products(ID)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FK_SS_Warehouse FOREIGN KEY (Warehouse_ID) REFERENCES Warehouses(ID)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT CK_SS_Min_Stock CHECK (Min_Stock_Level IS NULL OR Min_Stock_Level >= 0)
) ENGINE=InnoDB;

CREATE INDEX IX_SS_Warehouse ON Stock_Summary (Warehouse_ID);

-- =====================================================================
-- 4. XUẤT KHO
-- =====================================================================

-- 4.12 Export_Receipts ------------------------------------------------
CREATE TABLE Export_Receipts (
    ID                        INT           NOT NULL AUTO_INCREMENT,
    Receipt_Code              VARCHAR(30)   NOT NULL,
    Customer_ID               INT           NOT NULL,
    Warehouse_ID              INT           NOT NULL,
    Receiver_ID               INT           NULL,
    Receiver_Name_Snapshot    VARCHAR(150)  NULL,
    Receiver_Address_Snapshot VARCHAR(255)  NULL,
    Receiver_Phone_Snapshot   VARCHAR(50)   NULL,
    Date_Out                  DATETIME      NOT NULL,
    Delivery_Date             DATETIME      NULL,
    VAT_ID                    INT           NULL,
    Deposit                   DECIMAL(18,2) NOT NULL DEFAULT 0,
    Total_Amount              DECIMAL(18,2) NOT NULL DEFAULT 0,
    Payment_Status_ID         INT           NULL,
    Paid                      DECIMAL(18,2) NOT NULL DEFAULT 0,
    Remain                    DECIMAL(18,2) NOT NULL DEFAULT 0,
    Status                    VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    Note                      TEXT          NULL,
    Created_By                INT           NOT NULL,
    Created_At                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_Export_Receipts PRIMARY KEY (ID),
    CONSTRAINT UQ_Export_Receipts_Code UNIQUE (Receipt_Code),
    CONSTRAINT FK_Export_Customer FOREIGN KEY (Customer_ID) REFERENCES Customers(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_Export_Warehouse FOREIGN KEY (Warehouse_ID) REFERENCES Warehouses(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_Export_Receiver FOREIGN KEY (Receiver_ID) REFERENCES Receivers(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT FK_Export_VAT FOREIGN KEY (VAT_ID) REFERENCES VAT_Options(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT FK_Export_Payment_Status FOREIGN KEY (Payment_Status_ID) REFERENCES Payment_Statuses(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT FK_Export_Created_By FOREIGN KEY (Created_By) REFERENCES Users(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT CK_Export_Money  CHECK (Deposit >= 0 AND Total_Amount >= 0 AND Paid >= 0),
    CONSTRAINT CK_Export_Status CHECK (Status IN ('PENDING','CONFIRMED','EXPORTED'))
) ENGINE=InnoDB;

CREATE INDEX IX_Export_Customer_ID       ON Export_Receipts (Customer_ID);
CREATE INDEX IX_Export_Date_Out          ON Export_Receipts (Date_Out);
CREATE INDEX IX_Export_Payment_Status_ID ON Export_Receipts (Payment_Status_ID);
CREATE INDEX IX_Export_Warehouse_Date    ON Export_Receipts (Warehouse_ID, Date_Out);

-- 4.13 Export_Receipt_Details -----------------------------------------
CREATE TABLE Export_Receipt_Details (
    ID                INT           NOT NULL AUTO_INCREMENT,
    Export_Receipt_ID INT           NOT NULL,
    Product_ID        INT           NOT NULL,
    Inventory_ID      INT           NULL,
    Quantity          INT           NOT NULL,
    Price_Out         DECIMAL(18,2) NOT NULL,
    Serial            VARCHAR(255)  NULL,
    Warranty_Months   INT           NULL,
    Warranty_Expiry   DATE          NULL,
    Line_Total        DECIMAL(18,2) NOT NULL,
    CONSTRAINT PK_Export_Receipt_Details PRIMARY KEY (ID),
    CONSTRAINT FK_ERD_Receipt FOREIGN KEY (Export_Receipt_ID) REFERENCES Export_Receipts(ID)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FK_ERD_Product FOREIGN KEY (Product_ID) REFERENCES Products(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_ERD_Inventory FOREIGN KEY (Inventory_ID) REFERENCES Inventory(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT CK_ERD_Quantity  CHECK (Quantity > 0),
    CONSTRAINT CK_ERD_Price_Out CHECK (Price_Out >= 0),
    CONSTRAINT CK_ERD_Warranty  CHECK (Warranty_Months IS NULL OR Warranty_Months >= 0)
) ENGINE=InnoDB;

-- =====================================================================
-- 5. CHO THUÊ / MƯỢN & TRẢ HÀNG THUÊ
-- =====================================================================

-- 4.14 Rental_Receipts ------------------------------------------------
CREATE TABLE Rental_Receipts (
    ID                        INT           NOT NULL AUTO_INCREMENT,
    Receipt_Code              VARCHAR(30)   NOT NULL,
    Customer_ID               INT           NOT NULL,
    Warehouse_ID              INT           NOT NULL,
    Receiver_ID               INT           NULL,
    Receiver_Name_Snapshot    VARCHAR(150)  NULL,
    Receiver_Address_Snapshot VARCHAR(255)  NULL,
    Receiver_Phone_Snapshot   VARCHAR(50)   NULL,
    Rental_Type               VARCHAR(10)   NOT NULL DEFAULT 'RENT',
    Date_Out                  DATETIME      NOT NULL,
    Due_Date                  DATETIME      NOT NULL,
    Deposit                   DECIMAL(18,2) NOT NULL DEFAULT 0,
    Rental_Price_Per_Day      DECIMAL(18,2) NOT NULL DEFAULT 0,
    Total_Rent                DECIMAL(18,2) NOT NULL DEFAULT 0,
    Penalty_Amount            DECIMAL(18,2) NOT NULL DEFAULT 0,
    Return_Date               DATETIME      NULL,
    Payment_Status_ID         INT           NULL,
    Created_By                INT           NOT NULL,
    Created_At                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_Rental_Receipts PRIMARY KEY (ID),
    CONSTRAINT UQ_Rental_Receipts_Code UNIQUE (Receipt_Code),
    CONSTRAINT FK_Rental_Customer FOREIGN KEY (Customer_ID) REFERENCES Customers(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_Rental_Warehouse FOREIGN KEY (Warehouse_ID) REFERENCES Warehouses(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_Rental_Receiver FOREIGN KEY (Receiver_ID) REFERENCES Receivers(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT FK_Rental_Payment_Status FOREIGN KEY (Payment_Status_ID) REFERENCES Payment_Statuses(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT FK_Rental_Created_By FOREIGN KEY (Created_By) REFERENCES Users(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT CK_Rental_Type  CHECK (Rental_Type IN ('RENT','LEND')),
    CONSTRAINT CK_Rental_Dates CHECK (Due_Date >= Date_Out),
    CONSTRAINT CK_Rental_Money CHECK (Deposit >= 0 AND Rental_Price_Per_Day >= 0
                                      AND Total_Rent >= 0 AND Penalty_Amount >= 0)
) ENGINE=InnoDB;

CREATE INDEX IX_Rental_Customer_ID ON Rental_Receipts (Customer_ID);
CREATE INDEX IX_Rental_Due_Date    ON Rental_Receipts (Due_Date);
CREATE INDEX IX_Rental_Warehouse   ON Rental_Receipts (Warehouse_ID, Date_Out);

-- 4.15 Rental_Receipt_Details -----------------------------------------
CREATE TABLE Rental_Receipt_Details (
    ID                INT           NOT NULL AUTO_INCREMENT,
    Rental_Receipt_ID INT           NOT NULL,
    Product_ID        INT           NOT NULL,
    Inventory_ID      INT           NULL,
    Quantity          INT           NOT NULL,
    Rental_Price      DECIMAL(18,2) NOT NULL,
    Serial            VARCHAR(255)  NULL,
    Is_Returned       BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT PK_Rental_Receipt_Details PRIMARY KEY (ID),
    CONSTRAINT FK_RRD_Receipt FOREIGN KEY (Rental_Receipt_ID) REFERENCES Rental_Receipts(ID)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FK_RRD_Product FOREIGN KEY (Product_ID) REFERENCES Products(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_RRD_Inventory FOREIGN KEY (Inventory_ID) REFERENCES Inventory(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT CK_RRD_Quantity CHECK (Quantity > 0),
    CONSTRAINT CK_RRD_Price    CHECK (Rental_Price >= 0)
) ENGINE=InnoDB;

-- 4.22 Return_Receipts ------------------------------------------------
CREATE TABLE Return_Receipts (
    ID                INT           NOT NULL AUTO_INCREMENT,
    Receipt_Code      VARCHAR(30)   NOT NULL,
    Rental_Receipt_ID INT           NOT NULL,
    Warehouse_ID      INT           NOT NULL,
    Return_Date       DATETIME      NOT NULL,
    Days_Overdue      INT           NOT NULL DEFAULT 0,
    Penalty_Amount    DECIMAL(18,2) NOT NULL DEFAULT 0,
    Note              TEXT          NULL,
    Created_By        INT           NOT NULL,
    Created_At        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_Return_Receipts PRIMARY KEY (ID),
    CONSTRAINT UQ_Return_Receipts_Code UNIQUE (Receipt_Code),
    CONSTRAINT FK_Return_Rental FOREIGN KEY (Rental_Receipt_ID) REFERENCES Rental_Receipts(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_Return_Warehouse FOREIGN KEY (Warehouse_ID) REFERENCES Warehouses(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_Return_Created_By FOREIGN KEY (Created_By) REFERENCES Users(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT CK_Return_Overdue CHECK (Days_Overdue >= 0),
    CONSTRAINT CK_Return_Penalty CHECK (Penalty_Amount >= 0)
) ENGINE=InnoDB;

CREATE INDEX IX_Return_Rental ON Return_Receipts (Rental_Receipt_ID);

-- 4.23 Return_Receipt_Details -----------------------------------------
CREATE TABLE Return_Receipt_Details (
    ID                INT          NOT NULL AUTO_INCREMENT,
    Return_Receipt_ID INT          NOT NULL,
    Rental_Detail_ID  INT          NOT NULL,
    Product_ID        INT          NOT NULL,
    Inventory_ID      INT          NULL,
    Quantity          INT          NOT NULL,
    Serial            VARCHAR(255) NULL,
    Condition_Note    VARCHAR(255) NULL,
    CONSTRAINT PK_Return_Receipt_Details PRIMARY KEY (ID),
    CONSTRAINT FK_RtRD_Receipt FOREIGN KEY (Return_Receipt_ID) REFERENCES Return_Receipts(ID)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FK_RtRD_Rental_Detail FOREIGN KEY (Rental_Detail_ID) REFERENCES Rental_Receipt_Details(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_RtRD_Product FOREIGN KEY (Product_ID) REFERENCES Products(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_RtRD_Inventory FOREIGN KEY (Inventory_ID) REFERENCES Inventory(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT CK_RtRD_Quantity CHECK (Quantity > 0)
) ENGINE=InnoDB;

-- =====================================================================
-- 6. ĐỔI / TRẢ HÀNG ĐÃ BÁN
-- =====================================================================

-- 4.23.1 Sales_Returns ------------------------------------------------
CREATE TABLE Sales_Returns (
    ID                    INT           NOT NULL AUTO_INCREMENT,
    Receipt_Code          VARCHAR(30)   NOT NULL,
    Export_Receipt_ID     INT           NOT NULL,
    Party_ID              INT           NOT NULL,
    Warehouse_ID          INT           NOT NULL,
    Return_Type           VARCHAR(20)   NOT NULL,
    Exchange_Direction    VARCHAR(20)   NULL,
    Return_Date           DATETIME      NOT NULL,
    Days_Since_Delivery   INT           NULL,
    Policy_ID             INT           NULL,
    Original_Value        DECIMAL(18,2) NOT NULL DEFAULT 0,
    Fee_Percent           DECIMAL(5,2)  NOT NULL DEFAULT 0,
    Fee_Amount            DECIMAL(18,2) NOT NULL DEFAULT 0,
    New_Export_Receipt_ID INT           NULL,
    Refund_Amount         DECIMAL(18,2) NOT NULL DEFAULT 0,
    Status                VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    Note                  TEXT          NULL,
    Created_By            INT           NOT NULL,
    Created_At            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_Sales_Returns PRIMARY KEY (ID),
    CONSTRAINT UQ_Sales_Returns_Code UNIQUE (Receipt_Code),
    CONSTRAINT FK_SR_Export FOREIGN KEY (Export_Receipt_ID) REFERENCES Export_Receipts(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_SR_Party FOREIGN KEY (Party_ID) REFERENCES Parties(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_SR_Warehouse FOREIGN KEY (Warehouse_ID) REFERENCES Warehouses(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_SR_Policy FOREIGN KEY (Policy_ID) REFERENCES Return_Fee_Policy(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT FK_SR_New_Export FOREIGN KEY (New_Export_Receipt_ID) REFERENCES Export_Receipts(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT FK_SR_Created_By FOREIGN KEY (Created_By) REFERENCES Users(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT CK_SR_Return_Type CHECK (Return_Type IN ('REFUND','EXCHANGE','WARRANTY')),
    CONSTRAINT CK_SR_Exchange_Dir CHECK (Exchange_Direction IS NULL
                                         OR Exchange_Direction IN ('EQUAL_HIGHER','LOWER')),
    CONSTRAINT CK_SR_Status CHECK (Status IN ('DRAFT','CONFIRMED','COMPLETED','REJECTED')),
    CONSTRAINT CK_SR_Fee_Percent CHECK (Fee_Percent BETWEEN 0 AND 100),
    CONSTRAINT CK_SR_Money CHECK (Original_Value >= 0 AND Fee_Amount >= 0)
) ENGINE=InnoDB;

CREATE INDEX IX_SalesReturn_Export ON Sales_Returns (Export_Receipt_ID);
CREATE INDEX IX_SalesReturn_Party  ON Sales_Returns (Party_ID);

-- 4.23.2 Sales_Return_Details -----------------------------------------
CREATE TABLE Sales_Return_Details (
    ID               INT          NOT NULL AUTO_INCREMENT,
    Sales_Return_ID  INT          NOT NULL,
    Export_Detail_ID INT          NOT NULL,
    Product_ID       INT          NOT NULL,
    Inventory_ID     INT          NULL,
    Quantity         INT          NOT NULL,
    Serial           VARCHAR(255) NULL,
    Return_Condition VARCHAR(20)  NOT NULL,
    Restock_Status   VARCHAR(20)  NULL,
    CONSTRAINT PK_Sales_Return_Details PRIMARY KEY (ID),
    CONSTRAINT FK_SRD_Sales_Return FOREIGN KEY (Sales_Return_ID) REFERENCES Sales_Returns(ID)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FK_SRD_Export_Detail FOREIGN KEY (Export_Detail_ID) REFERENCES Export_Receipt_Details(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_SRD_Product FOREIGN KEY (Product_ID) REFERENCES Products(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_SRD_Inventory FOREIGN KEY (Inventory_ID) REFERENCES Inventory(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT CK_SRD_Quantity  CHECK (Quantity > 0),
    CONSTRAINT CK_SRD_Condition CHECK (Return_Condition IN ('RESELLABLE','DAMAGED','TO_WARRANTY')),
    CONSTRAINT CK_SRD_Restock   CHECK (Restock_Status IS NULL OR Restock_Status IN
                                       ('IN_STOCK','SOLD','RENTED','LENT','DAMAGED','RESERVED','IN_TRANSIT'))
) ENGINE=InnoDB;

-- =====================================================================
-- 7. KIỂM KÊ & ĐIỀU CHỈNH
-- =====================================================================

-- 4.17 Stocktakes -----------------------------------------------------
CREATE TABLE Stocktakes (
    ID             INT         NOT NULL AUTO_INCREMENT,
    Stocktake_Code VARCHAR(30) NOT NULL,
    Warehouse_ID   INT         NOT NULL,
    Stocktake_Date DATETIME    NOT NULL,
    Status         VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    Created_By     INT         NOT NULL,
    CONSTRAINT PK_Stocktakes PRIMARY KEY (ID),
    CONSTRAINT UQ_Stocktakes_Code UNIQUE (Stocktake_Code),
    CONSTRAINT FK_Stocktake_Warehouse FOREIGN KEY (Warehouse_ID) REFERENCES Warehouses(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_Stocktake_Created_By FOREIGN KEY (Created_By) REFERENCES Users(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT CK_Stocktake_Status CHECK (Status IN ('DRAFT','COMPLETED'))
) ENGINE=InnoDB;

CREATE INDEX IX_Stocktake_Warehouse_Date ON Stocktakes (Warehouse_ID, Stocktake_Date);

-- 4.18 Stocktake_Details ----------------------------------------------
CREATE TABLE Stocktake_Details (
    ID           INT NOT NULL AUTO_INCREMENT,
    Stocktake_ID INT NOT NULL,
    Product_ID   INT NOT NULL,
    System_Qty   INT NOT NULL,
    Actual_Qty   INT NOT NULL,
    Difference   INT NOT NULL,
    CONSTRAINT PK_Stocktake_Details PRIMARY KEY (ID),
    CONSTRAINT UQ_Stocktake_Details UNIQUE (Stocktake_ID, Product_ID),
    CONSTRAINT FK_STD_Stocktake FOREIGN KEY (Stocktake_ID) REFERENCES Stocktakes(ID)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FK_STD_Product FOREIGN KEY (Product_ID) REFERENCES Products(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT CK_STD_Qty CHECK (System_Qty >= 0 AND Actual_Qty >= 0)
) ENGINE=InnoDB;

-- 4.19 Stock_Adjustments ----------------------------------------------
CREATE TABLE Stock_Adjustments (
    ID           INT          NOT NULL AUTO_INCREMENT,
    Stocktake_ID INT          NOT NULL,
    Product_ID   INT          NOT NULL,
    Adjust_Qty   INT          NOT NULL,
    Reason       VARCHAR(255) NULL,
    Created_At   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_Stock_Adjustments PRIMARY KEY (ID),
    CONSTRAINT FK_SA_Stocktake FOREIGN KEY (Stocktake_ID) REFERENCES Stocktakes(ID)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FK_SA_Product FOREIGN KEY (Product_ID) REFERENCES Products(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- =====================================================================
-- 8. CHUYỂN KHO [v6]
-- =====================================================================

-- 4.23.3 Stock_Transfers ----------------------------------------------
CREATE TABLE Stock_Transfers (
    ID                INT         NOT NULL AUTO_INCREMENT,
    Transfer_Code     VARCHAR(30) NOT NULL,
    From_Warehouse_ID INT         NOT NULL,
    To_Warehouse_ID   INT         NOT NULL,
    Transfer_Date     DATETIME    NOT NULL,
    Received_Date     DATETIME    NULL,
    Status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    Note              TEXT        NULL,
    Created_By        INT         NOT NULL,
    Received_By       INT         NULL,
    Created_At        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_Stock_Transfers PRIMARY KEY (ID),
    CONSTRAINT UQ_Stock_Transfers_Code UNIQUE (Transfer_Code),
    -- ON UPDATE RESTRICT: MySQL 8 không cho cột trong CHECK nằm ở FK có ON UPDATE CASCADE.
    CONSTRAINT FK_ST_From_Warehouse FOREIGN KEY (From_Warehouse_ID) REFERENCES Warehouses(ID)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT FK_ST_To_Warehouse FOREIGN KEY (To_Warehouse_ID) REFERENCES Warehouses(ID)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT FK_ST_Created_By FOREIGN KEY (Created_By) REFERENCES Users(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_ST_Received_By FOREIGN KEY (Received_By) REFERENCES Users(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT CK_ST_Diff_Warehouse CHECK (From_Warehouse_ID <> To_Warehouse_ID),
    CONSTRAINT CK_ST_Status CHECK (Status IN ('DRAFT','IN_TRANSIT','RECEIVED','CANCELLED'))
) ENGINE=InnoDB;

CREATE INDEX IX_Transfer_From ON Stock_Transfers (From_Warehouse_ID, Status);
CREATE INDEX IX_Transfer_To   ON Stock_Transfers (To_Warehouse_ID, Status);

-- 4.23.4 Stock_Transfer_Details ---------------------------------------
CREATE TABLE Stock_Transfer_Details (
    ID                INT          NOT NULL AUTO_INCREMENT,
    Stock_Transfer_ID INT          NOT NULL,
    Product_ID        INT          NOT NULL,
    Inventory_ID      INT          NULL,
    Quantity          INT          NOT NULL,
    Received_Qty      INT          NULL,
    Serial            VARCHAR(255) NULL,
    CONSTRAINT PK_Stock_Transfer_Details PRIMARY KEY (ID),
    CONSTRAINT FK_STD2_Transfer FOREIGN KEY (Stock_Transfer_ID) REFERENCES Stock_Transfers(ID)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FK_STD2_Product FOREIGN KEY (Product_ID) REFERENCES Products(ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT FK_STD2_Inventory FOREIGN KEY (Inventory_ID) REFERENCES Inventory(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT CK_STD2_Quantity     CHECK (Quantity > 0),
    CONSTRAINT CK_STD2_Received_Qty CHECK (Received_Qty IS NULL OR Received_Qty >= 0)
) ENGINE=InnoDB;

-- =====================================================================
-- 9. HỖ TRỢ VẬN HÀNH
-- =====================================================================

-- 4.20 Alerts ---------------------------------------------------------
CREATE TABLE Alerts (
    ID           INT          NOT NULL AUTO_INCREMENT,
    Alert_Type   VARCHAR(30)  NOT NULL,
    Warehouse_ID INT          NULL,
    Message      VARCHAR(255) NOT NULL,
    Ref_Table    VARCHAR(50)  NULL,
    Ref_ID       INT          NULL,
    Is_Resolved  BOOLEAN      NOT NULL DEFAULT FALSE,
    Created_At   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_Alerts PRIMARY KEY (ID),
    CONSTRAINT FK_Alerts_Warehouse FOREIGN KEY (Warehouse_ID) REFERENCES Warehouses(ID)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT CK_Alerts_Type CHECK (Alert_Type IN
        ('LOW_STOCK','RENT_DUE','PENDING_APPROVAL','DEBT_OVERDUE','TRANSFER_PENDING'))
) ENGINE=InnoDB;

CREATE INDEX IX_Alerts_Warehouse_Resolved ON Alerts (Warehouse_ID, Is_Resolved);
CREATE INDEX IX_Alerts_Ref                ON Alerts (Ref_Table, Ref_ID);

-- 4.21 Audit_Logs -----------------------------------------------------
CREATE TABLE Audit_Logs (
    ID           BIGINT      NOT NULL AUTO_INCREMENT,
    User_ID      INT         NULL,
    Action       VARCHAR(50) NOT NULL,
    Target_Table VARCHAR(50) NULL,
    Target_ID    INT         NULL,
    Detail       JSON        NULL,
    Created_At   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_Audit_Logs PRIMARY KEY (ID),
    CONSTRAINT FK_Audit_User FOREIGN KEY (User_ID) REFERENCES Users(ID)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX IX_Audit_User_Time ON Audit_Logs (User_ID, Created_At);
CREATE INDEX IX_Audit_Target    ON Audit_Logs (Target_Table, Target_ID);

SET FOREIGN_KEY_CHECKS = 1;

-- Số lượng thực của 1 dòng Inventory: hàng serial = 1 đơn vị, không serial = Quantity_On_Hand.

-- View_Inventory_Full (§6.2.1)
CREATE OR REPLACE VIEW View_Inventory_Full AS
SELECT  i.ID              AS Inventory_ID,
        w.Warehouse_Code,
        w.Warehouse_Name,
        pg.Group_Name,
        p.ID              AS Product_ID,
        p.Product_Name,
        p.Unit,
        i.Serial,
        CASE WHEN i.Serial IS NOT NULL THEN 1 ELSE i.Quantity_On_Hand END AS Quantity,
        i.Status,
        i.Price_In,
        i.Date_In,
        i.Date_Out,
        i.Note
FROM    Inventory i
JOIN    Products       p  ON p.ID  = i.Product_ID
JOIN    Product_Groups pg ON pg.ID = p.Group_ID
JOIN    Warehouses     w  ON w.ID  = i.Warehouse_ID;

-- View_Product_Count_By_Group (§6.2.2)
CREATE OR REPLACE VIEW View_Product_Count_By_Group AS
SELECT  pg.ID         AS Group_ID,
        pg.Group_Name,
        COUNT(p.ID)   AS Product_Count
FROM    Product_Groups pg
LEFT JOIN Products p ON p.Group_ID = pg.ID AND p.Is_Deleted = FALSE
WHERE   pg.Is_Deleted = FALSE
GROUP BY pg.ID, pg.Group_Name;

-- View_Inventory_Summary (§6.2.3) – tổng hợp theo nhóm SP.
-- Paid/Remain nằm ở header phiếu xuất (một phiếu có thể nhiều nhóm) nên
-- không chia được theo nhóm; xem công nợ theo phiếu/khách hàng.
CREATE OR REPLACE VIEW View_Inventory_Summary AS
SELECT  pg.ID         AS Group_ID,
        pg.Group_Name,
        COALESCE(inv.Total_Items_In_Stock, 0) AS Total_Items_In_Stock,
        COALESCE(inv.Total_Stock_Value_In, 0) AS Total_Stock_Value_In,
        COALESCE(imp.Total_Price_In, 0)       AS Total_Price_In,
        COALESCE(exp.Total_Price_Out, 0)      AS Total_Price_Out,
        COALESCE(ren.Total_Rent, 0)           AS Total_Rent
FROM    Product_Groups pg
LEFT JOIN (
        SELECT p.Group_ID,
               SUM(CASE WHEN i.Serial IS NOT NULL THEN 1 ELSE i.Quantity_On_Hand END) AS Total_Items_In_Stock,
               SUM(COALESCE(i.Price_In, 0) *
                   CASE WHEN i.Serial IS NOT NULL THEN 1 ELSE i.Quantity_On_Hand END) AS Total_Stock_Value_In
        FROM   Inventory i JOIN Products p ON p.ID = i.Product_ID
        WHERE  i.Status = 'IN_STOCK'
        GROUP BY p.Group_ID
) inv ON inv.Group_ID = pg.ID
LEFT JOIN (
        SELECT p.Group_ID, SUM(d.Line_Total) AS Total_Price_In
        FROM   Import_Receipt_Details d
        JOIN   Import_Receipts r ON r.ID = d.Import_Receipt_ID AND r.Status = 'CONFIRMED'
        JOIN   Products p ON p.ID = d.Product_ID
        GROUP BY p.Group_ID
) imp ON imp.Group_ID = pg.ID
LEFT JOIN (
        SELECT p.Group_ID, SUM(d.Line_Total) AS Total_Price_Out
        FROM   Export_Receipt_Details d
        JOIN   Export_Receipts r ON r.ID = d.Export_Receipt_ID AND r.Status = 'EXPORTED'
        JOIN   Products p ON p.ID = d.Product_ID
        GROUP BY p.Group_ID
) exp ON exp.Group_ID = pg.ID
LEFT JOIN (
        SELECT p.Group_ID, SUM(d.Rental_Price * d.Quantity) AS Total_Rent
        FROM   Rental_Receipt_Details d
        JOIN   Products p ON p.ID = d.Product_ID
        GROUP BY p.Group_ID
) ren ON ren.Group_ID = pg.ID
WHERE   pg.Is_Deleted = FALSE;

-- View_Stock_By_Warehouse [v6] (§3.8 LOW_STOCK theo kho)
CREATE OR REPLACE VIEW View_Stock_By_Warehouse AS
SELECT  ss.Warehouse_ID,
        w.Warehouse_Code,
        w.Warehouse_Name,
        ss.Product_ID,
        p.Product_Name,
        p.Unit,
        ss.Qty_On_Hand,
        ss.Qty_Reserved,
        ss.Qty_Rented,
        ss.Qty_Damaged,
        ss.Qty_In_Transit,
        COALESCE(ss.Min_Stock_Level, p.Min_Stock_Level) AS Effective_Min_Stock,
        (ss.Qty_On_Hand < COALESCE(ss.Min_Stock_Level, p.Min_Stock_Level)) AS Is_Low,
        ss.Last_Updated
FROM    Stock_Summary ss
JOIN    Products   p ON p.ID = ss.Product_ID
JOIN    Warehouses w ON w.ID = ss.Warehouse_ID
WHERE   p.Is_Deleted = FALSE;

-- View_Stock_Company_Total [v6] (§7.1.2 dashboard)
CREATE OR REPLACE VIEW View_Stock_Company_Total AS
SELECT  p.ID   AS Product_ID,
        p.Product_Name,
        p.Unit,
        COALESCE(SUM(ss.Qty_On_Hand), 0)    AS Qty_On_Hand,
        COALESCE(SUM(ss.Qty_Reserved), 0)   AS Qty_Reserved,
        COALESCE(SUM(ss.Qty_Rented), 0)     AS Qty_Rented,
        COALESCE(SUM(ss.Qty_Damaged), 0)    AS Qty_Damaged,
        COALESCE(SUM(ss.Qty_In_Transit), 0) AS Qty_In_Transit
FROM    Products p
LEFT JOIN Stock_Summary ss ON ss.Product_ID = p.ID
WHERE   p.Is_Deleted = FALSE
GROUP BY p.ID, p.Product_Name, p.Unit;

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
