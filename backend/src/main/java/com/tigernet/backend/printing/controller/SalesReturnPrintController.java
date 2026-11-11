package com.tigernet.backend.printing.controller;

import com.tigernet.backend.printing.service.SalesReturnPrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SalesReturnPrintController {
    private final SalesReturnPrintService service;

    // Phiếu nhận hàng trả lại / đổi hàng
    @GetMapping("/sales-returns/{id}/print")
    ResponseEntity<byte[]> print(@PathVariable Integer id,
                                 @RequestParam(required = false) String format,
                                 @RequestParam(defaultValue = "false") boolean download) {
        return service.print(id, format, download);
    }
}
