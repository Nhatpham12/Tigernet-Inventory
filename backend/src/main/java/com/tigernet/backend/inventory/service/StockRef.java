package com.tigernet.backend.inventory.service;

// phiếu gây ra biến động tồn; type là tên bảng, khớp CHECK CK_SM_Ref_Type của Stock_Movements
public record StockRef(String type, Integer id) {
    public static final String IMPORT = "Import_Receipts";
    public static final String EXPORT = "Export_Receipts";
    public static final String RENTAL = "Rental_Receipts";
    public static final String RETURN = "Return_Receipts";
    public static final String SALES_RETURN = "Sales_Returns";
    public static final String TRANSFER = "Stock_Transfers";
    public static final String ADJUSTMENT = "Stock_Adjustments";
    public static final String SUPPLIER_RETURN = "Supplier_Returns";
    public static final String WARRANTY_CLAIM = "Warranty_Claims";

    public static StockRef of(String type, Integer id) {
        return new StockRef(type, id);
    }
}
