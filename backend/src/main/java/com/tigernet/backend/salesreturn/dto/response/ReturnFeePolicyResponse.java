package com.tigernet.backend.salesreturn.dto.response;

import com.tigernet.backend.salesreturn.enums.FeeActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReturnFeePolicyResponse {
    private Integer id;
    private String tierName;
    private Integer maxDays;
    private FeeActionType actionType;
    private BigDecimal feePercent;
    private Boolean isAllowed;
    private LocalDate effectiveFrom;
}
