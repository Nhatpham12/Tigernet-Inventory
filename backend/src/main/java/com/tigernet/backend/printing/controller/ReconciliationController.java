package com.tigernet.backend.printing.controller;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.partner.service.PartyService;
import com.tigernet.backend.printing.dto.DebtStatementResponse;
import com.tigernet.backend.printing.service.DebtReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class ReconciliationController {
    private final DebtReconciliationService service;
    private final PartyService partyService;

    // xem trước đối chiếu công nợ một đối tác trong kỳ [from, to] (bản in: /reports/reconciliation/print)
    @GetMapping("/reports/reconciliation")
    @Transactional(readOnly = true)
    ApiResponse<DebtStatementResponse> reconciliation(
            @RequestParam Integer partyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (to.isBefore(from)) throw new AppException(ErrorCode.INVALID_PARAMETER);
        String name = partyService.findParty(partyId).getFullName();
        return ApiResponse.success(DebtStatementResponse.of(partyId, name, from, to,
                service.compute(partyId, from, to)));
    }
}
