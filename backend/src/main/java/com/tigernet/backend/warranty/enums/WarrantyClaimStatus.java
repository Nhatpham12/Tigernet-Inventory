package com.tigernet.backend.warranty.enums;

public enum WarrantyClaimStatus {
    RECEIVED, // đã nhận máy từ khách
    SENT,     // đã gửi đi sửa/đổi
    RETURNED, // máy đã về kho
    CLOSED,   // đã trả khách
    REJECTED  // từ chối bảo hành
}
