package com.tigernet.backend.inbound.controller;

import com.tigernet.backend.common.security.Pages;
import com.tigernet.backend.common.security.RequiresPage;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.inbound.dto.request.ImportReceiptCreationRequest;
import com.tigernet.backend.inbound.dto.request.ImportReceiptUpdateRequest;
import com.tigernet.backend.inbound.dto.response.ImportReceiptResponse;
import com.tigernet.backend.inbound.enums.ImportStatus;
import com.tigernet.backend.inbound.service.ImportReceiptService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/import-receipts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ImportReceiptController {
    ImportReceiptService importReceiptService;

    @RequiresPage({Pages.IMPORTS_NEW})
    @PostMapping
    ApiResponse<ImportReceiptResponse> createReceipt(@RequestBody @Valid ImportReceiptCreationRequest request) {
        return ApiResponse.success(importReceiptService.createReceipt(request));
    }

    @RequiresPage({Pages.IMPORTS, Pages.IMPORTS_NEW, Pages.PAYMENTS, Pages.PAYMENTS_NEW_OUT})
    @GetMapping // ?warehouseId=&status=&sourcePartyId=&from=&to=
    ApiResponse<List<ImportReceiptResponse>> getReceipts(
            @RequestParam(required = false) Integer warehouseId,
            @RequestParam(required = false) ImportStatus status,
            @RequestParam(required = false) Integer sourcePartyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(importReceiptService.getReceipts(warehouseId, status, sourcePartyId, from, to));
    }

    @RequiresPage({Pages.IMPORTS, Pages.IMPORTS_NEW, Pages.PAYMENTS, Pages.PAYMENTS_NEW_OUT})
    @GetMapping("/{receiptId}")
    ApiResponse<ImportReceiptResponse> getReceipt(@PathVariable Integer receiptId) {
        return ApiResponse.success(importReceiptService.getReceipt(receiptId));
    }

    @RequiresPage({Pages.IMPORTS_NEW})
    @PutMapping("/{receiptId}")
    ApiResponse<ImportReceiptResponse> updateReceipt(@PathVariable Integer receiptId,
                                                     @RequestBody @Valid ImportReceiptUpdateRequest request) {
        return ApiResponse.success(importReceiptService.updateReceipt(receiptId, request));
    }

    // hủy phiếu chưa nhập kho: phiếu chuyển sang CANCELLED, không xóa
    @RequiresPage({Pages.IMPORTS_NEW})
    @DeleteMapping("/{receiptId}")
    ApiResponse<ImportReceiptResponse> cancelReceipt(@PathVariable Integer receiptId,
                                                     @RequestParam(required = false) String reason) {
        return ApiResponse.success(importReceiptService.cancelReceipt(receiptId, reason));
    }
}
