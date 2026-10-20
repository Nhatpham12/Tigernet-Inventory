package com.tigernet.backend.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// tạo mới hoặc ghi đè ngưỡng tồn của (SP, kho)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockSettingRequest {
    @NotNull(message = "Product ID is required")
    private Integer productId;

    @NotNull(message = "Warehouse ID is required")
    private Integer warehouseId;

    @PositiveOrZero(message = "Min stock must be >= 0")
    private Integer minStock; // null = dùng Products.Min_Stock_Level

    @PositiveOrZero(message = "Max stock must be >= 0")
    private Integer maxStock;

    @PositiveOrZero(message = "Reorder point must be >= 0")
    private Integer reorderPoint;
}
