package com.tigernet.backend.warranty.enums;

public enum WarrantyClaimStatus {
    RECEIVED, // đã nhận máy từ khách
    SENT,     // đã gửi đi sửa/đổi
    RETURNED, // máy đã về kho
    CLOSED,   // đã trả khách
    REJECTED; // từ chối bảo hành

    // chuyển hợp lệ: RECEIVED -> SENT | REJECTED, SENT -> RETURNED, RETURNED -> CLOSED, REJECTED -> CLOSED
    public boolean canMoveTo(WarrantyClaimStatus next) {
        return switch (this) {
            case RECEIVED -> next == SENT || next == REJECTED;
            case SENT -> next == RETURNED;
            case RETURNED, REJECTED -> next == CLOSED;
            case CLOSED -> false;
        };
    }
}
