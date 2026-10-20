package com.tigernet.backend.alert.service;

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

// cảnh báo do các job sinh ra; người dùng chỉ xem và đánh dấu đã xử lý
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class AlertService {
    AlertRepository alertRepository;
    AlertMapper alertMapper;

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlerts(Integer warehouseId, AlertType alertType, Boolean isResolved) {
        return alertMapper.toAlertResponses(alertRepository.search(warehouseId, alertType, isResolved));
    }

    @Transactional(readOnly = true)
    public long countUnresolved(Integer warehouseId) {
        return alertRepository.countUnresolved(warehouseId);
    }

    // Is_Resolved là cờ dùng chung: "đã đọc" trên giao diện = đã xử lý
    public AlertResponse markResolved(Integer alertId) {
        AlertEntity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AppException(ErrorCode.ALERT_NOT_FOUND));
        alert.setIsResolved(true);
        return alertMapper.toAlertResponse(alertRepository.save(alert));
    }
}
