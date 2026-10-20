package com.tigernet.backend.rental.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReturnReceiptDetailResponse {
    private Integer id;
    private Integer rentalDetailId;
    private Integer productId;
    private String productName;
    private Integer inventoryId;
    private Integer quantity;
    private String serial;
    private String conditionNote;
}
