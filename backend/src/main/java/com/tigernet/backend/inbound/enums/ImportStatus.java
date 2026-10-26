package com.tigernet.backend.inbound.enums;

public enum ImportStatus {
    DRAFT,
    PENDING,   // đã gửi duyệt
    CONFIRMED, // đã nhập kho
    CANCELLED  // đã hủy, giữ lại để tra cứu (Cancelled_By/At/Reason)
}
