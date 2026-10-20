package com.tigernet.backend.rental.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RentalReceiptDetailResponse {
    private Integer id;
    private Integer productId;
    private String productName;
    private String unit;
    private Integer inventoryId;
    private Integer quantity;
    private BigDecimal rentalPrice;
    private String serial;
    private Boolean isReturned;
}
