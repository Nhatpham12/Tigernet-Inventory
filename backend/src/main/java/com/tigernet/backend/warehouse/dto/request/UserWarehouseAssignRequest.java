package com.tigernet.backend.warehouse.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// thay toàn bộ danh sách kho của user bằng danh sách này
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserWarehouseAssignRequest {
    @NotNull(message = "Warehouses list is required")
    @Valid
    private List<UserWarehouseItem> warehouses;
}
