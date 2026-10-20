package com.tigernet.backend.salesreturn.dto.request;

import com.tigernet.backend.salesreturn.enums.ReturnCondition;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesReturnDetailRequest {
    @NotNull(message = "Export detail ID is required")
    private Integer exportDetailId; // dòng bán gốc

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be > 0")
    private Integer quantity;

    private Integer inventoryId; // null = lấy theo dòng bán gốc
    private String serial;       // null = lấy theo dòng bán gốc

    @NotNull(message = "Return condition is required")
    private ReturnCondition returnCondition;
}
