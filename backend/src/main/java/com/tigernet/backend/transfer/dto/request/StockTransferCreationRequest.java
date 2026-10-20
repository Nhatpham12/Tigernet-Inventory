package com.tigernet.backend.transfer.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockTransferCreationRequest {
    @NotNull(message = "Source warehouse ID is required")
    private Integer fromWarehouseId;

    @NotNull(message = "Destination warehouse ID is required")
    private Integer toWarehouseId;

    private String note;

    @NotEmpty(message = "Transfer must have at least one line")
    @Valid
    private List<StockTransferDetailRequest> details;
}
