package com.tigernet.backend.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserWarehouseResponse {
    private Integer userId;
    private Integer warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private Boolean isDefault;
    private LocalDateTime assignedAt;
}
