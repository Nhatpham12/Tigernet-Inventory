package com.tigernet.backend.partner.dto.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// gửi partyId (Party đã có, VD đang là khách hàng) HOẶC party (tạo Party mới)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SupplierCreationRequest {
    private Integer partyId;

    @Valid
    private PartyRequest party;

    private String contractNo;
    private String trackingCode;
}
