package com.tigernet.backend.inbound.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImportReceiptDetailResponse {
    private Integer id;
    private Integer productId;
    private String productName;
    private String unit;
    private Integer quantity;
    private BigDecimal priceIn;
    private String serial;
    private String trackingCode;
    private BigDecimal lineTotal;
}
