package com.tigernet.backend.stocktake.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StocktakeDetailResponse {
    private Integer id;
    private Integer productId;
    private String productName;
    private String unit;
    private Integer systemQty;
    private Integer actualQty;
    private Integer difference;
}
