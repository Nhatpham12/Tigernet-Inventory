package com.tigernet.backend.transfer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockTransferDetailResponse {
    private Integer id;
    private Integer productId;
    private String productName;
    private String unit;
    private Integer inventoryId;
    private Integer qtySent;
    private Integer qtyReceived;
    private String serial;
    private String note;
}
