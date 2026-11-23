package com.tigernet.backend.warehouse.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.warehouse.entity.UserWarehouseEntity;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import com.tigernet.backend.warehouse.repository.UserWarehouseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WarehouseAccessServiceTest {
    UserWarehouseRepository repository = mock(UserWarehouseRepository.class);
    WarehouseAccessService access = new WarehouseAccessService(repository);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    static void loginAs(int userId, String role) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS512").claim("userId", userId).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("SCOPE_" + role))));
    }

    static WarehouseEntity warehouse(int id) {
        WarehouseEntity w = new WarehouseEntity();
        w.setId(id);
        return w;
    }

    private void assign(int userId, int... warehouseIds) {
        List<UserWarehouseEntity> rows = java.util.Arrays.stream(warehouseIds).mapToObj(id -> {
            UserWarehouseEntity uw = new UserWarehouseEntity();
            uw.setWarehouse(warehouse(id));
            return uw;
        }).toList();
        when(repository.findByUserUserId(userId)).thenReturn(rows);
    }

    @Test
    void adminManagerAccountantSeeAllWarehouses() {
        for (String role : List.of("ADMIN", "MANAGER", "ACCOUNTANT")) {
            loginAs(1, role);
            assertTrue(access.all());
            assertNull(access.allowedWarehouseIds());
            assertDoesNotThrow(() -> access.assertAccess(99));
            assertDoesNotThrow(() -> access.assertAccess((Integer) null));
        }
    }

    @Test
    void storekeeperOnlyAssignedWarehouses() {
        loginAs(5, "WAREHOUSE");
        assign(5, 1, 3);
        assertEquals(Set.of(1, 3), access.allowedWarehouseIds());
        assertDoesNotThrow(() -> access.assertAccess(1));
        AppException e = assertThrows(AppException.class, () -> access.assertAccess(2));
        assertEquals(ErrorCode.ACCESS_DENIED, e.getErrorCode());
        assertThrows(AppException.class, () -> access.assertAccess((Integer) null)); // "mọi kho" không được
        assertDoesNotThrow(() -> access.assertAnyAccess(warehouse(2), warehouse(3)));
        assertThrows(AppException.class, () -> access.assertAnyAccess(warehouse(2), warehouse(4)));
    }

    @Test
    void noAssignmentGivesSentinelAndDeniesEverything() {
        loginAs(6, "SALES");
        assign(6);
        assertEquals(List.of(-1), List.copyOf(access.idsOrSentinel()));
        assertThrows(AppException.class, () -> access.assertAccess(1));
    }

    @Test
    void filterDropsOtherWarehousesOrChecksRequestedOne() {
        loginAs(5, "WAREHOUSE");
        assign(5, 1);
        List<WarehouseEntity> rows = List.of(warehouse(1), warehouse(2), warehouse(1));
        assertEquals(2, access.filter(rows, null, w -> w).size());
        assertThrows(AppException.class, () -> access.filter(rows, 2, w -> w));
    }
}
