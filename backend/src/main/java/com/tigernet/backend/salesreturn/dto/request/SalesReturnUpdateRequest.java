package com.tigernet.backend.salesreturn.dto.request;

import com.tigernet.backend.salesreturn.enums.ExchangeDirection;
import com.tigernet.backend.salesreturn.enums.ReturnType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// gửi lại toàn bộ phiếu vì phí đổi/trả phải tính lại từ đầu; không đổi phiếu xuất gốc và kho
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesReturnUpdateRequest {
    private Integer partyId;

    @NotNull(message = "Return type is required")
    private ReturnType returnType;

    private ExchangeDirection exchangeDirection;
    private Integer newExportReceiptId;
    private LocalDateTime returnDate;
    private String note;

    @NotEmpty(message = "Sales return must have at least one line")
    @Valid
    private List<SalesReturnDetailRequest> details;
}
