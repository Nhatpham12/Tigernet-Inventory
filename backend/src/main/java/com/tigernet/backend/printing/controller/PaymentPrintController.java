package com.tigernet.backend.printing.controller;

import com.tigernet.backend.printing.service.PaymentPrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentPrintController {
    private final PaymentPrintService service;

    // Phiếu thu 01-TT (IN) / phiếu chi 02-TT (OUT). ?format=html để xem HTML, ?download=true để tải file
    @GetMapping("/payments/{id}/print")
    ResponseEntity<byte[]> print(@PathVariable Integer id,
                                 @RequestParam(required = false) String format,
                                 @RequestParam(defaultValue = "false") boolean download) {
        return service.print(id, format, download);
    }
}
