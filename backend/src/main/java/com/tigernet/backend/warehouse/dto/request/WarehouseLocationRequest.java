package com.tigernet.backend.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseLocationRequest {
    @NotBlank(message = "Location code is required")
    @Size(max = 30, message = "Location code must be at most 30 characters")
    private String locationCode;

    private String description;
}
