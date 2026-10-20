package com.tigernet.backend.stocktake.controller;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.stocktake.dto.request.StocktakeCreationRequest;
import com.tigernet.backend.stocktake.dto.request.StocktakeItemsRequest;
import com.tigernet.backend.stocktake.dto.response.StockAdjustmentResponse;
import com.tigernet.backend.stocktake.dto.response.StocktakeResponse;
import com.tigernet.backend.stocktake.enums.StocktakeStatus;
import com.tigernet.backend.stocktake.service.StocktakeService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StocktakeController {
    StocktakeService stocktakeService;

    @PostMapping("/stocktakes")
    ApiResponse<StocktakeResponse> createStocktake(@RequestBody @Valid StocktakeCreationRequest request) {
        return ApiResponse.success(stocktakeService.createStocktake(request));
    }

    @GetMapping("/stocktakes") // ?warehouseId=&status=&from=&to=
    ApiResponse<List<StocktakeResponse>> getStocktakes(
            @RequestParam(required = false) Integer warehouseId,
            @RequestParam(required = false) StocktakeStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(stocktakeService.getStocktakes(warehouseId, status, from, to));
    }

    @GetMapping("/stocktakes/{stocktakeId}")
    ApiResponse<StocktakeResponse> getStocktake(@PathVariable Integer stocktakeId) {
        return ApiResponse.success(stocktakeService.getStocktake(stocktakeId));
    }

    @PutMapping("/stocktakes/{stocktakeId}/items")
    ApiResponse<StocktakeResponse> updateItems(@PathVariable Integer stocktakeId,
                                               @RequestBody @Valid StocktakeItemsRequest request) {
        return ApiResponse.success(stocktakeService.updateItems(stocktakeId, request));
    }

    @DeleteMapping("/stocktakes/{stocktakeId}")
    ApiResponse<Void> deleteStocktake(@PathVariable Integer stocktakeId) {
        stocktakeService.deleteStocktake(stocktakeId);
        return ApiResponse.success("Stocktake has been deleted", null);
    }

    // điều chỉnh tồn chỉ sinh khi chốt phiếu kiểm kê nên chỉ có API đọc
    @GetMapping("/stock-adjustments") // ?stocktakeId=&warehouseId=&productId=
    ApiResponse<List<StockAdjustmentResponse>> getAdjustments(@RequestParam(required = false) Integer stocktakeId,
                                                              @RequestParam(required = false) Integer warehouseId,
                                                              @RequestParam(required = false) Integer productId) {
        return ApiResponse.success(stocktakeService.getAdjustments(stocktakeId, warehouseId, productId));
    }

    @GetMapping("/stock-adjustments/{adjustmentId}")
    ApiResponse<StockAdjustmentResponse> getAdjustment(@PathVariable Integer adjustmentId) {
        return ApiResponse.success(stocktakeService.getAdjustment(adjustmentId));
    }
}
