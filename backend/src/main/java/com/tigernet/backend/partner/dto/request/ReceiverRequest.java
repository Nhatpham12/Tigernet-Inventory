package com.tigernet.backend.partner.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReceiverRequest {
    @NotBlank(message = "Receiver name is required")
    @Size(max = 150, message = "Receiver name must be at most 150 characters")
    private String receiverName;

    private String address;
    private String phone;
}
