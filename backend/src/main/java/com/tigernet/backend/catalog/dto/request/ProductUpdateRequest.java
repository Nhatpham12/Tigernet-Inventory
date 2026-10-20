package com.tigernet.backend.catalog.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductUpdateRequest {
    @Size(max = 255, message = "Product name must be at most 255 characters")
    private String productName;

    private Integer groupId;

    @Size(max = 30, message = "Unit must be at most 30 characters")
    private String unit;

    @PositiveOrZero(message = "Default price in must be >= 0")
    private BigDecimal defaultPriceIn;

    @PositiveOrZero(message = "Default price out must be >= 0")
    private BigDecimal defaultPriceOut;

    private Boolean isSerialized;

    @PositiveOrZero(message = "Min stock level must be >= 0")
    private Integer minStockLevel;

    private String imagePath;
}
