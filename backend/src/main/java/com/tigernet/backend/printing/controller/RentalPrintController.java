package com.tigernet.backend.printing.controller;

import com.tigernet.backend.printing.service.RentalPrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RentalPrintController {
    private final RentalPrintService service;

    // Biên bản bàn giao cho thuê (RENT) / cho mượn (LEND). ?format=html để xem HTML, ?download=true để tải file
    @GetMapping("/rental-receipts/{id}/print")
    ResponseEntity<byte[]> printRental(@PathVariable Integer id,
                                       @RequestParam(required = false) String format,
                                       @RequestParam(defaultValue = "false") boolean download) {
        return service.printRental(id, format, download);
    }

    // Biên bản trả hàng thuê / mượn
    @GetMapping("/return-receipts/{id}/print")
    ResponseEntity<byte[]> printReturn(@PathVariable Integer id,
                                       @RequestParam(required = false) String format,
                                       @RequestParam(defaultValue = "false") boolean download) {
        return service.printReturn(id, format, download);
    }
}
