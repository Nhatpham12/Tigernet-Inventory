package com.tigernet.backend.common.security;

import com.tigernet.backend.common.exception.GlobalExceptionHandler;
import com.tigernet.backend.user.repository.UserPagePermissionRepository;
import com.tigernet.backend.user.service.PageAccessPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Interceptor chạy thật qua MockMvc: bị chặn phải ra 403 + mã 1007 (không phải 500). */
class AccessInterceptorTest {

    // lớp lồng không-static: component scan (context thật, AccessAnnotationCoverageTest) bỏ qua
    @RestController
    class DummyController {
        @RequiresPage(Pages.WAREHOUSES)
        @PutMapping("/t/warehouses")
        String editWarehouse() {
            return "ok";
        }

        @RequiresPage(Pages.STOCKTAKES)
        @RequiresRole(PageAccessPolicy.MANAGER)
        @PutMapping("/t/stocktakes/complete")
        String completeStocktake() {
            return "ok";
        }

        @NoPageCheck("test")
        @GetMapping("/t/lookup")
        String lookup() {
            return "ok";
        }

        @GetMapping("/t/forgotten")
        String forgotten() {
            return "ok";
        }
    }

    UserPagePermissionRepository repository = mock(UserPagePermissionRepository.class);
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        when(repository.findByUserIdOrderByPageKey(anyInt())).thenReturn(List.of());
        AccessInterceptor interceptor = new AccessInterceptor(new PageAccessPolicy(repository));
        mvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .addInterceptors(interceptor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(String role) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS512").claim("userId", 5).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("SCOPE_" + role))));
    }

    private static com.tigernet.backend.user.entity.UserPagePermissionEntity row(String page) {
        var entity = new com.tigernet.backend.user.entity.UserPagePermissionEntity();
        entity.setUserId(5);
        entity.setPageKey(page);
        return entity;
    }

    @Test
    void roleWithoutPageGets403NotServerError() throws Exception {
        loginAs("WAREHOUSE");
        mvc.perform(put("/t/warehouses")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(1007));
    }

    @Test
    void customPageGrantExtendsRoleDefault() throws Exception {
        loginAs("WAREHOUSE");
        when(repository.findByUserIdOrderByPageKey(5)).thenReturn(List.of(row(Pages.WAREHOUSES)));
        mvc.perform(put("/t/warehouses")).andExpect(status().isOk());
    }

    @Test
    void managerOnlyActionNeedsRoleEvenWithPage() throws Exception {
        loginAs("WAREHOUSE"); // Thủ kho có trang Kiểm kê nhưng không được chốt
        mvc.perform(put("/t/stocktakes/complete")).andExpect(status().isForbidden());
        loginAs("MANAGER");
        mvc.perform(put("/t/stocktakes/complete")).andExpect(status().isOk());
    }

    @Test
    void adminPassesEverything() throws Exception {
        loginAs("ADMIN");
        mvc.perform(put("/t/warehouses")).andExpect(status().isOk());
        mvc.perform(put("/t/stocktakes/complete")).andExpect(status().isOk());
    }

    @Test
    void handlerWithoutAnnotationIsDenied() throws Exception {
        loginAs("ADMIN");
        mvc.perform(get("/t/forgotten")).andExpect(status().isForbidden());
        mvc.perform(get("/t/lookup")).andExpect(status().isOk());
    }
}
