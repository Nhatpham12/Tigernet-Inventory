package com.tigernet.backend.warehouse.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// không cho sửa warehouseCode vì đã in trên các số phiếu
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseUpdateRequest {
    @Size(max = 150, message = "Warehouse name must be at most 150 characters")
    private String warehouseName;
    private String address;
    private String phone;
    private Integer managerId;
    private Boolean isActive;
}
