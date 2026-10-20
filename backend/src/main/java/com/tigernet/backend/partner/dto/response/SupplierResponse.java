package com.tigernet.backend.partner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SupplierResponse {
    private Integer id;
    private PartyResponse party;
    private String contractNo;
    private String trackingCode;
}
