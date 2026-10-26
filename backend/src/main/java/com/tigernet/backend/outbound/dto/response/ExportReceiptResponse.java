package com.tigernet.backend.outbound.dto.response;

import com.tigernet.backend.outbound.enums.ExportStatus;
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
public class ExportReceiptResponse {
    private Integer id;
    private String receiptCode;
    private Integer customerId;
    private String customerName;
    private Integer warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private Integer receiverId;
    private String receiverNameSnapshot;
    private String receiverAddressSnapshot;
    private String receiverPhoneSnapshot;
    private LocalDateTime dateOut;
    private LocalDateTime deliveryDate;
    private Integer vatId;
    private String vat;
    private BigDecimal vatRate;
    private BigDecimal deposit;
    private BigDecimal totalAmount;
    private Integer paymentStatusId;
    private String paymentStatus;
    private BigDecimal paid;
    private BigDecimal remain;
    private ExportStatus status;
    private String note;
    private Integer createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private Integer cancelledById;
    private String cancelledByName;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private List<ExportReceiptDetailResponse> details;
}
