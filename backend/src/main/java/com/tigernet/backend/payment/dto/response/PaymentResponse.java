package com.tigernet.backend.payment.dto.response;

import com.tigernet.backend.partner.enums.PartyType;
import com.tigernet.backend.payment.enums.PaymentDirection;
import com.tigernet.backend.payment.enums.PaymentEntryStatus;
import com.tigernet.backend.payment.enums.PaymentMethod;
import com.tigernet.backend.payment.enums.PaymentReferenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    private Integer id;
    private String paymentCode;
    private PaymentDirection direction;
    private Integer partyId;
    private String partyName;
    private PartyType partyType;
    private PaymentReferenceType referenceType;
    private Integer referenceId;
    private String referenceCode; // số phiếu gốc (nhập/xuất/thuê), null nếu không tra được
    private BigDecimal amount;
    private PaymentMethod method;
    private LocalDateTime paidAt;
    private PaymentEntryStatus status;
    private String note;
    private String recordedByName;
}
