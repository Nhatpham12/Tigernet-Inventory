package com.tigernet.backend.audit.controller;

import com.tigernet.backend.audit.dto.response.AuditLogResponse;
import com.tigernet.backend.audit.service.AuditLogService;
import com.tigernet.backend.common.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// nhật ký chỉ đọc, không có API sửa/xóa
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditLogController {
    AuditLogService auditLogService;

    @GetMapping // ?userId=&action=&targetTable=&targetId=&from=&to=
    ApiResponse<List<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetTable,
            @RequestParam(required = false) Integer targetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(auditLogService.getAuditLogs(userId, action, targetTable, targetId, from, to));
    }
}
