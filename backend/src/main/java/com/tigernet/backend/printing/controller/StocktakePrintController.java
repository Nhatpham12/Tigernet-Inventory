package com.tigernet.backend.printing.controller;

import com.tigernet.backend.printing.service.StocktakePrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StocktakePrintController {
    private final StocktakePrintService service;

    // Biên bản kiểm kê 05-VT (mặc định) hoặc ?template=adjustment: phiếu điều chỉnh tồn kho / xử lý hàng hư hỏng, mất
    @GetMapping("/stocktakes/{id}/print")
    ResponseEntity<byte[]> print(@PathVariable Integer id,
                                 @RequestParam(required = false) String template,
                                 @RequestParam(required = false) String format,
                                 @RequestParam(defaultValue = "false") boolean download) {
        return service.print(id, template, format, download);
    }
}
