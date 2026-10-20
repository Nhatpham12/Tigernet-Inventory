package com.tigernet.backend.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VatOptionRequest {
    @NotBlank(message = "VAT label is required")
    @Size(max = 10, message = "VAT label must be at most 10 characters")
    private String vat;

    @PositiveOrZero(message = "Rate must be >= 0")
    private BigDecimal rate; // null = không chịu VAT
}
