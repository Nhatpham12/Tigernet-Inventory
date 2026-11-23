package com.tigernet.backend.user.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.security.SecurityUtils;
import com.tigernet.backend.user.entity.UserPagePermissionEntity;
import com.tigernet.backend.user.repository.UserPagePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.tigernet.backend.common.security.Pages.*;

/**
 * Quyền vào trang của nhân viên, dùng chung cho kiểm tra API (AccessInterceptor) và cho FE (effectivePages).
 * - Quản trị: mọi trang.
 * - Có phân quyền riêng (User_Page_Permissions): đúng danh sách đó, có thể rộng hoặc hẹp hơn mặc định của vai trò.
 * - Chưa có: mặc định theo vai trò (ma trận Frontend_Structure.md §3, khớp menu.dart).
 * Menu Quản trị (nhân viên, phân quyền, nhật ký, cài đặt) luôn theo vai trò, không phân quyền riêng được.
 */
@Service
@RequiredArgsConstructor
public class PageAccessPolicy {
    public static final String ADMIN = "ADMIN";
    public static final String MANAGER = "MANAGER";
    public static final String WAREHOUSE = "WAREHOUSE";
    public static final String ACCOUNTANT = "ACCOUNTANT";
    public static final String SALES = "SALES";

    private static final Set<String> ADMIN_MENU = Set.of(USERS, PERMISSIONS, AUDIT_LOGS, SETTINGS);

    /** trang → các vai trò mặc định được vào (ADMIN luôn được nên không ghi) */
    private static final Map<String, Set<String>> ROLE_DEFAULTS = Map.ofEntries(
            Map.entry(DASHBOARD, roles(WAREHOUSE, ACCOUNTANT, SALES, MANAGER)),
            Map.entry(PRODUCTS, roles(WAREHOUSE, ACCOUNTANT, SALES, MANAGER)),
            Map.entry(PRODUCT_GROUPS, roles(WAREHOUSE, MANAGER)),
            Map.entry(PRODUCTS_NEW, roles(WAREHOUSE, MANAGER)),
            Map.entry(IMPORTS, roles(WAREHOUSE, ACCOUNTANT, MANAGER)),
            Map.entry(IMPORTS_NEW, roles(WAREHOUSE, MANAGER)),
            Map.entry(SUPPLIERS, roles(WAREHOUSE, ACCOUNTANT, MANAGER)),
            Map.entry(SUPPLIERS_NEW, roles(WAREHOUSE, MANAGER)),
            Map.entry(EXPORTS, roles(WAREHOUSE, ACCOUNTANT, SALES, MANAGER)),
            Map.entry(EXPORTS_NEW, roles(SALES, MANAGER)),
            Map.entry(CUSTOMERS, roles(SALES, MANAGER, ACCOUNTANT)),
            Map.entry(SALES_RETURNS, roles(WAREHOUSE, SALES, MANAGER)),
            Map.entry(TRANSFERS, roles(WAREHOUSE, MANAGER)),
            Map.entry(RENTALS, roles(WAREHOUSE, SALES, MANAGER)),
            Map.entry(RENTALS_NEW, roles(SALES, MANAGER)),
            Map.entry(RETURN_RECEIPTS, roles(WAREHOUSE, SALES, MANAGER)),
            Map.entry(LENDS, roles(WAREHOUSE, SALES, MANAGER)),
            Map.entry(LENDS_NEW, roles(SALES, MANAGER)),
            Map.entry(WARRANTY, roles(WAREHOUSE, SALES, MANAGER)),
            Map.entry(WARRANTY_NEW, roles(WAREHOUSE, SALES, MANAGER)),
            Map.entry(WAREHOUSES, roles(MANAGER)),
            Map.entry(STOCKTAKES, roles(WAREHOUSE, MANAGER)),
            Map.entry(PAYMENTS, roles(ACCOUNTANT, SALES, MANAGER)),
            Map.entry(PAYMENTS_NEW_IN, roles(ACCOUNTANT, MANAGER)),
            Map.entry(PAYMENTS_NEW_OUT, roles(ACCOUNTANT, MANAGER)),
            Map.entry(REPORT_STOCK, roles(WAREHOUSE, ACCOUNTANT, MANAGER)),
            Map.entry(REPORT_MOVEMENTS, roles(ACCOUNTANT, MANAGER)),
            Map.entry(REPORT_RECEIVABLES, roles(ACCOUNTANT, MANAGER)),
            Map.entry(REPORT_RENTALS, roles(MANAGER)),
            Map.entry(REPORT_RECONCILIATION, roles(ACCOUNTANT, MANAGER)),
            Map.entry(USERS, roles(MANAGER)),
            Map.entry(PERMISSIONS, roles()),
            Map.entry(AUDIT_LOGS, roles()),
            Map.entry(SETTINGS, roles(WAREHOUSE, ACCOUNTANT, SALES, MANAGER)));

    private final UserPagePermissionRepository pagePermissionRepository;

    /** mọi page key hợp lệ */
    public static Set<String> allPages() {
        return ROLE_DEFAULTS.keySet();
    }

    /** trang vai trò được vào khi chưa phân quyền riêng */
    public static Set<String> roleDefaults(String role) {
        if (ADMIN.equals(role)) {
            return allPages();
        }
        return ROLE_DEFAULTS.entrySet().stream()
                .filter(e -> e.getValue().contains(role))
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** trang user thực sự được vào (sắp xếp để trả về FE ổn định) */
    @Transactional(readOnly = true)
    public Set<String> effectivePages(Integer userId, String role) {
        if (ADMIN.equals(role)) {
            return new TreeSet<>(allPages());
        }
        Set<String> defaults = roleDefaults(role);
        List<String> custom = pagePermissionRepository.findByUserIdOrderByPageKey(userId).stream()
                .map(UserPagePermissionEntity::getPageKey)
                .toList();
        if (custom.isEmpty()) {
            return new TreeSet<>(defaults);
        }
        TreeSet<String> pages = new TreeSet<>();
        custom.stream().filter(ROLE_DEFAULTS::containsKey).filter(p -> !ADMIN_MENU.contains(p)).forEach(pages::add);
        pages.add(DASHBOARD);
        defaults.stream().filter(ADMIN_MENU::contains).forEach(pages::add);
        return pages;
    }

    /** user đang gọi API có được vào ít nhất một trang trong [pages] không */
    public boolean currentUserCanOpenAny(String... pages) {
        String role = SecurityUtils.currentRole();
        if (ADMIN.equals(role)) {
            return true;
        }
        Set<String> allowed = effectivePages(SecurityUtils.currentUserId(), role);
        return Arrays.stream(pages).anyMatch(allowed::contains);
    }

    /** ném 403 nếu user đang gọi API không được vào trang nào trong [pages] */
    public void requireAnyPage(String... pages) {
        if (!currentUserCanOpenAny(pages)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    private static Set<String> roles(String... roles) {
        return Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(roles)));
    }
}
