package com.tigernet.backend.outbound.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExportReceiptDetailRequest {
    @NotNull(message = "Product ID is required")
    private Integer productId;

    private Integer inventoryId; // đơn vị cụ thể (hàng serial); có thể chọn sau ở khâu xuất kho

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be > 0")
    private Integer quantity;

    @NotNull(message = "Price out is required")
    @PositiveOrZero(message = "Price out must be >= 0")
    private BigDecimal priceOut;

    private String serial;

    @PositiveOrZero(message = "Warranty months must be >= 0")
    private Integer warrantyMonths;
}
