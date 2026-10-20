package com.tigernet.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 1xxx: user
    USER_NOT_FOUND(1001, "User not found", HttpStatus.NOT_FOUND),
    USERNAME_EXISTED(1002, "Username already exists", HttpStatus.CONFLICT),
    UNAUTHENTICATED(1003, "Invalid username or password", HttpStatus.UNAUTHORIZED), // dùng chung cho sai username và sai mật khẩu
    USER_INACTIVE(1004, "User account is inactive", HttpStatus.FORBIDDEN),
    AUTHENTICATION_REQUIRED(1005, "Missing, invalid or expired access token", HttpStatus.UNAUTHORIZED),

    // 11xx: role
    ROLE_NOT_FOUND(1101, "Role not found", HttpStatus.NOT_FOUND),

    // 12xx: kho
    WAREHOUSE_NOT_FOUND(1201, "Warehouse not found", HttpStatus.NOT_FOUND),
    WAREHOUSE_CODE_EXISTED(1202, "Warehouse code already exists", HttpStatus.CONFLICT),
    WAREHOUSE_NAME_EXISTED(1203, "Warehouse name already exists", HttpStatus.CONFLICT),
    WAREHOUSE_INACTIVE(1204, "Warehouse is inactive", HttpStatus.BAD_REQUEST),
    WAREHOUSE_IN_USE(1205, "Warehouse is referenced by other records", HttpStatus.CONFLICT),
    LOCATION_NOT_FOUND(1206, "Warehouse location not found", HttpStatus.NOT_FOUND),
    LOCATION_CODE_EXISTED(1207, "Location code already exists in this warehouse", HttpStatus.CONFLICT),
    MULTIPLE_DEFAULT_WAREHOUSES(1208, "A user can have only one default warehouse", HttpStatus.BAD_REQUEST),
    LOCATION_NOT_IN_WAREHOUSE(1209, "Location does not belong to the warehouse", HttpStatus.BAD_REQUEST),

    // 13xx: sản phẩm & danh mục
    PRODUCT_GROUP_NOT_FOUND(1301, "Product group not found", HttpStatus.NOT_FOUND),
    PRODUCT_GROUP_EXISTED(1302, "Product group name already exists", HttpStatus.CONFLICT),
    PRODUCT_GROUP_IN_USE(1303, "Product group still has products", HttpStatus.CONFLICT),
    PRODUCT_NOT_FOUND(1304, "Product not found", HttpStatus.NOT_FOUND),
    PRODUCT_EXISTED(1305, "Product name already exists in this group", HttpStatus.CONFLICT),
    STOCK_SETTING_INVALID(1306, "Max stock must be greater than or equal to min stock", HttpStatus.BAD_REQUEST),
    VAT_OPTION_NOT_FOUND(1307, "VAT option not found", HttpStatus.NOT_FOUND),
    VAT_OPTION_EXISTED(1308, "VAT option already exists", HttpStatus.CONFLICT),
    VAT_RATE_IN_USE(1309, "VAT rate is used by export receipts, add a new VAT option instead", HttpStatus.CONFLICT),
    PAYMENT_STATUS_NOT_FOUND(1310, "Payment status not found", HttpStatus.NOT_FOUND),

    // 14xx: đối tác
    PARTY_NOT_FOUND(1401, "Party not found", HttpStatus.NOT_FOUND),
    TAX_CODE_EXISTED(1402, "Tax code already exists", HttpStatus.CONFLICT),
    PARTY_REQUIRED(1403, "Either partyId or party information is required", HttpStatus.BAD_REQUEST),
    SUPPLIER_NOT_FOUND(1404, "Supplier not found", HttpStatus.NOT_FOUND),
    SUPPLIER_EXISTED(1405, "This party is already a supplier", HttpStatus.CONFLICT),
    BANK_ACCOUNT_NOT_FOUND(1406, "Supplier bank account not found", HttpStatus.NOT_FOUND),
    SUPPLIER_DOCUMENT_NOT_FOUND(1407, "Supplier document not found", HttpStatus.NOT_FOUND),
    CUSTOMER_NOT_FOUND(1408, "Customer not found", HttpStatus.NOT_FOUND),
    CUSTOMER_EXISTED(1409, "This party is already a customer", HttpStatus.CONFLICT),
    RECEIVER_NOT_FOUND(1410, "Receiver not found", HttpStatus.NOT_FOUND),
    RECEIVER_NOT_OF_CUSTOMER(1411, "Receiver does not belong to the customer", HttpStatus.BAD_REQUEST),

    // 15xx: tồn kho
    INVENTORY_NOT_FOUND(1501, "Inventory item not found", HttpStatus.NOT_FOUND),
    INVENTORY_PRODUCT_MISMATCH(1502, "Inventory item does not match the product of the line", HttpStatus.BAD_REQUEST),
    INVENTORY_NOT_IN_WAREHOUSE(1503, "Inventory item is not in the receipt's warehouse", HttpStatus.BAD_REQUEST),
    SERIAL_EXISTED(1504, "Serial already exists in inventory", HttpStatus.CONFLICT),

    // 2xxx: chứng từ
    RECEIPT_NOT_EDITABLE(2001, "Receipt can no longer be changed in its current status", HttpStatus.CONFLICT),
    IMPORT_RECEIPT_NOT_FOUND(2002, "Import receipt not found", HttpStatus.NOT_FOUND),
    EXPORT_RECEIPT_NOT_FOUND(2003, "Export receipt not found", HttpStatus.NOT_FOUND),
    PAID_EXCEEDS_TOTAL(2004, "Paid amount cannot exceed total amount", HttpStatus.BAD_REQUEST),
    RENTAL_RECEIPT_NOT_FOUND(2005, "Rental receipt not found", HttpStatus.NOT_FOUND),
    INVALID_DUE_DATE(2006, "Due date must not be before date out", HttpStatus.BAD_REQUEST),
    RETURN_RECEIPT_NOT_FOUND(2007, "Return receipt not found", HttpStatus.NOT_FOUND),
    RENTAL_DETAIL_INVALID(2008, "Rental line does not belong to the rental receipt or is already returned", HttpStatus.BAD_REQUEST),
    SALES_RETURN_NOT_FOUND(2009, "Sales return not found", HttpStatus.NOT_FOUND),
    EXPORT_DETAIL_INVALID(2010, "Export line does not belong to the export receipt", HttpStatus.BAD_REQUEST),
    RETURN_QUANTITY_EXCEEDED(2011, "Returned quantity exceeds the quantity on the original line", HttpStatus.BAD_REQUEST),
    EXCHANGE_DIRECTION_REQUIRED(2012, "Exchange direction is required for EXCHANGE returns", HttpStatus.BAD_REQUEST),
    DELIVERY_DATE_MISSING(2013, "Original export receipt has no delivery date", HttpStatus.BAD_REQUEST),
    RETURN_FEE_POLICY_NOT_FOUND(2014, "Return fee policy not found", HttpStatus.NOT_FOUND),
    STOCKTAKE_NOT_FOUND(2015, "Stocktake not found", HttpStatus.NOT_FOUND),
    DUPLICATE_PRODUCT_LINE(2016, "A product appears more than once", HttpStatus.BAD_REQUEST),
    STOCK_ADJUSTMENT_NOT_FOUND(2017, "Stock adjustment not found", HttpStatus.NOT_FOUND),
    STOCK_TRANSFER_NOT_FOUND(2018, "Stock transfer not found", HttpStatus.NOT_FOUND),
    SAME_WAREHOUSE_TRANSFER(2019, "Source and destination warehouses must be different", HttpStatus.BAD_REQUEST),
    SERIAL_QUANTITY_INVALID(2020, "A line with a serial must have quantity 1", HttpStatus.BAD_REQUEST),
    DUPLICATE_SERIAL(2021, "A serial appears more than once", HttpStatus.BAD_REQUEST),
    STATUS_NOT_ALLOWED(2022, "This status cannot be set here", HttpStatus.BAD_REQUEST),
    NEW_EXPORT_INVALID(2023, "Exchange export receipt must differ from the original export receipt", HttpStatus.BAD_REQUEST),

    // 3xxx: hệ thống
    ALERT_NOT_FOUND(3001, "Alert not found", HttpStatus.NOT_FOUND),

    // 9xxx: lỗi chung
    INVALID_REQUEST(9001, "Invalid request data", HttpStatus.BAD_REQUEST),
    MALFORMED_JSON(9002, "Malformed JSON request", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER(9003, "Invalid parameter", HttpStatus.BAD_REQUEST),
    DATA_CONFLICT(9004, "Data conflicts with existing records", HttpStatus.CONFLICT),
    TOKEN_GENERATION_FAILED(9005, "Could not generate access token", HttpStatus.INTERNAL_SERVER_ERROR),
    CONCURRENT_UPDATE(9006, "Record was changed by another transaction, please retry", HttpStatus.CONFLICT),
    UNCATEGORIZED(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
