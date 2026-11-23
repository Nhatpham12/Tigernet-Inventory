package com.tigernet.backend.common.security;

/** Page key của các màn FE (frontend/lib/core/router/menu.dart). Giữ hai bên khớp nhau. */
public final class Pages {
    private Pages() {
    }

    public static final String DASHBOARD = "/app/dashboard";
    public static final String PRODUCTS = "/app/products";
    public static final String PRODUCT_GROUPS = "/app/product-groups";
    public static final String PRODUCTS_NEW = "/app/products/new";
    public static final String IMPORTS = "/app/import-receipts";
    public static final String IMPORTS_NEW = "/app/import-receipts/new";
    public static final String SUPPLIERS = "/app/suppliers";
    public static final String SUPPLIERS_NEW = "/app/suppliers/new";
    public static final String EXPORTS = "/app/export-receipts";
    public static final String EXPORTS_NEW = "/app/export-receipts/new";
    public static final String CUSTOMERS = "/app/customers";
    public static final String SALES_RETURNS = "/app/sales-returns";
    public static final String TRANSFERS = "/app/transfers";
    public static final String RENTALS = "/app/rentals";
    public static final String RENTALS_NEW = "/app/rentals/new";
    public static final String RETURN_RECEIPTS = "/app/return-receipts";
    public static final String LENDS = "/app/lends";
    public static final String LENDS_NEW = "/app/lends/new";
    public static final String WARRANTY = "/app/warranty-claims";
    public static final String WARRANTY_NEW = "/app/warranty-claims/new";
    public static final String WAREHOUSES = "/app/warehouses";
    public static final String STOCKTAKES = "/app/stocktakes";
    public static final String PAYMENTS = "/app/payments";
    public static final String PAYMENTS_NEW_IN = "/app/payments/new-in";
    public static final String PAYMENTS_NEW_OUT = "/app/payments/new-out";
    public static final String REPORT_STOCK = "/app/reports/stock";
    public static final String REPORT_MOVEMENTS = "/app/reports/movements";
    public static final String REPORT_RECEIVABLES = "/app/reports/receivables";
    public static final String REPORT_RENTALS = "/app/reports/rentals";
    public static final String REPORT_RECONCILIATION = "/app/reports/reconciliation";
    // menu Quản trị: không phân quyền riêng được, luôn theo vai trò
    public static final String USERS = "/app/users";
    public static final String PERMISSIONS = "/app/permissions";
    public static final String AUDIT_LOGS = "/app/audit-logs";
    public static final String SETTINGS = "/app/settings";
}
