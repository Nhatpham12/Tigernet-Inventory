package com.tigernet.backend.rental.dto.request;

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
public class RentalReceiptDetailRequest {
    @NotNull(message = "Product ID is required")
    private Integer productId;

    private Integer inventoryId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be > 0")
    private Integer quantity;

    @NotNull(message = "Rental price is required")
    @PositiveOrZero(message = "Rental price must be >= 0")
    private BigDecimal rentalPrice; // giá thuê cả kỳ của 1 đơn vị (cho mượn thì 0)

    private String serial;
}
