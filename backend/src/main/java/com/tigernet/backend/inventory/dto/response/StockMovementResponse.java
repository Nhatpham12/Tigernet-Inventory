package com.tigernet.backend.inventory.dto.response;

import com.tigernet.backend.inventory.enums.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockMovementResponse {
    private Long id;
    private Integer productId;
    private String productName;
    private Integer warehouseId;
    private String warehouseCode;
    private MovementType movementType;
    private Integer quantity;
    private Integer balanceAfter;
    private String referenceType;
    private Integer referenceId;
    private LocalDateTime createdAt;
}
