package com.tigernet.backend.outbound.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExportReceiptDetailResponse {
    private Integer id;
    private Integer productId;
    private String productName;
    private String unit;
    private Integer inventoryId;
    private Integer quantity;
    private BigDecimal priceOut;
    private String serial;
    private Integer warrantyMonths;
    private LocalDate warrantyExpiry;
    private BigDecimal lineTotal;
}
