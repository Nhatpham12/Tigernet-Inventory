package com.tigernet.backend.salesreturn.dto.request;

import com.tigernet.backend.salesreturn.enums.FeeActionType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReturnFeePolicyRequest {
    @NotBlank(message = "Tier name is required")
    @Size(max = 30, message = "Tier name must be at most 30 characters")
    private String tierName;

    @NotNull(message = "Max days is required")
    @Positive(message = "Max days must be > 0")
    private Integer maxDays;

    @NotNull(message = "Action type is required")
    private FeeActionType actionType;

    @NotNull(message = "Fee percent is required")
    @DecimalMin(value = "0", message = "Fee percent must be between 0 and 100")
    @DecimalMax(value = "100", message = "Fee percent must be between 0 and 100")
    private BigDecimal feePercent;

    private Boolean isAllowed;
    private LocalDate effectiveFrom; // null = áp dụng từ trước tới nay
}
