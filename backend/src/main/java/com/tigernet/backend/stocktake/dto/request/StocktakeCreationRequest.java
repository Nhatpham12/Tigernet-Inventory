package com.tigernet.backend.stocktake.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class StocktakeCreationRequest {
    @NotNull(message = "Warehouse ID is required")
    private Integer warehouseId;

    private LocalDateTime stocktakeDate; // null = thời điểm hiện tại

    @NotEmpty(message = "Stocktake must have at least one line")
    @Valid
    private List<StocktakeDetailRequest> details;
}
