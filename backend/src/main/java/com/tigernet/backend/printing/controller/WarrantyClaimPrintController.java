package com.tigernet.backend.printing.controller;

import com.tigernet.backend.printing.service.WarrantyClaimPrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class WarrantyClaimPrintController {
    private final WarrantyClaimPrintService service;

    // Biên bản tiếp nhận bảo hành – sửa chữa cho 1 phiếu bảo hành
    @GetMapping("/warranty-claims/{id}/print")
    ResponseEntity<byte[]> print(@PathVariable Integer id,
                                 @RequestParam(required = false) String format,
                                 @RequestParam(defaultValue = "false") boolean download) {
        return service.print(List.of(id), format, download);
    }

    // In chung nhiều phiếu bảo hành của cùng một khách: ?ids=1,2,3
    @GetMapping("/warranty-claims/print")
    ResponseEntity<byte[]> printMany(@RequestParam List<Integer> ids,
                                     @RequestParam(required = false) String format,
                                     @RequestParam(defaultValue = "false") boolean download) {
        return service.print(ids, format, download);
    }
}
