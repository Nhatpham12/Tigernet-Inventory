package com.tigernet.backend.warehouse.controller;

import com.tigernet.backend.common.security.NoPageCheck;
import com.tigernet.backend.common.security.Pages;
import com.tigernet.backend.common.security.RequiresPage;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.warehouse.dto.request.WarehouseCreationRequest;
import com.tigernet.backend.warehouse.dto.request.WarehouseLocationRequest;
import com.tigernet.backend.warehouse.dto.request.WarehouseUpdateRequest;
import com.tigernet.backend.warehouse.dto.response.WarehouseLocationResponse;
import com.tigernet.backend.warehouse.dto.response.WarehouseResponse;
import com.tigernet.backend.warehouse.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WarehouseController {
    WarehouseService warehouseService;

    @RequiresPage({Pages.WAREHOUSES})
    @PostMapping("/warehouses")
    ApiResponse<WarehouseResponse> createWarehouse(@RequestBody @Valid WarehouseCreationRequest request) {
        return ApiResponse.success(warehouseService.createWarehouse(request));
    }

    @NoPageCheck("Danh mục tra cứu dùng chung cho form, dropdown và dashboard")
    @GetMapping("/warehouses") // ?isActive=true để chỉ lấy kho đang hoạt động
    ApiResponse<List<WarehouseResponse>> getWarehouses(@RequestParam(required = false) Boolean isActive) {
        return ApiResponse.success(warehouseService.getWarehouses(isActive));
    }

    @NoPageCheck("Danh mục tra cứu dùng chung cho form, dropdown và dashboard")
    @GetMapping("/warehouses/{warehouseId}")
    ApiResponse<WarehouseResponse> getWarehouse(@PathVariable Integer warehouseId) {
        return ApiResponse.success(warehouseService.getWarehouse(warehouseId));
    }

    @RequiresPage({Pages.WAREHOUSES})
    @PutMapping("/warehouses/{warehouseId}")
    ApiResponse<WarehouseResponse> updateWarehouse(@PathVariable Integer warehouseId,
                                                   @RequestBody @Valid WarehouseUpdateRequest request) {
        return ApiResponse.success(warehouseService.updateWarehouse(warehouseId, request));
    }

    @RequiresPage({Pages.WAREHOUSES})
    @DeleteMapping("/warehouses/{warehouseId}")
    ApiResponse<Void> deleteWarehouse(@PathVariable Integer warehouseId) {
        warehouseService.deleteWarehouse(warehouseId);
        return ApiResponse.success("Warehouse has been deleted", null);
    }

    @NoPageCheck("Danh mục tra cứu dùng chung cho form, dropdown và dashboard")
    @GetMapping("/warehouses/{warehouseId}/locations")
    ApiResponse<List<WarehouseLocationResponse>> getLocations(@PathVariable Integer warehouseId) {
        return ApiResponse.success(warehouseService.getLocations(warehouseId));
    }

    @RequiresPage({Pages.WAREHOUSES})
    @PostMapping("/warehouses/{warehouseId}/locations")
    ApiResponse<WarehouseLocationResponse> createLocation(@PathVariable Integer warehouseId,
                                                          @RequestBody @Valid WarehouseLocationRequest request) {
        return ApiResponse.success(warehouseService.createLocation(warehouseId, request));
    }

    @RequiresPage({Pages.WAREHOUSES})
    @PutMapping("/warehouse-locations/{locationId}")
    ApiResponse<WarehouseLocationResponse> updateLocation(@PathVariable Integer locationId,
                                                          @RequestBody @Valid WarehouseLocationRequest request) {
        return ApiResponse.success(warehouseService.updateLocation(locationId, request));
    }

    @RequiresPage({Pages.WAREHOUSES})
    @DeleteMapping("/warehouse-locations/{locationId}")
    ApiResponse<Void> deleteLocation(@PathVariable Integer locationId) {
        warehouseService.deleteLocation(locationId);
        return ApiResponse.success("Location has been deleted", null);
    }
}
