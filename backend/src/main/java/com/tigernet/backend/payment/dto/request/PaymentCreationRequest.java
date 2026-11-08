package com.tigernet.backend.payment.dto.request;

import com.tigernet.backend.payment.enums.PaymentDirection;
import com.tigernet.backend.payment.enums.PaymentMethod;
import com.tigernet.backend.payment.enums.PaymentReferenceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// lập phiếu thu/chi; Payments.Reference_* là NOT NULL nên service bắt buộc có phiếu gốc
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCreationRequest {
    @NotNull(message = "Direction is required")
    private PaymentDirection direction;

    @NotNull(message = "Party is required")
    private Integer partyId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be > 0")
    private BigDecimal amount;

    private PaymentMethod method; // null = tiền mặt
    private LocalDateTime paidAt; // null = bây giờ
    private String note;
    private PaymentReferenceType referenceType;
    private Integer referenceId;
}
