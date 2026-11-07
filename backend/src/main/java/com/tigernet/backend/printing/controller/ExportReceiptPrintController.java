package com.tigernet.backend.printing.controller;

import com.tigernet.backend.printing.service.ExportReceiptPrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExportReceiptPrintController {
    private final ExportReceiptPrintService service;

    // template: invoice (Phiếu xuất kho 02-VT, mặc định) | delivery (Phiếu gửi hàng A5, ?parcels=N số kiện)
    // | handover (Biên bản bàn giao – Phiếu bảo hành). ?format=html xem HTML, ?download=true tải file
    @GetMapping("/export-receipts/{id}/print")
    ResponseEntity<byte[]> print(@PathVariable Integer id,
                                 @RequestParam(defaultValue = "invoice") String template,
                                 @RequestParam(required = false) Integer parcels,
                                 @RequestParam(required = false) String format,
                                 @RequestParam(defaultValue = "false") boolean download) {
        return service.print(id, template, parcels, format, download);
    }
}
