package com.tigernet.backend.transfer.dto.response;

import com.tigernet.backend.transfer.enums.TransferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockTransferResponse {
    private Integer id;
    private String transferCode;
    private Integer fromWarehouseId;
    private String fromWarehouseCode;
    private Integer toWarehouseId;
    private String toWarehouseCode;
    private TransferStatus status;
    private String note;
    private Integer createdById;
    private String createdByName;
    private Integer approvedById;
    private String approvedByName;
    private Integer receivedById;
    private String receivedByName;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime receivedAt;
    private List<StockTransferDetailResponse> details;
}
