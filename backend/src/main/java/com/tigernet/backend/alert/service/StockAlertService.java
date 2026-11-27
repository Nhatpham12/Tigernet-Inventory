package com.tigernet.backend.alert.service;

import com.tigernet.backend.alert.entity.AlertEntity;
import com.tigernet.backend.alert.enums.AlertType;
import com.tigernet.backend.alert.repository.AlertRepository;
import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.inventory.entity.ProductWarehouseSettingEntity;
import com.tigernet.backend.inventory.repository.ProductWarehouseSettingRepository;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Cảnh báo sinh tự động theo biến động kho và lịch (không có API tạo tay).
 * Mỗi đối tượng chỉ có tối đa 1 cảnh báo đang mở cùng loại; hết điều kiện thì tự đánh dấu đã xử lý.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class StockAlertService {
    public static final String REF_PRODUCTS = "Products";
    public static final String REF_RENTALS = "Rental_Receipts";
    static final int MESSAGE_MAX = 255;

    AlertRepository alertRepository;
    ProductWarehouseSettingRepository settingRepository;

    /**
     * Gọi sau mỗi lần tồn khả dụng của (SP, kho) đổi. Ngưỡng lấy Min_Stock của kho, không có thì Min_Stock_Level của SP
     * (khớp cờ "Tồn thấp" ở BFF: tồn ≤ ngưỡng). Ngưỡng 0 = không theo dõi.
     */
    public void refreshLowStock(ProductEntity product, WarehouseEntity warehouse, int onHand) {
        int min = settingRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .map(ProductWarehouseSettingEntity::getMinStock)
                .filter(Objects::nonNull)
                .orElse(Objects.requireNonNullElse(product.getMinStockLevel(), 0));
        boolean low = min > 0 && onHand <= min;
        AlertEntity open = alertRepository.findFirstByAlertTypeAndWarehouseIdAndRefTableAndRefIdAndIsResolvedFalse(
                AlertType.LOW_STOCK, warehouse.getId(), REF_PRODUCTS, product.getId()).orElse(null);
        if (low && open == null) {
            open(AlertType.LOW_STOCK, warehouse, REF_PRODUCTS, product.getId(),
                    "Tồn thấp: %s tại %s còn %d (ngưỡng %d)".formatted(product.getProductName(),
                            warehouse.getWarehouseName(), onHand, min));
        } else if (!low && open != null) {
            open.setIsResolved(true);
            alertRepository.save(open);
        }
    }

    /** đánh dấu đã xử lý mọi cảnh báo đang mở của một chứng từ (VD phiếu thuê đã trả đủ / đã hủy) */
    public void resolve(AlertType type, String refTable, Integer refId) {
        for (AlertEntity a : alertRepository.findByAlertTypeAndRefTableAndRefIdAndIsResolvedFalse(type, refTable, refId)) {
            a.setIsResolved(true);
            alertRepository.save(a);
        }
    }

    public void open(AlertType type, WarehouseEntity warehouse, String refTable, Integer refId, String message) {
        AlertEntity alert = new AlertEntity();
        alert.setAlertType(type);
        alert.setWarehouse(warehouse);
        alert.setRefTable(refTable);
        alert.setRefId(refId);
        alert.setMessage(message.length() > MESSAGE_MAX ? message.substring(0, MESSAGE_MAX) : message);
        alert.setIsResolved(false);
        alert.setCreatedAt(LocalDateTime.now());
        alertRepository.save(alert);
    }
}
