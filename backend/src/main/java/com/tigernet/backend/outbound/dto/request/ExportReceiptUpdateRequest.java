package com.tigernet.backend.outbound.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// không đổi kho vì mã kho nằm trong số phiếu; details != null thì thay toàn bộ dòng
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExportReceiptUpdateRequest {
    private Integer customerId;
    private Integer receiverId;
    private String receiverName;
    private String receiverAddress;
    private String receiverPhone;
    private LocalDateTime dateOut;
    private LocalDateTime deliveryDate;
    private Integer vatId;

    @PositiveOrZero(message = "Deposit must be >= 0")
    private BigDecimal deposit;

    private String note;

    @Size(min = 1, message = "Receipt must have at least one line")
    @Valid
    private List<ExportReceiptDetailRequest> details;
}
