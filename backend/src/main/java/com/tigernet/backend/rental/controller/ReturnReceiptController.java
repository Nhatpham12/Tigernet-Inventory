package com.tigernet.backend.rental.controller;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.rental.dto.request.ReturnReceiptCreationRequest;
import com.tigernet.backend.rental.dto.response.ReturnReceiptResponse;
import com.tigernet.backend.rental.service.ReturnReceiptService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// phiếu trả đã ảnh hưởng phiếu thuê nên không có API sửa/xóa
@RestController
@RequestMapping("/return-receipts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReturnReceiptController {
    ReturnReceiptService returnReceiptService;

    @PostMapping
    ApiResponse<ReturnReceiptResponse> createReceipt(@RequestBody @Valid ReturnReceiptCreationRequest request) {
        return ApiResponse.success(returnReceiptService.createReceipt(request));
    }

    @GetMapping // ?warehouseId=&rentalReceiptId=
    ApiResponse<List<ReturnReceiptResponse>> getReceipts(@RequestParam(required = false) Integer warehouseId,
                                                         @RequestParam(required = false) Integer rentalReceiptId) {
        return ApiResponse.success(returnReceiptService.getReceipts(warehouseId, rentalReceiptId));
    }

    @GetMapping("/{receiptId}")
    ApiResponse<ReturnReceiptResponse> getReceipt(@PathVariable Integer receiptId) {
        return ApiResponse.success(returnReceiptService.getReceipt(receiptId));
    }
}
