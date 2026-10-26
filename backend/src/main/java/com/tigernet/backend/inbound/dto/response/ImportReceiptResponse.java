package com.tigernet.backend.inbound.dto.response;

import com.tigernet.backend.inbound.enums.ImportStatus;
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
public class ImportReceiptResponse {
    private Integer id;
    private String receiptCode;
    private Integer sourcePartyId;
    private String sourcePartyName;
    private Integer warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private LocalDateTime dateIn;
    private Integer vatId;
    private String vat;
    private BigDecimal vatRate;
    private BigDecimal totalAmount;
    private Integer paymentStatusId;
    private String paymentStatus;
    private BigDecimal paid;
    private BigDecimal remain;
    private ImportStatus status;
    private String note;
    private Integer createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private Integer cancelledById;
    private String cancelledByName;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private List<ImportReceiptDetailResponse> details;
}
