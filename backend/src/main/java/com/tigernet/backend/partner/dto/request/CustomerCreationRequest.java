package com.tigernet.backend.partner.dto.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// gửi partyId (Party đã có, VD đang là NCC) HOẶC party (tạo Party mới)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerCreationRequest {
    private Integer partyId;

    @Valid
    private PartyRequest party;
}
