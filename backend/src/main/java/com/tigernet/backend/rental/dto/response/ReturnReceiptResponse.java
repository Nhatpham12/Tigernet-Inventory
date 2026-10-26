package com.tigernet.backend.rental.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReturnReceiptResponse {
    private Integer id;
    private String receiptCode;
    private Integer rentalReceiptId;
    private String rentalReceiptCode;
    private Integer warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private LocalDateTime returnDate;
    private Integer daysOverdue;
    private BigDecimal penaltyAmount;
    private BigDecimal compensationAmount;
    private BigDecimal depositRefund;
    private String note;
    private Integer createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private List<ReturnReceiptDetailResponse> details;
}
