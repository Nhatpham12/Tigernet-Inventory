package com.tigernet.backend.transfer.controller;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.transfer.dto.request.StockTransferCreationRequest;
import com.tigernet.backend.transfer.dto.request.StockTransferUpdateRequest;
import com.tigernet.backend.transfer.dto.response.StockTransferResponse;
import com.tigernet.backend.transfer.enums.TransferStatus;
import com.tigernet.backend.transfer.service.StockTransferService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock-transfers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockTransferController {
    StockTransferService stockTransferService;

    @PostMapping
    ApiResponse<StockTransferResponse> createTransfer(@RequestBody @Valid StockTransferCreationRequest request) {
        return ApiResponse.success(stockTransferService.createTransfer(request));
    }

    @GetMapping // ?fromWarehouseId=&toWarehouseId=&status=
    ApiResponse<List<StockTransferResponse>> getTransfers(@RequestParam(required = false) Integer fromWarehouseId,
                                                          @RequestParam(required = false) Integer toWarehouseId,
                                                          @RequestParam(required = false) TransferStatus status) {
        return ApiResponse.success(stockTransferService.getTransfers(fromWarehouseId, toWarehouseId, status));
    }

    @GetMapping("/{transferId}")
    ApiResponse<StockTransferResponse> getTransfer(@PathVariable Integer transferId) {
        return ApiResponse.success(stockTransferService.getTransfer(transferId));
    }

    @PutMapping("/{transferId}")
    ApiResponse<StockTransferResponse> updateTransfer(@PathVariable Integer transferId,
                                                      @RequestBody @Valid StockTransferUpdateRequest request) {
        return ApiResponse.success(stockTransferService.updateTransfer(transferId, request));
    }

    @PutMapping("/{transferId}/approve")
    ApiResponse<StockTransferResponse> approveTransfer(@PathVariable Integer transferId) {
        return ApiResponse.success(stockTransferService.approveTransfer(transferId));
    }

    @PutMapping("/{transferId}/cancel")
    ApiResponse<StockTransferResponse> cancelTransfer(@PathVariable Integer transferId) {
        return ApiResponse.success(stockTransferService.cancelTransfer(transferId));
    }

    @DeleteMapping("/{transferId}")
    ApiResponse<Void> deleteTransfer(@PathVariable Integer transferId) {
        stockTransferService.deleteTransfer(transferId);
        return ApiResponse.success("Stock transfer has been deleted", null);
    }
}
