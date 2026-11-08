package com.tigernet.backend.warranty.dto.response;

import com.tigernet.backend.warranty.enums.WarrantyClaimStatus;
import com.tigernet.backend.warranty.enums.WarrantyResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WarrantyClaimResponse {
    private Integer id;
    private String claimCode;
    private Integer exportDetailId;
    private Integer exportReceiptId;
    private String exportReceiptCode;
    private Integer customerId;
    private String customerName;
    private Integer productId;
    private String productName;
    private String serial;
    private Integer inventoryId;
    private Integer receivedWarehouseId;
    private String receivedWarehouseName;
    private Integer restockedWarehouseId;
    private WarrantyClaimStatus status;
    private LocalDateTime receivedAt;
    private LocalDateTime sentAt;
    private LocalDateTime returnedAt;
    private WarrantyResult result;
    private BigDecimal cost;
    private String replacementSerial;
    private LocalDate newWarrantyExpiry;
    private String note;
    private String handledByName;
    private LocalDate warrantyExpiry;
}
