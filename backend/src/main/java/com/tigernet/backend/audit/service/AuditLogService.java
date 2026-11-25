package com.tigernet.backend.audit.service;

import com.tigernet.backend.audit.dto.response.AuditLogResponse;
import com.tigernet.backend.audit.entity.AuditLogEntity;
import com.tigernet.backend.audit.repository.AuditLogRepository;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.user.service.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
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

    // request đã có dòng nhật ký nghiệp vụ thì AuditInterceptor không ghi thêm dòng chung
    public static final String LOGGED_ATTRIBUTE = AuditLogService.class.getName() + ".logged";

    // ghi 1 dòng nhật ký cho user đang đăng nhập; detail được lưu dạng JSON
    public void log(String action, String targetTable, Integer targetId, Object detail) {
        write(currentUserService.getCurrentUser(), action, targetTable, targetId, null, detail);
    }

    // như log(), kèm số giấy tờ của đối tượng (số phiếu nhập/xuất/chuyển kho, số phiếu thu/chi...)
    public void logDocument(String action, String targetTable, Integer targetId, String targetCode, Object detail) {
        write(currentUserService.getCurrentUser(), action, targetTable, targetId, targetCode, detail);
    }

    // dùng khi chưa có token trong SecurityContext (vd đăng nhập)
    public void log(UserEntity user, String action, String targetTable, Integer targetId, Object detail) {
        write(user, action, targetTable, targetId, null, detail);
    }

    private void write(UserEntity user, String action, String targetTable, Integer targetId, String targetCode,
                       Object detail) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUser(user);
        auditLog.setAction(action);
        auditLog.setTargetTable(targetTable);
        auditLog.setTargetId(targetId);
        auditLog.setTargetCode(targetCode == null || targetCode.length() <= 50 ? targetCode : targetCode.substring(0, 50));
        auditLog.setDetail(detail == null ? null : jsonMapper.writeValueAsString(detail));
        auditLog.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(auditLog);
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            attributes.getRequest().setAttribute(LOGGED_ATTRIBUTE, Boolean.TRUE);
        }
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
                .targetCode(auditLog.getTargetCode())
                .detail(auditLog.getDetail())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
