package com.tigernet.backend.user.service;

import com.tigernet.backend.common.security.Pages;
import com.tigernet.backend.user.entity.UserPagePermissionEntity;
import com.tigernet.backend.user.repository.UserPagePermissionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PageAccessPolicyTest {
    UserPagePermissionRepository repository = mock(UserPagePermissionRepository.class);
    PageAccessPolicy policy = new PageAccessPolicy(repository);

    private static UserPagePermissionEntity row(String page) {
        UserPagePermissionEntity e = new UserPagePermissionEntity();
        e.setUserId(5);
        e.setPageKey(page);
        return e;
    }

    @Test
    void roleDefaultsFollowMenuMatrix() {
        when(repository.findByUserIdOrderByPageKey(5)).thenReturn(List.of());
        Set<String> sales = policy.effectivePages(5, "SALES");
        assertTrue(sales.contains(Pages.EXPORTS_NEW));
        assertTrue(sales.contains(Pages.PAYMENTS));
        assertFalse(sales.contains(Pages.PAYMENTS_NEW_IN));
        assertFalse(sales.contains(Pages.IMPORTS));
        assertFalse(sales.contains(Pages.USERS));

        Set<String> warehouse = policy.effectivePages(5, "WAREHOUSE");
        assertTrue(warehouse.contains(Pages.IMPORTS_NEW));
        assertFalse(warehouse.contains(Pages.WAREHOUSES));
        assertFalse(warehouse.contains(Pages.EXPORTS_NEW));
    }

    @Test
    void customListReplacesRoleDefaultButAdminMenuStaysByRole() {
        when(repository.findByUserIdOrderByPageKey(5)).thenReturn(List.of(row(Pages.DASHBOARD), row(Pages.WAREHOUSES),
                row(Pages.AUDIT_LOGS), row("/app/khong-ton-tai")));
        Set<String> pages = policy.effectivePages(5, "WAREHOUSE");
        assertTrue(pages.contains(Pages.WAREHOUSES));    // mở rộng ngoài vai trò
        assertFalse(pages.contains(Pages.IMPORTS_NEW));  // thu hẹp: không tick thì mất
        assertFalse(pages.contains(Pages.AUDIT_LOGS));   // menu Quản trị không cấp riêng được
        assertTrue(pages.contains(Pages.SETTINGS));      // menu Quản trị theo vai trò
        assertFalse(pages.contains("/app/khong-ton-tai"));
    }

    @Test
    void adminGetsAllAndUnknownRoleOnlyBasics() {
        assertEquals(PageAccessPolicy.allPages(), policy.effectivePages(1, "ADMIN"));
        when(repository.findByUserIdOrderByPageKey(9)).thenReturn(List.of());
        assertEquals(Set.of(), policy.effectivePages(9, "LA"));
    }
}
