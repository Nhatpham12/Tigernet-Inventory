package com.tigernet.backend.printing.controller;

import com.tigernet.backend.printing.service.ImportReceiptPrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ImportReceiptPrintController {
    private final ImportReceiptPrintService service;

    // Phiếu nhập kho 01-VT. ?format=html để xem HTML, ?download=true để tải file
    @GetMapping("/import-receipts/{id}/print")
    ResponseEntity<byte[]> print(@PathVariable Integer id,
                                 @RequestParam(required = false) String format,
                                 @RequestParam(defaultValue = "false") boolean download) {
        return service.print(id, format, download);
    }
}
