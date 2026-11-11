package com.tigernet.backend.warranty.dto.request;

import com.tigernet.backend.warranty.enums.WarrantyClaimStatus;
import com.tigernet.backend.warranty.enums.WarrantyResult;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// đổi trạng thái: SENT | RETURNED | CLOSED | REJECTED; các trường còn lại chỉ ghi khi có gửi lên
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WarrantyStatusRequest {
    @NotNull(message = "Status is required")
    private WarrantyClaimStatus status;

    private WarrantyResult result;

    @PositiveOrZero(message = "Cost must be >= 0")
    private BigDecimal cost;

    private String replacementSerial;
    private LocalDate newWarrantyExpiry;
    private Integer restockedWarehouseId;
    private String note;
}
