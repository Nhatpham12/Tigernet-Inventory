package com.tigernet.backend.stocktake.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

// chốt phiếu kiểm kê; reasons: productId -> lý do chênh lệch của dòng đó
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StocktakeCompleteRequest {
    private Map<Integer, String> reasons;
}
