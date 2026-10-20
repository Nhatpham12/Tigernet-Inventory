package com.tigernet.backend.inbound.dto.request;

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
public class ImportReceiptDetailRequest {
    @NotNull(message = "Product ID is required")
    private Integer productId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be > 0")
    private Integer quantity;

    @NotNull(message = "Price in is required")
    @PositiveOrZero(message = "Price in must be >= 0")
    private BigDecimal priceIn;

    private String serial;       // hàng serial: mỗi dòng 1 serial, quantity = 1
    private String trackingCode; // mã vận đơn
}
