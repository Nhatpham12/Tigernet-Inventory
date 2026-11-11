package com.tigernet.backend.audit.config;

import com.tigernet.backend.audit.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Ghi nhật ký cho mọi thao tác ghi (POST/PUT/PATCH/DELETE) thành công của nhân viên đã đăng nhập,
 * để trang "Lịch sử thao tác" thấy được ai đã làm gì. Thao tác nào service đã tự ghi nhật ký chi tiết
 * (hủy phiếu, thanh toán, quản lý user...) thì bỏ qua để không trùng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final AuditLogService auditLogService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String method = request.getMethod();
        int status = response.getStatus();
        if (!WRITE_METHODS.contains(method) || ex != null || status < 200 || status >= 300
                || request.getAttribute(AuditLogService.LOGGED_ATTRIBUTE) != null
                || !(SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof Jwt)) {
            return;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/auth/")) {
            return;
        }
        String[] segments = path.replaceFirst("^/", "").split("/");
        Integer targetId = null;
        String subAction = null;
        for (int i = 1; i < segments.length; i++) {
            if (targetId == null && segments[i].matches("\\d{1,9}")) {
                targetId = Integer.valueOf(segments[i]);
            } else if (targetId != null && !segments[i].matches("\\d+")) {
                subAction = segments[i]; // vd /export-receipts/5/confirm → CONFIRM
            }
        }
        String action = subAction != null
                ? subAction.toUpperCase().replace('-', '_')
                : switch (method) {
                    case "POST" -> "CREATE";
                    case "DELETE" -> "DELETE";
                    default -> "UPDATE";
                };
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("method", method);
        detail.put("path", path);
        try {
            auditLogService.log(action.length() > 50 ? action.substring(0, 50) : action,
                    segments[0].length() > 50 ? segments[0].substring(0, 50) : segments[0], targetId, detail);
        } catch (RuntimeException e) {
            // không để lỗi ghi nhật ký làm hỏng thao tác chính (đã xong)
            log.warn("Cannot write audit log for {} {}: {}", method, path, e.getMessage());
        }
    }
}
