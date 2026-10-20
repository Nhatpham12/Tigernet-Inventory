package com.tigernet.backend.partner.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// filePath là object key sau khi upload file lên object storage (module storage)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SupplierDocumentRequest {
    private String docType;

    @NotBlank(message = "File path is required")
    private String filePath;
}
