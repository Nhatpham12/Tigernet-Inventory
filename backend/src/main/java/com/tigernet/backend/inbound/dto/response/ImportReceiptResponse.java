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
    private BigDecimal totalAmount;
    private ImportStatus status;
    private String note;
    private Integer createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private List<ImportReceiptDetailResponse> details;
}
