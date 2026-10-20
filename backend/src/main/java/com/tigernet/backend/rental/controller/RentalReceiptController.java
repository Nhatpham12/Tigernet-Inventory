package com.tigernet.backend.rental.controller;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.rental.dto.request.RentalReceiptCreationRequest;
import com.tigernet.backend.rental.dto.request.RentalReceiptUpdateRequest;
import com.tigernet.backend.rental.dto.response.RentalReceiptResponse;
import com.tigernet.backend.rental.enums.RentalType;
import com.tigernet.backend.rental.service.RentalReceiptService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/rental-receipts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RentalReceiptController {
    RentalReceiptService rentalReceiptService;

    @PostMapping
    ApiResponse<RentalReceiptResponse> createReceipt(@RequestBody @Valid RentalReceiptCreationRequest request) {
        return ApiResponse.success(rentalReceiptService.createReceipt(request));
    }

    @GetMapping // ?warehouseId=&customerId=&type=RENT|LEND&paymentStatus=RENTING&from=&to=
    ApiResponse<List<RentalReceiptResponse>> getReceipts(
            @RequestParam(required = false) Integer warehouseId,
            @RequestParam(required = false) Integer customerId,
            @RequestParam(name = "type", required = false) RentalType rentalType,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(rentalReceiptService.getReceipts(
                warehouseId, customerId, rentalType, paymentStatus, from, to));
    }

    @GetMapping("/expiring") // ?days=7&warehouseId=
    ApiResponse<List<RentalReceiptResponse>> getExpiringReceipts(@RequestParam(required = false) Integer days,
                                                                 @RequestParam(required = false) Integer warehouseId) {
        return ApiResponse.success(rentalReceiptService.getExpiringReceipts(days, warehouseId));
    }

    @GetMapping("/{receiptId}")
    ApiResponse<RentalReceiptResponse> getReceipt(@PathVariable Integer receiptId) {
        return ApiResponse.success(rentalReceiptService.getReceipt(receiptId));
    }

    @PutMapping("/{receiptId}")
    ApiResponse<RentalReceiptResponse> updateReceipt(@PathVariable Integer receiptId,
                                                     @RequestBody @Valid RentalReceiptUpdateRequest request) {
        return ApiResponse.success(rentalReceiptService.updateReceipt(receiptId, request));
    }

    // hủy phiếu (Payment_Status = CANCELLED), không xóa bản ghi
    @DeleteMapping("/{receiptId}")
    ApiResponse<RentalReceiptResponse> cancelReceipt(@PathVariable Integer receiptId) {
        return ApiResponse.success("Rental receipt has been cancelled", rentalReceiptService.cancelReceipt(receiptId));
    }
}
