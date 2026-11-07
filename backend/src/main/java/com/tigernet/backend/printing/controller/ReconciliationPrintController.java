package com.tigernet.backend.printing.controller;

import com.tigernet.backend.printing.service.ReconciliationPrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class ReconciliationPrintController {
    private final ReconciliationPrintService service;

    // Biên bản đối chiếu công nợ của một đối tác trong kỳ [from, to]
    @GetMapping("/reports/reconciliation/print")
    ResponseEntity<byte[]> print(@RequestParam Integer partyId,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                 @RequestParam(required = false) String format,
                                 @RequestParam(defaultValue = "false") boolean download) {
        return service.print(partyId, from, to, format, download);
    }
}
