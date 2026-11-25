package com.tigernet.backend.auth.service;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.tigernet.backend.auth.dto.request.AuthenticationRequest;
import com.tigernet.backend.auth.dto.response.AuthenticationResponse;
import com.tigernet.backend.user.entity.RoleEntity;
import com.tigernet.backend.user.entity.UserEntity;
import jakarta.persistence.EntityManager;
import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cài MFA lần đầu trên DB thật (MySQL như CI). Lỗi cũ: bước tạo mã khôi phục xoá bulk kèm clear
 * persistence context → user bị tách, cấp token đọc role (lazy) ném LazyInitializationException → HTTP 500.
 * Test chạy trong transaction và rollback, không để lại dữ liệu.
 */
@SpringBootTest(properties = "tigernet.mfa.encryption-key=test-only-mfa-key-0123456789abcdef")
@Transactional
class MfaConfirmIntegrationTest {

    // sinh ngẫu nhiên mỗi lần chạy, chỉ sống trong transaction của test
    private final String rawPassword = UUID.randomUUID().toString();

    @Autowired AuthenticationService authenticationService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EntityManager em;

    @Test
    void confirmFirstCodeIssuesTokensAndRecoveryCodes() throws Exception {
        RoleEntity admin = em.createQuery("select r from RoleEntity r where r.roleName = 'ADMIN'", RoleEntity.class)
                .getSingleResult();
        UserEntity user = new UserEntity();
        user.setUsername("mfa_it_" + System.nanoTime());
        user.setFullName("MFA integration test");
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(admin);
        em.persist(user);
        em.flush();
        em.clear(); // mỗi bước như một request mới: user nạp lại từ DB, role là proxy lazy

        AuthenticationResponse login = authenticationService.authenticate(
                new AuthenticationRequest(user.getUsername(), rawPassword), "127.0.0.1");
        assertTrue(login.isMfaSetupRequired());
        String mfaToken = login.getMfaToken();
        em.flush();
        em.clear();
        String secret = authenticationService.mfaSetup(mfaToken).secret();
        em.flush();
        em.clear();

        AuthenticationResponse response = authenticationService.mfaConfirm(mfaToken, currentCode(secret), "127.0.0.1");

        assertNotNull(response.getToken());
        assertEquals(8, response.getRecoveryCodes().size());
        em.flush();
        em.clear();
        assertTrue(em.find(UserEntity.class, user.getUserId()).isMfaEnabled());
    }

    private static String currentCode(String base32Secret) throws Exception {
        TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator();
        SecretKeySpec key = new SecretKeySpec(new Base32().decode(base32Secret), totp.getAlgorithm());
        return totp.generateOneTimePasswordString(key, Instant.now());
    }
}
