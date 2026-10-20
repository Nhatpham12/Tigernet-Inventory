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

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesReturnCreationRequest {
    @NotNull(message = "Export receipt ID is required")
    private Integer exportReceiptId;

    private Integer partyId; // null = khách hàng của phiếu xuất gốc

    @NotNull(message = "Warehouse ID is required")
    private Integer warehouseId; // kho tiếp nhận, có thể khác kho đã bán

    @NotNull(message = "Return type is required")
    private ReturnType returnType;

    private ExchangeDirection exchangeDirection; // bắt buộc khi EXCHANGE
    private Integer newExportReceiptId;          // phiếu xuất đổi hàng (nếu đã lập)
    private LocalDateTime returnDate;            // null = thời điểm hiện tại
    private String note;

    @NotEmpty(message = "Sales return must have at least one line")
    @Valid
    private List<SalesReturnDetailRequest> details;
}
