package com.tigernet.backend.inventory.service;

import com.tigernet.backend.catalog.service.ProductService;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.inventory.dto.request.StockSettingRequest;
import com.tigernet.backend.inventory.dto.response.StockSettingResponse;
import com.tigernet.backend.inventory.entity.ProductWarehouseSettingEntity;
import com.tigernet.backend.inventory.mapper.InventoryMapper;
import com.tigernet.backend.inventory.repository.ProductWarehouseSettingRepository;
import com.tigernet.backend.warehouse.service.WarehouseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// ngưỡng tồn Min/Max/điểm đặt hàng lại theo (SP, kho)
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class StockSettingService {
    ProductWarehouseSettingRepository settingRepository;
    ProductService productService;
    WarehouseService warehouseService;
    InventoryMapper inventoryMapper;

    @Transactional(readOnly = true)
    public List<StockSettingResponse> getSettings(Integer warehouseId, Integer productId) {
        return inventoryMapper.toStockSettingResponses(settingRepository.search(warehouseId, productId));
    }

    // chưa có thì tạo, có rồi thì ghi đè
    public StockSettingResponse saveSetting(StockSettingRequest request) {
        if (request.getMinStock() != null && request.getMaxStock() != null
                && request.getMaxStock() < request.getMinStock()) {
            throw new AppException(ErrorCode.STOCK_SETTING_INVALID);
        }
        ProductWarehouseSettingEntity setting = settingRepository
                .findByProductIdAndWarehouseId(request.getProductId(), request.getWarehouseId())
                .orElseGet(() -> {
                    ProductWarehouseSettingEntity created = new ProductWarehouseSettingEntity();
                    created.setProduct(productService.findProduct(request.getProductId()));
                    created.setWarehouse(warehouseService.findWarehouse(request.getWarehouseId()));
                    return created;
                });
        setting.setMinStock(request.getMinStock());
        setting.setMaxStock(request.getMaxStock());
        setting.setReorderPoint(request.getReorderPoint());
        return inventoryMapper.toStockSettingResponse(settingRepository.save(setting));
    }
}
