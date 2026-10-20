package com.tigernet.backend.stocktake.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StocktakeDetailRequest {
    @NotNull(message = "Product ID is required")
    private Integer productId;

    @NotNull(message = "Actual quantity is required")
    @PositiveOrZero(message = "Actual quantity must be >= 0")
    private Integer actualQty; // số đếm thực tế
}
