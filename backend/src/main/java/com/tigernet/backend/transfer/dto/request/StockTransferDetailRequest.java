package com.tigernet.backend.transfer.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockTransferDetailRequest {
    @NotNull(message = "Product ID is required")
    private Integer productId;

    private Integer inventoryId; // đơn vị/lô tồn ở kho nguồn

    @NotNull(message = "Quantity sent is required")
    @Positive(message = "Quantity sent must be > 0")
    private Integer qtySent;

    private String serial;
    private String note;
}
