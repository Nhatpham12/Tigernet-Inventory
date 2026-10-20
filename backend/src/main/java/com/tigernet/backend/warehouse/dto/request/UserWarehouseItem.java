package com.tigernet.backend.warehouse.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserWarehouseItem {
    @NotNull(message = "Warehouse ID is required")
    private Integer warehouseId;

    private Boolean isDefault;
}
