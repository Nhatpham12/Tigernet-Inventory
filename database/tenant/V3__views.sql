-- =====================================================================
-- TIGERNET — Tenant database v6 — Flyway V3: VIEW báo cáo & dashboard
-- Nguồn: docs/Database_Design_v6.md (Phụ lục: Các VIEW)
-- =====================================================================

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
