package com.tigernet.backend.alert.controller;

import com.tigernet.backend.alert.dto.response.AlertResponse;
import com.tigernet.backend.alert.enums.AlertType;
import com.tigernet.backend.alert.service.AlertService;
import com.tigernet.backend.common.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AlertController {
    AlertService alertService;

    @GetMapping // ?warehouseId=&alertType=&isResolved=
    ApiResponse<List<AlertResponse>> getAlerts(@RequestParam(required = false) Integer warehouseId,
                                               @RequestParam(required = false) AlertType alertType,
                                               @RequestParam(required = false) Boolean isResolved) {
        return ApiResponse.success(alertService.getAlerts(warehouseId, alertType, isResolved));
    }

    @GetMapping("/unread-count") // ?warehouseId=
    ApiResponse<Long> countUnresolved(@RequestParam(required = false) Integer warehouseId) {
        return ApiResponse.success(alertService.countUnresolved(warehouseId));
    }

    @PutMapping("/{alertId}/read")
    ApiResponse<AlertResponse> markResolved(@PathVariable Integer alertId) {
        return ApiResponse.success(alertService.markResolved(alertId));
    }
}
