package com.tigernet.backend.warehouse.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.user.service.UserService;
import com.tigernet.backend.warehouse.mapper.WarehouseMapper;
import com.tigernet.backend.warehouse.repository.UserWarehouseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserWarehouseServiceTest {
    @Mock UserWarehouseRepository userWarehouseRepository;
    @Mock WarehouseService warehouseService;
    @Mock UserService userService;
    @Mock WarehouseMapper warehouseMapper;
    @InjectMocks UserWarehouseService service;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(int userId, String role) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS512").claim("userId", userId).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("SCOPE_" + role))));
    }

    @Test
    void staffCannotReadOtherUsersWarehouses() {
        loginAs(5, "WAREHOUSE");
        AppException e = assertThrows(AppException.class, () -> service.getUserWarehouses(7));
        assertEquals(ErrorCode.ACCESS_DENIED, e.getErrorCode());
        verify(userService, never()).findUserById(7);
    }

    @Test
    void staffCanReadOwnWarehouses() {
        loginAs(5, "SALES");
        assertDoesNotThrow(() -> service.getUserWarehouses(5));
    }

    @Test
    void managerAndAdminCanReadOthers() {
        loginAs(1, "MANAGER");
        assertDoesNotThrow(() -> service.getUserWarehouses(7));
        loginAs(1, "ADMIN");
        assertDoesNotThrow(() -> service.getUserWarehouses(7));
    }
}
