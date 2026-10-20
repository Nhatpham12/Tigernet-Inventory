package com.tigernet.backend.partner.dto.response;

import com.tigernet.backend.partner.enums.PartyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PartyResponse {
    private Integer id;
    private PartyType partyType;
    private String fullName;
    private String taxCode;
    private String nationalId;
    private String address;
    private String phone;
    private String email;
}
