package com.tigernet.backend.outbound.controller;

import com.tigernet.backend.common.security.Pages;
import com.tigernet.backend.common.security.RequiresPage;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.outbound.dto.request.ExportReceiptCreationRequest;
import com.tigernet.backend.outbound.dto.request.ExportReceiptUpdateRequest;
import com.tigernet.backend.outbound.dto.request.PaymentRequest;
import com.tigernet.backend.outbound.dto.response.ExportReceiptResponse;
import com.tigernet.backend.outbound.enums.ExportStatus;
import com.tigernet.backend.outbound.service.ExportReceiptService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/export-receipts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExportReceiptController {
    ExportReceiptService exportReceiptService;

    @RequiresPage({Pages.EXPORTS_NEW})
    @PostMapping
    ApiResponse<ExportReceiptResponse> createReceipt(@RequestBody @Valid ExportReceiptCreationRequest request) {
        return ApiResponse.success(exportReceiptService.createReceipt(request));
    }

    @RequiresPage({Pages.EXPORTS, Pages.EXPORTS_NEW, Pages.PAYMENTS, Pages.PAYMENTS_NEW_IN, Pages.SALES_RETURNS, Pages.WARRANTY, Pages.WARRANTY_NEW, Pages.REPORT_RECEIVABLES, Pages.REPORT_RECONCILIATION})
    @GetMapping // ?warehouseId=&customerId=&status=&from=&to=
    ApiResponse<List<ExportReceiptResponse>> getReceipts(
            @RequestParam(required = false) Integer warehouseId,
            @RequestParam(required = false) Integer customerId,
            @RequestParam(required = false) ExportStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(exportReceiptService.getReceipts(warehouseId, customerId, status, from, to));
    }

    @RequiresPage({Pages.EXPORTS, Pages.EXPORTS_NEW, Pages.PAYMENTS, Pages.PAYMENTS_NEW_IN, Pages.SALES_RETURNS, Pages.WARRANTY, Pages.WARRANTY_NEW, Pages.REPORT_RECEIVABLES, Pages.REPORT_RECONCILIATION})
    @GetMapping("/{receiptId}")
    ApiResponse<ExportReceiptResponse> getReceipt(@PathVariable Integer receiptId) {
        return ApiResponse.success(exportReceiptService.getReceipt(receiptId));
    }

    @RequiresPage({Pages.EXPORTS_NEW})
    @PutMapping("/{receiptId}")
    ApiResponse<ExportReceiptResponse> updateReceipt(@PathVariable Integer receiptId,
                                                     @RequestBody @Valid ExportReceiptUpdateRequest request) {
        return ApiResponse.success(exportReceiptService.updateReceipt(receiptId, request));
    }

    // hủy phiếu PENDING: phiếu chuyển sang CANCELLED, không xóa
    @RequiresPage({Pages.EXPORTS_NEW})
    @DeleteMapping("/{receiptId}")
    ApiResponse<ExportReceiptResponse> cancelReceipt(@PathVariable Integer receiptId,
                                                     @RequestParam(required = false) String reason) {
        return ApiResponse.success(exportReceiptService.cancelReceipt(receiptId, reason));
    }

    @RequiresPage({Pages.EXPORTS_NEW, Pages.PAYMENTS_NEW_IN})
    @PostMapping("/{receiptId}/payments")
    ApiResponse<ExportReceiptResponse> recordPayment(@PathVariable Integer receiptId,
                                                     @RequestBody @Valid PaymentRequest request) {
        return ApiResponse.success(exportReceiptService.recordPayment(receiptId, request));
    }
}
