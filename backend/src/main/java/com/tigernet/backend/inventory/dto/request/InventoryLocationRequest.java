package com.tigernet.backend.inventory.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// xếp hàng lên kệ/ô; locationId null = gỡ khỏi vị trí
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryLocationRequest {
    private Integer locationId;
}
