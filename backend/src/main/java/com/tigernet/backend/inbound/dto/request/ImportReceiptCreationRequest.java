package com.tigernet.backend.inbound.dto.request;

import com.tigernet.backend.inbound.enums.ImportStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImportReceiptCreationRequest {
    @NotNull(message = "Source party ID is required")
    private Integer sourcePartyId;

    @NotNull(message = "Warehouse ID is required")
    private Integer warehouseId;

    private LocalDateTime dateIn; // null = thời điểm hiện tại
    private Integer vatId;        // VAT đầu vào, null = không VAT
    private ImportStatus status;  // DRAFT (mặc định) hoặc PENDING = "Lưu & gửi duyệt"
    private String note;

    @NotEmpty(message = "Receipt must have at least one line")
    @Valid
    private List<ImportReceiptDetailRequest> details;
}
