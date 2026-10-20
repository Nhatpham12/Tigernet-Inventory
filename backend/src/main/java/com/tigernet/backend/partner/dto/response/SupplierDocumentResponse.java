package com.tigernet.backend.partner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SupplierDocumentResponse {
    private Integer id;
    private Integer supplierId;
    private String docType;
    private String filePath;
    private LocalDateTime uploadedAt;
}
