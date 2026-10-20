package com.tigernet.backend.inventory.dto.response;

import com.tigernet.backend.inventory.enums.InventoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryResponse {
    private Integer id;
    private Integer productId;
    private String productName;
    private Integer warehouseId;
    private String warehouseCode;
    private Integer locationId;
    private String locationCode;
    private String serial;
    private Integer importDetailId;
    private Integer quantityOnHand;
    private InventoryStatus status;
    private BigDecimal priceIn;
    private String note;
    private LocalDateTime dateIn;
    private LocalDateTime dateOut;
}
