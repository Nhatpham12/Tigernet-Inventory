package com.tigernet.backend.stocktake.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// nhập lại số liệu kiểm kê: thay toàn bộ dòng
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StocktakeItemsRequest {
    private LocalDateTime stocktakeDate;

    @NotEmpty(message = "Stocktake must have at least one line")
    @Valid
    private List<StocktakeDetailRequest> details;
}
