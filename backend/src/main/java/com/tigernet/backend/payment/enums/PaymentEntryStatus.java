package com.tigernet.backend.payment.enums;

public enum PaymentEntryStatus {
    COMPLETED,
    CANCELLED // hủy thay vì xóa, không tính vào Paid của phiếu
}
