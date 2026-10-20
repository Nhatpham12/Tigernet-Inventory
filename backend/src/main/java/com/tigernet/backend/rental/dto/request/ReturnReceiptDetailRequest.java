package com.tigernet.backend.rental.dto.request;

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
public class ReturnReceiptDetailRequest {
    @NotNull(message = "Rental detail ID is required")
    private Integer rentalDetailId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be > 0")
    private Integer quantity;

    private Integer inventoryId;    // null = lấy theo dòng thuê gốc
    private String serial;          // null = lấy theo dòng thuê gốc
    private String conditionNote;   // nguyên vẹn / hư / mất...
}
