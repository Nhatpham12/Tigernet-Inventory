package com.tigernet.backend.alert.dto.response;

import com.tigernet.backend.alert.enums.AlertType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AlertResponse {
    private Integer id;
    private AlertType alertType;
    private Integer warehouseId;
    private String warehouseCode;
    private String message;
    private String refTable;
    private Integer refId;
    private Boolean isResolved;
    private LocalDateTime createdAt;
}
