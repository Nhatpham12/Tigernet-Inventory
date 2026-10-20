package com.tigernet.backend.catalog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private Integer id;
    private String productName;
    private Integer groupId;
    private String groupName;
    private String unit;
    private BigDecimal defaultPriceIn;
    private BigDecimal defaultPriceOut;
    private Boolean isSerialized;
    private Integer minStockLevel;
    private String imagePath;
}
