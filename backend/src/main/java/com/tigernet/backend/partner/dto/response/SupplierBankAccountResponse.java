package com.tigernet.backend.partner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SupplierBankAccountResponse {
    private Integer id;
    private Integer supplierId;
    private String accountName;
    private String accountNumber;
    private String bankName;
    private String bankPaymentCode;
}
