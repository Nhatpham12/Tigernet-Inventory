package com.tigernet.backend.stocktake.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockAdjustmentResponse {
    private Integer id;
    private Integer stocktakeId;
    private String stocktakeCode;
    private Integer warehouseId; // kho điều chỉnh = kho của phiếu kiểm kê
    private Integer productId;
    private String productName;
    private Integer adjustQty;
    private String reason;
    private LocalDateTime createdAt;
}
