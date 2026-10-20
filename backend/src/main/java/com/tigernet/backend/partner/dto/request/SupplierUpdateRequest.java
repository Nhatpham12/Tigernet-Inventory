package com.tigernet.backend.partner.dto.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SupplierUpdateRequest {
    @Valid
    private PartyUpdateRequest party;

    private String contractNo;
    private String trackingCode;
}
