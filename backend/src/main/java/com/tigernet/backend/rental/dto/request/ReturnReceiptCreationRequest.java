package com.tigernet.backend.rental.dto.request;

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
public class ReturnReceiptCreationRequest {
    @NotNull(message = "Rental receipt ID is required")
    private Integer rentalReceiptId;

    @NotNull(message = "Warehouse ID is required")
    private Integer warehouseId; // kho nhận lại, có thể khác kho đã cho thuê

    private LocalDateTime returnDate; // null = thời điểm hiện tại
    private String note;

    @NotEmpty(message = "Receipt must have at least one line")
    @Valid
    private List<ReturnReceiptDetailRequest> details;
}
