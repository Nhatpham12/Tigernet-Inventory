package com.tigernet.backend.alert.service;

import com.tigernet.backend.warehouse.service.WarehouseAccessService;
import com.tigernet.backend.alert.dto.response.AlertResponse;
import com.tigernet.backend.alert.entity.AlertEntity;
import com.tigernet.backend.alert.enums.AlertType;
import com.tigernet.backend.alert.mapper.AlertMapper;
import com.tigernet.backend.alert.repository.AlertRepository;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

// cảnh báo do các job sinh ra; người dùng chỉ xem và đánh dấu đã xử lý
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class AlertService {
    AlertRepository alertRepository;
    AlertMapper alertMapper;
    WarehouseAccessService warehouseAccess;

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlerts(Integer warehouseId, AlertType alertType, Boolean isResolved) {
        return alertMapper.toAlertResponses(visible(alertRepository.search(warehouseId, alertType, isResolved),
                warehouseId));
    }

    @Transactional(readOnly = true)
    public long countUnresolved(Integer warehouseId) {
        if (warehouseAccess.all()) {
            return alertRepository.countUnresolved(warehouseId);
        }
        return visible(alertRepository.search(warehouseId, null, false), warehouseId).size();
    }

    // cảnh báo toàn công ty (không gắn kho) ai cũng thấy; cảnh báo theo kho chỉ người được giao kho đó thấy
    private List<AlertEntity> visible(List<AlertEntity> alerts, Integer warehouseId) {
        Set<Integer> allowed = warehouseAccess.allowedWarehouseIds();
        if (allowed == null) {
            return alerts;
        }
        if (warehouseId != null) {
            warehouseAccess.assertAccess(warehouseId);
        }
        return alerts.stream().filter(a -> a.getWarehouse() == null || allowed.contains(a.getWarehouse().getId()))
                .toList();
    }

    // Is_Resolved là cờ dùng chung: "đã đọc" trên giao diện = đã xử lý
    public AlertResponse markResolved(Integer alertId) {
        AlertEntity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AppException(ErrorCode.ALERT_NOT_FOUND));
        if (alert.getWarehouse() != null) {
            warehouseAccess.assertAccess(alert.getWarehouse());
        }
        alert.setIsResolved(true);
        return alertMapper.toAlertResponse(alertRepository.save(alert));
    }
}
