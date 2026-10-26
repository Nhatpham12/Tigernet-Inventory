package com.tigernet.backend.supplierreturn.enums;

public enum SupplierReturnStatus {
    DRAFT,
    SENT,          // đã gửi NCC
    RECEIVED_BACK, // NCC đã trả hàng/đổi/hoàn tiền
    CLOSED,
    CANCELLED
}
