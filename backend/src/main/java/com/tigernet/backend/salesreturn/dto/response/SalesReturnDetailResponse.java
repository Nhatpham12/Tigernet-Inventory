package com.tigernet.backend.salesreturn.dto.response;

import com.tigernet.backend.inventory.enums.InventoryStatus;
import com.tigernet.backend.salesreturn.enums.ReturnCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesReturnDetailResponse {
    private Integer id;
    private Integer exportDetailId;
    private Integer productId;
    private String productName;
    private Integer inventoryId;
    private Integer quantity;
    private String serial;
    private ReturnCondition returnCondition;
    private InventoryStatus restockStatus;
}
