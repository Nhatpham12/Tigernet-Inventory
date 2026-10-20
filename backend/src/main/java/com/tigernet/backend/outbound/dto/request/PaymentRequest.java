package com.tigernet.backend.outbound.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// ghi nhận 1 lần thu tiền của phiếu xuất
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be > 0")
    private BigDecimal amount;

    private String note;
}
