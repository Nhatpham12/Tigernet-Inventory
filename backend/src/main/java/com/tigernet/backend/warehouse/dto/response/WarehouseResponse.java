package com.tigernet.backend.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseResponse {
    private Integer id;
    private String warehouseCode;
    private String warehouseName;
    private String address;
    private String phone;
    private Integer managerId;
    private String managerName;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
