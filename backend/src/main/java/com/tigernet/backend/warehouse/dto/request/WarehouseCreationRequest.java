package com.tigernet.backend.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseCreationRequest {
    // in vào số phiếu nên chỉ cho chữ hoa, số, gạch ngang
    @NotBlank(message = "Warehouse code is required")
    @Size(max = 20, message = "Warehouse code must be at most 20 characters")
    @Pattern(regexp = "[A-Z0-9_]+", message = "Warehouse code only allows A-Z, 0-9 and _")
    private String warehouseCode;

    @NotBlank(message = "Warehouse name is required")
    @Size(max = 150, message = "Warehouse name must be at most 150 characters")
    private String warehouseName;

    @NotBlank(message = "Address is required")
    private String address;

    private String phone;
    private Integer managerId;
    private Boolean isActive;
}
