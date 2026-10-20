package com.tigernet.backend.stocktake.dto.response;

import com.tigernet.backend.stocktake.enums.StocktakeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StocktakeResponse {
    private Integer id;
    private String stocktakeCode;
    private Integer warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private LocalDateTime stocktakeDate;
    private StocktakeStatus status;
    private Integer createdById;
    private String createdByName;
    private List<StocktakeDetailResponse> details;
}
