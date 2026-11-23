package com.tigernet.backend.auth.service;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.tigernet.backend.auth.entity.RecoveryCodeEntity;
import com.tigernet.backend.auth.repository.RecoveryCodeRepository;
import com.tigernet.backend.user.entity.RoleEntity;
import com.tigernet.backend.user.entity.UserEntity;
import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MfaServiceTest {
    RecoveryCodeRepository repository = mock(RecoveryCodeRepository.class);
    MfaService mfa = new MfaService("test-only-mfa-key", repository, new BCryptPasswordEncoder(4));

    private static String codeAt(String secret, Instant time) throws Exception {
        TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator();
        return totp.generateOneTimePasswordString(
                new SecretKeySpec(new Base32().decode(secret), totp.getAlgorithm()), time);
    }

    @Test
    void acceptsCurrentAndAdjacentStepOnly() throws Exception {
        String secret = mfa.newSecret();
        Instant now = Instant.parse("2026-11-23T10:00:00Z");
        assertTrue(mfa.verifyCode(secret, codeAt(secret, now), now));
        assertTrue(mfa.verifyCode(secret, codeAt(secret, now.minusSeconds(30)), now)); // giờ điện thoại lệch
        assertFalse(mfa.verifyCode(secret, codeAt(secret, now.minusSeconds(120)), now));
        assertFalse(mfa.verifyCode(secret, "abcdef", now));
        assertFalse(mfa.verifyCode(secret, null, now));
    }

    @Test
    void secretIsEncryptedAtRestAndRoundTrips() {
        String secret = mfa.newSecret();
        String stored = mfa.encrypt(secret);
        assertNotEquals(secret, stored);
        assertFalse(stored.contains(secret));
        assertEquals(secret, mfa.decrypt(stored));
    }

    @Test
    void recoveryCodesAreHashedAndSingleUse() {
        List<RecoveryCodeEntity> stored = new ArrayList<>();
        doAnswer(inv -> {
            stored.addAll(inv.getArgument(0));
            return inv.getArgument(0);
        }).when(repository).saveAll(anyList());
        when(repository.findByUserId(7)).thenAnswer(inv -> List.copyOf(stored));
        doAnswer(inv -> stored.remove((RecoveryCodeEntity) inv.getArgument(0))).when(repository).delete(any());

        List<String> codes = mfa.regenerateRecoveryCodes(7);
        assertEquals(8, codes.size());
        assertTrue(stored.stream().noneMatch(r -> codes.contains(r.getCodeHash())));
        assertTrue(mfa.useRecoveryCode(7, codes.getFirst().toLowerCase()));
        assertFalse(mfa.useRecoveryCode(7, codes.getFirst())); // đã dùng
        assertEquals(7, stored.size());
    }

    @Test
    void requiredOnlyForAdminAndOnlyWhenKeyConfigured() {
        UserEntity admin = new UserEntity();
        RoleEntity role = new RoleEntity();
        role.setRoleName("ADMIN");
        admin.setRole(role);
        assertTrue(mfa.requiredFor(admin));
        role.setRoleName("MANAGER");
        assertFalse(mfa.requiredFor(admin));
        role.setRoleName("ADMIN");
        assertFalse(new MfaService("", repository, new BCryptPasswordEncoder(4)).requiredFor(admin));
    }

    @Test
    void otpauthUriEscapesUsername() {
        String uri = MfaService.otpauthUri("quản trị", "ABC");
        assertTrue(uri.startsWith("otpauth://totp/TigerNet%3Aqu"), uri);
        assertFalse(uri.contains(" "));
        assertTrue(uri.contains("secret=ABC&issuer=TigerNet"));
    }
}
