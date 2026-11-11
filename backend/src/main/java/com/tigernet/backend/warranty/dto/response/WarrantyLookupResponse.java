package com.tigernet.backend.warranty.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// một dòng bán có thể mang đi bảo hành
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WarrantyLookupResponse {
    private Integer exportDetailId;
    private Integer exportReceiptId;
    private String exportReceiptCode;
    private Integer productId;
    private String productName;
    private String serial;
    private Integer quantity;
    private Integer customerId;
    private String customerName;
    private LocalDateTime dateOut;
    private Integer warrantyMonths;
    private LocalDate warrantyExpiry;
    private boolean inWarranty;
    private Integer inventoryId;
}
