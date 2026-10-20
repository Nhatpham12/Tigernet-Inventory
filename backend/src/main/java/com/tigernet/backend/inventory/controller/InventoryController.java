package com.tigernet.backend.inventory.controller;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.inventory.dto.request.InventoryLocationRequest;
import com.tigernet.backend.inventory.dto.request.StockSettingRequest;
import com.tigernet.backend.inventory.dto.response.InventoryResponse;
import com.tigernet.backend.inventory.dto.response.StockMovementResponse;
import com.tigernet.backend.inventory.dto.response.StockSettingResponse;
import com.tigernet.backend.inventory.dto.response.StockSummaryResponse;
import com.tigernet.backend.inventory.enums.InventoryStatus;
import com.tigernet.backend.inventory.service.InventoryService;
import com.tigernet.backend.inventory.service.StockSettingService;
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
public class InventoryController {
    InventoryService inventoryService;
    StockSettingService stockSettingService;

    @GetMapping("/inventory") // ?warehouseId=&productId=&status=&serial=
    ApiResponse<List<InventoryResponse>> getInventories(@RequestParam(required = false) Integer warehouseId,
                                                        @RequestParam(required = false) Integer productId,
                                                        @RequestParam(required = false) InventoryStatus status,
                                                        @RequestParam(required = false) String serial) {
        return ApiResponse.success(inventoryService.getInventories(warehouseId, productId, status, serial));
    }

    @GetMapping("/inventory/{inventoryId}")
    ApiResponse<InventoryResponse> getInventory(@PathVariable Integer inventoryId) {
        return ApiResponse.success(inventoryService.getInventory(inventoryId));
    }

    @PutMapping("/inventory/{inventoryId}/location")
    ApiResponse<InventoryResponse> updateLocation(@PathVariable Integer inventoryId,
                                                  @RequestBody InventoryLocationRequest request) {
        return ApiResponse.success(inventoryService.updateLocation(inventoryId, request));
    }

    @GetMapping("/stock-summary") // ?warehouseId=&productId=
    ApiResponse<List<StockSummaryResponse>> getStockSummaries(@RequestParam(required = false) Integer warehouseId,
                                                              @RequestParam(required = false) Integer productId) {
        return ApiResponse.success(inventoryService.getStockSummaries(warehouseId, productId));
    }

    @GetMapping("/stock-movements") // ?warehouseId=&productId=&from=2026-10-01&to=2026-10-31
    ApiResponse<List<StockMovementResponse>> getStockMovements(
            @RequestParam(required = false) Integer warehouseId,
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(inventoryService.getStockMovements(warehouseId, productId, from, to));
    }

    @GetMapping("/stock-settings") // ?warehouseId=&productId=
    ApiResponse<List<StockSettingResponse>> getStockSettings(@RequestParam(required = false) Integer warehouseId,
                                                             @RequestParam(required = false) Integer productId) {
        return ApiResponse.success(stockSettingService.getSettings(warehouseId, productId));
    }

    @PutMapping("/stock-settings")
    ApiResponse<StockSettingResponse> saveStockSetting(@RequestBody @Valid StockSettingRequest request) {
        return ApiResponse.success(stockSettingService.saveSetting(request));
    }
}
