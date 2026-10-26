package com.tigernet.backend.outbound.enums;

public enum ExportStatus {
    PENDING,
    CONFIRMED,
    EXPORTED,
    CANCELLED // đã hủy, giữ lại để tra cứu (Cancelled_By/At/Reason)
}
