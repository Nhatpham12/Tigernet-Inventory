package com.tigernet.backend.partner.dto.request;

import com.tigernet.backend.partner.enums.PartyType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PartyUpdateRequest {
    private PartyType partyType;

    @Size(max = 255, message = "Full name must be at most 255 characters")
    private String fullName;

    @Size(max = 30, message = "Tax code must be at most 30 characters")
    private String taxCode;

    @Size(max = 20, message = "National ID must be at most 20 characters")
    private String nationalId;

    private String address;
    private String phone;

    @Email(message = "Email is invalid")
    private String email;
}
