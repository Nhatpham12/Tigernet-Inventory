package com.tigernet.backend.auth.service;

import com.nimbusds.jwt.SignedJWT;
import com.tigernet.backend.audit.service.AuditLogService;
import com.tigernet.backend.auth.dto.request.AuthenticationRequest;
import com.tigernet.backend.auth.dto.response.AuthenticationResponse;
import com.tigernet.backend.auth.entity.LoginFailureEntity;
import com.tigernet.backend.auth.repository.LoginFailureRepository;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.security.SecurityConfig;
import com.tigernet.backend.user.entity.RoleEntity;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    private static final String KEY = "test-only-signer-key-test-only-signer-key-test-only-signer-key-0123";

    @Mock UserRepository userRepository;
    @Mock LoginFailureRepository loginFailureRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditLogService auditLogService;
    @InjectMocks AuthenticationService service;

    UserEntity user;
    // bảng Login_Failures giả trong bộ nhớ
    final Map<LoginFailureEntity.Key, LoginFailureEntity> failures = new HashMap<>();

    @BeforeEach
    void setUp() {
        lenient().when(loginFailureRepository.findById(any()))
                .thenAnswer(inv -> Optional.ofNullable(failures.get(inv.<LoginFailureEntity.Key>getArgument(0))));
        lenient().when(loginFailureRepository.save(any())).thenAnswer(inv -> {
            LoginFailureEntity f = inv.getArgument(0);
            failures.put(f.getId(), f);
            return f;
        });
        lenient().doAnswer(inv -> failures.remove(inv.<LoginFailureEntity>getArgument(0).getId()))
                .when(loginFailureRepository).delete(any());
        ReflectionTestUtils.setField(service, "signerKey", KEY);
        ReflectionTestUtils.setField(service, "validDuration", 3600L);
        ReflectionTestUtils.setField(service, "refreshDuration", 604800L);
        RoleEntity role = new RoleEntity();
        role.setId(1);
        role.setRoleName("WAREHOUSE");
        user = new UserEntity();
        user.setUserId(7);
        user.setUsername("kho1");
        user.setPasswordHash("hash");
        user.setIsActive(true);
        user.setRole(role);
    }

    private AuthenticationRequest req(String password) {
        return new AuthenticationRequest("kho1", password);
    }

    private void wrongPassword() {
        when(userRepository.findByUsername("kho1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);
    }

    private LoginFailureEntity failure(String ip) {
        return failures.get(new LoginFailureEntity.Key(7, ip));
    }

    private void lock(String ip, LocalDateTime until) {
        LoginFailureEntity f = new LoginFailureEntity();
        f.setId(new LoginFailureEntity.Key(7, ip));
        f.setLockedUntil(until);
        f.setUpdatedAt(LocalDateTime.now());
        failures.put(f.getId(), f);
    }

    @Test
    void wrongPasswordCountsAndLocksAfterFiveAttemptsFromSameIp() {
        wrongPassword();
        for (int i = 1; i <= 4; i++) {
            assertThrows(AppException.class, () -> service.authenticate(req("bad"), "1.2.3.4"));
            assertEquals(i, failure("1.2.3.4").getFailedCount());
            assertNull(failure("1.2.3.4").getLockedUntil());
        }
        assertThrows(AppException.class, () -> service.authenticate(req("bad"), "1.2.3.4"));
        assertTrue(failure("1.2.3.4").getLockedUntil().isAfter(LocalDateTime.now().plusMinutes(14)));
        assertEquals(0, failure("1.2.3.4").getFailedCount());
        verify(auditLogService, times(5)).log(eq(user), eq("LOGIN_FAILED"), any(), any(), any());
    }

    @Test
    void lockedIpRejectsEvenCorrectPasswordWithSameError() {
        lock("1.2.3.4", LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByUsername("kho1")).thenReturn(Optional.of(user));
        AppException e = assertThrows(AppException.class, () -> service.authenticate(req("good"), "1.2.3.4"));
        assertEquals(ErrorCode.UNAUTHENTICATED, e.getErrorCode());
        verify(passwordEncoder, never()).matches("good", "hash"); // không thử mật khẩu thật khi đang khoá
        verify(passwordEncoder).matches(eq("good"), anyString()); // nhưng vẫn so hash giả để thời gian như nhau
    }

    @Test
    void lockOnOneIpDoesNotBlockRealUserOnAnotherIp() {
        lock("6.6.6.6", LocalDateTime.now().plusMinutes(10)); // kẻ xấu cố tình khoá tài khoản
        when(userRepository.findByUsername("kho1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("good", "hash")).thenReturn(true);
        assertNotNull(service.authenticate(req("good"), "10.0.0.5").getToken());
        assertNotNull(failure("6.6.6.6")); // khoá của IP kia vẫn còn
    }

    @Test
    void expiredLockAndCorrectPasswordLogsInAndClearsRow() {
        lock("1.2.3.4", LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByUsername("kho1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("good", "hash")).thenReturn(true);
        AuthenticationResponse response = service.authenticate(req("good"), "1.2.3.4");
        assertNotNull(response.getToken());
        assertNull(failure("1.2.3.4"));
    }

    @Test
    void invalidIpHeaderIsGroupedAsUnknown() {
        assertEquals("unknown", AuthenticationService.normalizeIp("1.2.3.4, 5.6.7.8"));
        assertEquals("unknown", AuthenticationService.normalizeIp(null));
        assertEquals("::1", AuthenticationService.normalizeIp(" ::1 "));
    }

    @Test
    void unknownUserGetsSameErrorAsWrongPassword() {
        when(userRepository.findByUsername("kho1")).thenReturn(Optional.empty());
        AppException e = assertThrows(AppException.class, () -> service.authenticate(req("x"), "ip"));
        assertEquals(ErrorCode.UNAUTHENTICATED, e.getErrorCode());
        verify(passwordEncoder).matches(eq("x"), anyString()); // vẫn so hash giả để thời gian như nhau
    }

    @Test
    void refreshRejectedAfterLogoutBumpsTokenVersion() {
        when(userRepository.findByUsername("kho1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("good", "hash")).thenReturn(true);
        String refresh = service.authenticate(req("good"), "ip").getRefreshToken();

        when(userRepository.findById(7)).thenReturn(Optional.of(user));
        assertNotNull(service.refresh(refresh).getToken()); // còn hiệu lực

        service.logout(7);
        assertEquals(1, user.getTokenVersion());
        AppException e = assertThrows(AppException.class, () -> service.refresh(refresh));
        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, e.getErrorCode());
    }

    @Test
    void accessTokenValidatorRejectsOldVersionAndInactiveUser() throws Exception {
        when(userRepository.findByUsername("kho1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("good", "hash")).thenReturn(true);
        String access = service.authenticate(req("good"), "ip").getToken();
        var claims = SignedJWT.parse(access).getJWTClaimsSet();
        Jwt jwt = Jwt.withTokenValue(access).header("alg", "HS512")
                .claim("userId", claims.getClaim("userId"))
                .claim(AuthenticationService.TOKEN_VERSION_CLAIM, claims.getClaim(AuthenticationService.TOKEN_VERSION_CLAIM))
                .build();

        var validator = SecurityConfig.tokenVersionValidator(userRepository);
        when(userRepository.findActiveTokenVersion(7)).thenReturn(Optional.of(0));
        assertFalse(validator.validate(jwt).hasErrors());

        when(userRepository.findActiveTokenVersion(7)).thenReturn(Optional.of(1)); // đã đăng xuất / đổi mật khẩu
        assertTrue(validator.validate(jwt).hasErrors());

        when(userRepository.findActiveTokenVersion(7)).thenReturn(Optional.empty()); // bị khoá
        assertTrue(validator.validate(jwt).hasErrors());
    }

    @Test
    void safeLogValueStripsControlCharacters() {
        assertEquals("admin__FAKE_LINE", AuthenticationService.safeLogValue("admin\r\nFAKE LINE"));
        assertEquals("-", AuthenticationService.safeLogValue(null));
    }
}
