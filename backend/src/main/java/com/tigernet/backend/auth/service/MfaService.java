package com.tigernet.backend.auth.service;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.tigernet.backend.auth.entity.RecoveryCodeEntity;
import com.tigernet.backend.auth.repository.RecoveryCodeRepository;
import com.tigernet.backend.user.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Xác thực 2 bước (TOTP, RFC 6238: mã 6 số đổi mỗi 30 giây trên Google Authenticator / Microsoft Authenticator)
 * cho tài khoản Quản trị. Thuật toán do thư viện java-otp làm, không tự viết.
 * - Khoá TOTP lưu trong Users.Mfa_Secret đã mã hoá AES-GCM (Spring Security Encryptors) bằng MFA_ENC_KEY.
 * - MFA_ENC_KEY để trống thì tắt MFA (máy dev); prod bắt buộc đặt (xem tài liệu deploy).
 * - 8 mã khôi phục dùng một lần, chỉ lưu hash BCrypt.
 */
@Slf4j
@Service
public class MfaService {
    static final String ISSUER = "TigerNet";
    private static final int RECOVERY_CODES = 8;
    private static final Base32 BASE32 = new Base32();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TextEncryptor encryptor;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator();

    public MfaService(@Value("${tigernet.mfa.encryption-key:}") String encryptionKey,
                      RecoveryCodeRepository recoveryCodeRepository, PasswordEncoder passwordEncoder) {
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.passwordEncoder = passwordEncoder;
        if (encryptionKey == null || encryptionKey.isBlank()) {
            this.encryptor = null;
            log.warn("MFA_ENC_KEY chưa đặt: TẮT xác thực 2 bước cho Quản trị (chỉ chấp nhận trên máy dev)");
        } else {
            // salt cố định theo khoá (không cần bí mật); mỗi lần mã hoá vẫn có IV ngẫu nhiên riêng
            String salt = HexFormat.of().formatHex(DigestUtils.sha256(encryptionKey), 0, 16);
            this.encryptor = Encryptors.delux(encryptionKey, salt);
        }
    }

    /** user này phải qua bước nhập mã 6 số khi đăng nhập */
    public boolean requiredFor(UserEntity user) {
        return encryptor != null && user.getRole() != null && "ADMIN".equals(user.getRole().getRoleName());
    }

    /** tạo khoá TOTP mới (160 bit), trả dạng Base32 để hiện trên app Authenticator */
    public String newSecret() {
        byte[] key = new byte[20];
        RANDOM.nextBytes(key);
        return BASE32.encodeToString(key).replace("=", "");
    }

    public String encrypt(String base32Secret) {
        return encryptor.encrypt(base32Secret);
    }

    public String decrypt(String encryptedSecret) {
        return encryptor.decrypt(encryptedSecret);
    }

    /** link quét QR: otpauth://totp/TigerNet:<username>?secret=...&issuer=TigerNet */
    public static String otpauthUri(String username, String base32Secret) {
        String label = URLEncoder.encode(ISSUER + ":" + username, StandardCharsets.UTF_8).replace("+", "%20");
        return "otpauth://totp/" + label + "?secret=" + base32Secret + "&issuer=" + ISSUER
                + "&algorithm=SHA1&digits=6&period=30";
    }

    /** mã 6 số đúng với khoá; chấp nhận lệch ±1 bước (30 giây) do giờ điện thoại */
    public boolean verifyCode(String base32Secret, String code, Instant now) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        SecretKeySpec key = new SecretKeySpec(BASE32.decode(base32Secret), totp.getAlgorithm());
        Duration step = totp.getTimeStep();
        try {
            for (int offset = -1; offset <= 1; offset++) {
                String expected = totp.generateOneTimePasswordString(key, now.plus(step.multipliedBy(offset)));
                if (java.security.MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                        code.getBytes(StandardCharsets.US_ASCII))) {
                    return true;
                }
            }
        } catch (InvalidKeyException e) {
            log.error("Khoá TOTP không hợp lệ cho thuật toán {}", totp.getAlgorithm());
        }
        return false;
    }

    /** tạo lại 8 mã khôi phục (xoá mã cũ); trả mã gốc để hiện cho người dùng đúng 1 lần */
    public List<String> regenerateRecoveryCodes(Integer userId) {
        recoveryCodeRepository.deleteByUserId(userId);
        List<String> codes = new ArrayList<>();
        List<RecoveryCodeEntity> rows = new ArrayList<>();
        for (int i = 0; i < RECOVERY_CODES; i++) {
            byte[] raw = new byte[5];
            RANDOM.nextBytes(raw);
            String code = BASE32.encodeToString(raw); // 8 ký tự A-Z2-7
            codes.add(code.substring(0, 4) + "-" + code.substring(4));
            RecoveryCodeEntity row = new RecoveryCodeEntity();
            row.setUserId(userId);
            row.setCodeHash(passwordEncoder.encode(code));
            rows.add(row);
        }
        recoveryCodeRepository.saveAll(rows);
        return codes;
    }

    /** mã khôi phục đúng thì xoá (dùng một lần) và trả true */
    public boolean useRecoveryCode(Integer userId, String input) {
        if (input == null) {
            return false;
        }
        String normalized = input.replace("-", "").replace(" ", "").toUpperCase();
        if (!normalized.matches("[A-Z2-7]{8}")) {
            return false;
        }
        for (RecoveryCodeEntity row : recoveryCodeRepository.findByUserId(userId)) {
            if (passwordEncoder.matches(normalized, row.getCodeHash())) {
                recoveryCodeRepository.delete(row);
                return true;
            }
        }
        return false;
    }

    /** Quản trị tắt MFA của một tài khoản (mất điện thoại và hết mã khôi phục) */
    public void reset(UserEntity user) {
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        recoveryCodeRepository.deleteByUserId(user.getUserId());
    }
}
