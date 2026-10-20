package com.tigernet.backend.partner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReceiverResponse {
    private Integer id;
    private Integer customerId;
    private String receiverName;
    private String address;
    private String phone;
}
