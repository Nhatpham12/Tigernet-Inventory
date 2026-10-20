package com.tigernet.backend.inventory.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.inventory.dto.request.InventoryLocationRequest;
import com.tigernet.backend.inventory.dto.response.InventoryResponse;
import com.tigernet.backend.inventory.dto.response.StockMovementResponse;
import com.tigernet.backend.inventory.dto.response.StockSummaryResponse;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import com.tigernet.backend.inventory.entity.StockSummaryId;
import com.tigernet.backend.inventory.entity.StockSummaryEntity;
import com.tigernet.backend.inventory.enums.InventoryStatus;
import com.tigernet.backend.inventory.mapper.InventoryMapper;
import com.tigernet.backend.inventory.repository.InventoryRepository;
import com.tigernet.backend.inventory.repository.StockMovementRepository;
import com.tigernet.backend.inventory.repository.StockSummaryRepository;
import com.tigernet.backend.warehouse.entity.WarehouseLocationEntity;
import com.tigernet.backend.warehouse.service.WarehouseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

// phía đọc của tồn kho; thay đổi số lượng tồn sẽ đi qua StockService (chưa làm)
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class InventoryService {
    InventoryRepository inventoryRepository;
    StockSummaryRepository stockSummaryRepository;
    StockMovementRepository stockMovementRepository;
    WarehouseService warehouseService;
    InventoryMapper inventoryMapper;

    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventories(Integer warehouseId, Integer productId,
                                                  InventoryStatus status, String serial) {
        String normalizedSerial = StringUtils.hasText(serial) ? serial.trim() : null;
        return inventoryMapper.toInventoryResponses(
                inventoryRepository.search(warehouseId, productId, status, normalizedSerial));
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Integer inventoryId) {
        return inventoryMapper.toInventoryResponse(findInventory(inventoryId));
    }

    // chỉ đổi vị trí kệ/ô, không đổi số lượng hay trạng thái
    public InventoryResponse updateLocation(Integer inventoryId, InventoryLocationRequest request) {
        InventoryEntity inventory = findInventory(inventoryId);
        if (request.getLocationId() == null) {
            inventory.setLocation(null);
        } else {
            WarehouseLocationEntity location = warehouseService.findLocation(request.getLocationId());
            // vị trí phải thuộc đúng kho đang giữ hàng
            if (!location.getWarehouse().getId().equals(inventory.getWarehouse().getId())) {
                throw new AppException(ErrorCode.LOCATION_NOT_IN_WAREHOUSE);
            }
            inventory.setLocation(location);
        }
        return inventoryMapper.toInventoryResponse(inventoryRepository.save(inventory));
    }

    @Transactional(readOnly = true)
    public List<StockSummaryResponse> getStockSummaries(Integer warehouseId, Integer productId) {
        return inventoryMapper.toStockSummaryResponses(stockSummaryRepository.search(warehouseId, productId));
    }

    // to là ngày cuối (tính cả ngày đó)
    @Transactional(readOnly = true)
    public List<StockMovementResponse> getStockMovements(Integer warehouseId, Integer productId,
                                                         LocalDate from, LocalDate to) {
        return inventoryMapper.toStockMovementResponses(stockMovementRepository.search(
                warehouseId, productId,
                from != null ? from.atStartOfDay() : null,
                to != null ? to.plusDays(1).atStartOfDay() : null));
    }

    // ---------- dùng cho module khác ----------

    public InventoryEntity findInventory(Integer inventoryId) {
        return inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));
    }

    // dòng chi tiết có Inventory_ID thì Inventory.Product_ID phải khớp Product_ID của dòng
    public InventoryEntity findInventoryOfProduct(Integer inventoryId, Integer productId) {
        InventoryEntity inventory = findInventory(inventoryId);
        if (!inventory.getProduct().getId().equals(productId)) {
            throw new AppException(ErrorCode.INVENTORY_PRODUCT_MISMATCH);
        }
        return inventory;
    }

    // phiếu lấy hàng ra khỏi kho (xuất, thuê, chuyển): đơn vị hàng phải đúng SP và đang ở kho của phiếu
    public InventoryEntity findInventoryForLine(Integer inventoryId, Integer productId, Integer warehouseId) {
        InventoryEntity inventory = findInventoryOfProduct(inventoryId, productId);
        if (!inventory.getWarehouse().getId().equals(warehouseId)) {
            throw new AppException(ErrorCode.INVENTORY_NOT_IN_WAREHOUSE);
        }
        return inventory;
    }

    // serial là duy nhất toàn hệ thống
    @Transactional(readOnly = true)
    public boolean serialExists(String serial) {
        return inventoryRepository.existsBySerial(serial);
    }

    // tồn khả dụng của (SP, kho) theo Stock_Summary, chưa có dòng nào thì là 0
    @Transactional(readOnly = true)
    public int getQtyOnHand(Integer productId, Integer warehouseId) {
        return stockSummaryRepository.findById(new StockSummaryId(productId, warehouseId))
                .map(StockSummaryEntity::getQtyOnHand)
                .orElse(0);
    }
}
