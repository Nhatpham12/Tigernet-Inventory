package com.tigernet.backend.salesreturn.dto.response;

import com.tigernet.backend.salesreturn.enums.ExchangeDirection;
import com.tigernet.backend.salesreturn.enums.ReturnType;
import com.tigernet.backend.salesreturn.enums.SalesReturnStatus;
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
public class SalesReturnResponse {
    private Integer id;
    private String receiptCode;
    private Integer exportReceiptId;
    private String exportReceiptCode;
    private Integer partyId;
    private String partyName;
    private Integer warehouseId;
    private String warehouseCode;
    private ReturnType returnType;
    private ExchangeDirection exchangeDirection;
    private LocalDateTime returnDate;
    private Integer daysSinceDelivery;
    private Integer policyId;
    private String policyTierName;
    private BigDecimal originalValue;
    private BigDecimal feePercent;
    private BigDecimal feeAmount;
    private Integer newExportReceiptId;
    private String newExportReceiptCode;
    private BigDecimal refundAmount; // âm = khách trả thêm phần chênh khi đổi sang mã đắt hơn
    private SalesReturnStatus status;
    private String note;
    private Integer createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private List<SalesReturnDetailResponse> details;
}
