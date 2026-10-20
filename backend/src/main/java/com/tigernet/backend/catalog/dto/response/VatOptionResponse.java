package com.tigernet.backend.catalog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VatOptionResponse {
    private Integer id;
    private String vat;
    private BigDecimal rate;
}
