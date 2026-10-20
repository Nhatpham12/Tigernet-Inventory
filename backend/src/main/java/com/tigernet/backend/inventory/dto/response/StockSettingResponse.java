package com.tigernet.backend.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockSettingResponse {
    private Integer id;
    private Integer productId;
    private String productName;
    private Integer warehouseId;
    private String warehouseCode;
    private Integer minStock;
    private Integer maxStock;
    private Integer reorderPoint;
}
