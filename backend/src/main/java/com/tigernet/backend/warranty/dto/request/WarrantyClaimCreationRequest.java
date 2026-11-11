package com.tigernet.backend.warranty.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// tiếp nhận một đơn vị hàng bảo hành từ dòng bán gốc
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WarrantyClaimCreationRequest {
    @NotNull(message = "Export detail is required")
    private Integer exportDetailId;

    private Integer inventoryId; // null = tự tra theo serial của dòng bán

    @NotNull(message = "Receiving warehouse is required")
    private Integer receivedWarehouseId;

    private LocalDateTime receivedAt; // null = bây giờ
    private String note;
}
