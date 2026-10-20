package com.tigernet.backend.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseLocationResponse {
    private Integer id;
    private Integer warehouseId;
    private String locationCode;
    private String description;
}
