package com.tigernet.backend.transfer.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// không đổi kho nguồn vì mã kho nguồn nằm trong số phiếu; details != null thì thay toàn bộ dòng
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockTransferUpdateRequest {
    private Integer toWarehouseId;
    private String note;

    @Size(min = 1, message = "Transfer must have at least one line")
    @Valid
    private List<StockTransferDetailRequest> details;
}
