package com.tigernet.backend.audit.service;

import com.tigernet.backend.audit.dto.response.AuditLogResponse;
import com.tigernet.backend.audit.entity.AuditLogEntity;
import com.tigernet.backend.audit.repository.AuditLogRepository;
import com.tigernet.backend.user.service.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class AuditLogService {
    AuditLogRepository auditLogRepository;
    CurrentUserService currentUserService;
    JsonMapper jsonMapper;

    // ghi 1 dòng nhật ký cho user đang đăng nhập; detail được lưu dạng JSON
    public void log(String action, String targetTable, Integer targetId, Object detail) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUser(currentUserService.getCurrentUser());
        auditLog.setAction(action);
        auditLog.setTargetTable(targetTable);
        auditLog.setTargetId(targetId);
        auditLog.setDetail(detail == null ? null : jsonMapper.writeValueAsString(detail));
        auditLog.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }

    // to là ngày cuối (tính cả ngày đó)
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs(Integer userId, String action, String targetTable,
                                               Integer targetId, LocalDate from, LocalDate to) {
        return auditLogRepository.search(userId, action, targetTable, targetId,
                        from != null ? from.atStartOfDay() : null,
                        to != null ? to.plusDays(1).atStartOfDay() : null)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditLogResponse toResponse(AuditLogEntity auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUser() != null ? auditLog.getUser().getUserId() : null)
                .username(auditLog.getUser() != null ? auditLog.getUser().getUsername() : null)
                .action(auditLog.getAction())
                .targetTable(auditLog.getTargetTable())
                .targetId(auditLog.getTargetId())
                .detail(auditLog.getDetail())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
