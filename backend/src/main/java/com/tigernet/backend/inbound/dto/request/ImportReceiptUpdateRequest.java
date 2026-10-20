package com.tigernet.backend.inbound.dto.request;

import com.tigernet.backend.inbound.enums.ImportStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// không đổi kho vì mã kho nằm trong số phiếu; details != null thì thay toàn bộ dòng
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImportReceiptUpdateRequest {
    private Integer sourcePartyId;
    private LocalDateTime dateIn;
    private ImportStatus status;
    private String note;

    @Size(min = 1, message = "Receipt must have at least one line")
    @Valid
    private List<ImportReceiptDetailRequest> details;
}
