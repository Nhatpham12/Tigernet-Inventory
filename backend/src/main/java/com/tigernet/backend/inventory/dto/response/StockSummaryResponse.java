package com.tigernet.backend.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockSummaryResponse {
    private Integer productId;
    private String productName;
    private Integer warehouseId;
    private String warehouseCode;
    private Integer qtyOnHand;
    private Integer qtyReserved;
    private Integer qtyRented;
    private Integer qtyDamaged;
    private Integer qtyInTransit;
    private LocalDateTime lastUpdated;
}
