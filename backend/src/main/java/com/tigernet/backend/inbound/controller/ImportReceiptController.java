package com.tigernet.backend.inbound.controller;

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

    @PostMapping
    ApiResponse<ImportReceiptResponse> createReceipt(@RequestBody @Valid ImportReceiptCreationRequest request) {
        return ApiResponse.success(importReceiptService.createReceipt(request));
    }

    @GetMapping // ?warehouseId=&status=&sourcePartyId=&from=&to=
    ApiResponse<List<ImportReceiptResponse>> getReceipts(
            @RequestParam(required = false) Integer warehouseId,
            @RequestParam(required = false) ImportStatus status,
            @RequestParam(required = false) Integer sourcePartyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(importReceiptService.getReceipts(warehouseId, status, sourcePartyId, from, to));
    }

    @GetMapping("/{receiptId}")
    ApiResponse<ImportReceiptResponse> getReceipt(@PathVariable Integer receiptId) {
        return ApiResponse.success(importReceiptService.getReceipt(receiptId));
    }

    @PutMapping("/{receiptId}")
    ApiResponse<ImportReceiptResponse> updateReceipt(@PathVariable Integer receiptId,
                                                     @RequestBody @Valid ImportReceiptUpdateRequest request) {
        return ApiResponse.success(importReceiptService.updateReceipt(receiptId, request));
    }

    @DeleteMapping("/{receiptId}")
    ApiResponse<Void> deleteReceipt(@PathVariable Integer receiptId) {
        importReceiptService.deleteReceipt(receiptId);
        return ApiResponse.success("Import receipt has been deleted", null);
    }
}
