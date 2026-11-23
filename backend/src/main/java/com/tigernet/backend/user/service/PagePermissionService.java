package com.tigernet.backend.user.service;

import com.tigernet.backend.audit.service.AuditLogService;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.security.Pages;
import com.tigernet.backend.user.dto.response.PagePermissionResponse;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.user.entity.UserPagePermissionEntity;
import com.tigernet.backend.user.repository.UserPagePermissionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Quyền xem trang của từng nhân viên (Quản trị chỉnh trên màn "Phân quyền"). */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class PagePermissionService {
    // đường dẫn trang FE: /app/... chỉ gồm chữ thường, số, '-' và '/'
    private static final Pattern PAGE_KEY = Pattern.compile("^/app(/[a-z0-9-]+)+$");

    UserPagePermissionRepository repository;
    UserService userService;
    AuditLogService auditLogService;
    PageAccessPolicy pageAccessPolicy;

    @Transactional(readOnly = true)
    public PagePermissionResponse get(Integer userId) {
        List<String> pages = repository.findByUserIdOrderByPageKey(userId).stream()
                .map(UserPagePermissionEntity::getPageKey)
                .toList();
        return new PagePermissionResponse(userId, !pages.isEmpty(), pages, effectivePages(userId));
    }

    private List<String> effectivePages(Integer userId) {
        UserEntity user = userService.findUserById(userId);
        String role = user.getRole() != null ? user.getRole().getRoleName() : null;
        return List.copyOf(pageAccessPolicy.effectivePages(userId, role));
    }

    public PagePermissionResponse set(Integer userId, Collection<String> pages) {
        userService.findUserById(userId);
        TreeSet<String> keys = new TreeSet<>();
        for (String page : pages) {
            // chỉ nhận trang có thật trong ma trận quyền (PageAccessPolicy), không lưu key lạ
            if (page == null || page.length() > 100 || !PAGE_KEY.matcher(page.trim()).matches()
                    || !PageAccessPolicy.allPages().contains(page.trim())) {
                throw new AppException(ErrorCode.INVALID_PAGE_KEY);
            }
            keys.add(page.trim());
        }
        // luôn giữ Tổng quan để danh sách không rỗng (rỗng = quay về quyền mặc định theo vai trò)
        keys.add(Pages.DASHBOARD);
        repository.deleteByUserId(userId);
        repository.saveAll(keys.stream().map(key -> {
            UserPagePermissionEntity entity = new UserPagePermissionEntity();
            entity.setUserId(userId);
            entity.setPageKey(key);
            return entity;
        }).toList());
        auditLogService.log("SET_PERMISSIONS", "Users", userId, Map.of("pages", keys));
        return new PagePermissionResponse(userId, true, List.copyOf(keys), effectivePages(userId));
    }

    // xóa phân quyền riêng: nhân viên dùng lại quyền mặc định theo vai trò
    public PagePermissionResponse reset(Integer userId) {
        userService.findUserById(userId);
        repository.deleteByUserId(userId);
        auditLogService.log("RESET_PERMISSIONS", "Users", userId, null);
        return new PagePermissionResponse(userId, false, List.of(), effectivePages(userId));
    }
}
