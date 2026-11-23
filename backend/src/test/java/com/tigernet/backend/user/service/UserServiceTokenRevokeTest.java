package com.tigernet.backend.user.service;

import com.tigernet.backend.audit.service.AuditLogService;
import com.tigernet.backend.auth.repository.LoginFailureRepository;
import com.tigernet.backend.user.dto.request.UserUpdateRequest;
import com.tigernet.backend.user.entity.RoleEntity;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.user.mapper.UserMapper;
import com.tigernet.backend.user.repository.RoleRepository;
import com.tigernet.backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Đổi mật khẩu / vai trò / khoá tài khoản phải làm mọi token cũ mất hiệu lực (tăng Token_Version). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTokenRevokeTest {
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock AuditLogService auditLogService;
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock LoginFailureRepository loginFailureRepository;
    @InjectMocks UserService service;

    UserEntity user;
    RoleEntity warehouse;
    RoleEntity manager;

    @BeforeEach
    void setUp() {
        warehouse = role(3, "WAREHOUSE");
        manager = role(4, "MANAGER");
        user = new UserEntity();
        user.setUserId(7);
        user.setUsername("kho1");
        user.setIsActive(true);
        user.setRole(warehouse);
        when(userRepository.findById(7)).thenReturn(Optional.of(user));
        when(roleRepository.findById(4)).thenReturn(Optional.of(manager));
        when(roleRepository.findById(3)).thenReturn(Optional.of(warehouse));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // mapper giả: chép isActive như MapStruct (bỏ qua null)
        org.mockito.Mockito.doAnswer(inv -> {
            UserUpdateRequest r = inv.getArgument(1);
            if (r.getIsActive() != null) user.setIsActive(r.getIsActive());
            return null;
        }).when(userMapper).updateUser(any(), any());
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS512").claim("userId", 1).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("SCOPE_ADMIN"))));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static RoleEntity role(int id, String name) {
        RoleEntity r = new RoleEntity();
        r.setId(id);
        r.setRoleName(name);
        return r;
    }

    @Test
    void roleChangeRevokesTokens() {
        service.updateUser(7, UserUpdateRequest.builder().roleId(4).build());
        assertEquals(1, user.getTokenVersion());
    }

    @Test
    void passwordResetRevokesTokensAndUnlocks() {
        service.updateUser(7, UserUpdateRequest.builder().password("new-password").build());
        assertEquals(1, user.getTokenVersion());
        verify(loginFailureRepository).deleteByUserId(7); // mở khoá đăng nhập ở mọi IP
    }

    @Test
    void deactivateRevokesTokens() {
        service.updateUser(7, UserUpdateRequest.builder().isActive(false).build());
        assertEquals(1, user.getTokenVersion());
    }

    @Test
    void sameRoleAndNameOnlyKeepsTokens() {
        service.updateUser(7, UserUpdateRequest.builder().fullName("Kho 1").roleId(3).build());
        assertEquals(0, user.getTokenVersion());
    }
}
