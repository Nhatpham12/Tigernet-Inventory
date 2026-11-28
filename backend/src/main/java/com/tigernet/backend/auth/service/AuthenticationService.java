package com.tigernet.backend.auth.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tigernet.backend.audit.service.AuditLogService;
import com.tigernet.backend.auth.dto.request.AuthenticationRequest;
import com.tigernet.backend.auth.dto.response.AuthenticationResponse;
import com.tigernet.backend.auth.entity.LoginFailureEntity;
import com.tigernet.backend.auth.repository.LoginFailureRepository;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.user.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)

public class AuthenticationService {
    // claim "typ" đánh dấu token không phải access token; SecurityConfig từ chối mọi token có "typ" khi gọi API
    public static final String TOKEN_TYPE_CLAIM = "typ";
    public static final String REFRESH_TOKEN_TYPE = "refresh";
    // token tạm giữa bước mật khẩu và bước mã 6 số của Quản trị
    public static final String MFA_TOKEN_TYPE = "mfa";
    static final long MFA_TOKEN_SECONDS = 300;
    public static final String ISSUER = "tigernet";
    // claim "ver" = Users.Token_Version lúc cấp token; SecurityConfig và refresh() so với giá trị hiện tại
    public static final String TOKEN_VERSION_CLAIM = "ver";

    static final int MAX_FAILED_LOGINS = 5;
    static final int LOCK_MINUTES = 15;
    // hash BCrypt (cùng cost với SecurityConfig) để so khi username không tồn tại, thời gian phản hồi như nhau
    private static final String DUMMY_HASH = new BCryptPasswordEncoder(10).encode(UUID.randomUUID().toString());

    UserRepository userRepository;
    LoginFailureRepository loginFailureRepository;
    PasswordEncoder passwordEncoder;
    AuditLogService auditLogService;
    MfaService mfaService;

    /** khoá Base32 (để gõ tay) + link otpauth:// (để quét QR) khi cài xác thực 2 bước */
    public record MfaSetupResponse(String secret, String otpauthUri) {
    }

    @NonFinal
    @Value("${jwt.signer-key}")
    String signerKey;

    @NonFinal
    @Value("${jwt.valid-duration}")
    long validDuration; // giây

    @NonFinal
    @Value("${jwt.refresh-duration:604800}")
    long refreshDuration; // giây, mặc định 7 ngày

    /**
     * Đăng nhập thành công thì trả token, thất bại thì ném AppException.
     * Sai mật khẩu {@value #MAX_FAILED_LOGINS} lần liên tiếp từ cùng một IP thì khoá cặp (tài khoản, IP)
     * {@value #LOCK_MINUTES} phút; người dùng thật ở IP khác vẫn đăng nhập được.
     * Sai username, sai mật khẩu, đang bị khoá đều trả cùng 1 lỗi để không lộ username nào tồn tại.
     * noRollbackFor: bộ đếm sai và nhật ký vẫn được lưu dù hàm ném lỗi.
     */
    @Transactional(noRollbackFor = AppException.class)
    public AuthenticationResponse authenticate(AuthenticationRequest request, String clientIp){
        String ip = normalizeIp(clientIp);
        var found = userRepository.findByUsername(request.getUsername());
        if(found.isEmpty()){
            passwordEncoder.matches(request.getPassword(), DUMMY_HASH); // tốn thời gian như user có thật
            logFailure(request.getUsername(), ip, "unknown-user");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        UserEntity user = found.get();
        LocalDateTime now = LocalDateTime.now();
        LoginFailureEntity failure = failureRow(user, ip);

        if(isLocked(failure, now)){
            // không kiểm tra mật khẩu thật khi đang khoá, nhưng vẫn tốn thời gian như nhau để không lộ trạng thái
            passwordEncoder.matches(request.getPassword(), DUMMY_HASH);
            logFailure(user.getUsername(), ip, "locked");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if(!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())){
            recordFailure(user, failure, now, "bad-password");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if(!Boolean.TRUE.equals(user.getIsActive())){
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        // Quản trị: đúng mật khẩu chưa đủ, phải qua bước mã 6 số. Giữ bộ đếm sai cho tới khi qua bước 2
        // để kẻ có mật khẩu không thử mã liên tục được.
        if(mfaService.requiredFor(user)){
            return AuthenticationResponse.builder()
                    .mfaRequired(user.isMfaEnabled())
                    .mfaSetupRequired(!user.isMfaEnabled())
                    .mfaToken(generateToken(user, MFA_TOKEN_TYPE, MFA_TOKEN_SECONDS))
                    .build();
        }
        return completeLogin(user, failure, null);
    }

    /** Quản trị chưa bật MFA: tạo khoá mới (lưu mã hoá, chưa bật) và trả link QR để quét bằng app Authenticator */
    @Transactional
    public MfaSetupResponse mfaSetup(String mfaToken){
        UserEntity user = userFromMfaToken(mfaToken);
        if(user.isMfaEnabled()){
            throw new AppException(ErrorCode.MFA_ALREADY_ENABLED);
        }
        String secret = mfaService.newSecret();
        user.setMfaSecret(mfaService.encrypt(secret));
        return new MfaSetupResponse(secret, MfaService.otpauthUri(user.getUsername(), secret));
    }

    /** nhập mã 6 số đầu tiên để bật MFA; thành công thì đăng nhập luôn và trả 8 mã khôi phục (hiện 1 lần) */
    @Transactional(noRollbackFor = AppException.class)
    public AuthenticationResponse mfaConfirm(String mfaToken, String code, String clientIp){
        UserEntity user = userFromMfaToken(mfaToken);
        if(user.isMfaEnabled()){
            throw new AppException(ErrorCode.MFA_ALREADY_ENABLED);
        }
        if(user.getMfaSecret() == null){
            throw new AppException(ErrorCode.MFA_NOT_SET_UP);
        }
        LoginFailureEntity failure = checkMfaAttempt(user, clientIp);
        if(!mfaService.verifyCode(mfaService.decrypt(user.getMfaSecret()), code, Instant.now())){
            recordFailure(user, failure, LocalDateTime.now(), "bad-mfa-code");
            throw new AppException(ErrorCode.MFA_CODE_INVALID);
        }
        user.setMfaEnabled(true);
        List<String> recoveryCodes = mfaService.regenerateRecoveryCodes(user.getUserId());
        auditLogService.log(user, "MFA_ENABLED", "Users", user.getUserId(), null);
        AuthenticationResponse response = completeLogin(user, failure, null);
        response.setRecoveryCodes(recoveryCodes);
        return response;
    }

    /** bước 2 khi đăng nhập Quản trị: mã 6 số từ app, hoặc một mã khôi phục */
    @Transactional(noRollbackFor = AppException.class)
    public AuthenticationResponse mfaVerify(String mfaToken, String code, String recoveryCode, String clientIp){
        UserEntity user = userFromMfaToken(mfaToken);
        if(!user.isMfaEnabled() || user.getMfaSecret() == null){
            throw new AppException(ErrorCode.MFA_NOT_SET_UP);
        }
        LoginFailureEntity failure = checkMfaAttempt(user, clientIp);
        boolean byRecovery = (code == null || code.isBlank()) && recoveryCode != null;
        boolean ok = byRecovery
                ? mfaService.useRecoveryCode(user.getUserId(), recoveryCode)
                : mfaService.verifyCode(mfaService.decrypt(user.getMfaSecret()), code, Instant.now());
        if(!ok){
            recordFailure(user, failure, LocalDateTime.now(), "bad-mfa-code");
            throw new AppException(ErrorCode.MFA_CODE_INVALID);
        }
        return completeLogin(user, failure, byRecovery ? Map.of("mfa", "recovery-code") : Map.of("mfa", "totp"));
    }

    private AuthenticationResponse completeLogin(UserEntity user, LoginFailureEntity failure, Object detail){
        if(failure.getUpdatedAt() != null){ // dòng đã lưu: đăng nhập được thì xoá bộ đếm sai
            loginFailureRepository.delete(failure);
        }
        AuthenticationResponse response = issueTokens(user);
        auditLogService.log(user, "LOGIN", "Users", user.getUserId(), detail);
        return response;
    }

    /** bộ đếm sai theo (tài khoản, IP); chưa sai lần nào thì trả dòng tạm chưa lưu (updatedAt = null) */
    private LoginFailureEntity failureRow(UserEntity user, String ip){
        LoginFailureEntity.Key key = new LoginFailureEntity.Key(user.getUserId(), ip);
        return loginFailureRepository.findById(key).orElseGet(() -> {
            LoginFailureEntity row = new LoginFailureEntity();
            row.setId(key);
            return row;
        });
    }

    private static boolean isLocked(LoginFailureEntity failure, LocalDateTime now){
        return failure.getLockedUntil() != null && failure.getLockedUntil().isAfter(now);
    }

    /** bước mã 6 số dùng chung bộ đếm sai với mật khẩu: đang khoá thì từ chối luôn */
    private LoginFailureEntity checkMfaAttempt(UserEntity user, String clientIp){
        LoginFailureEntity failure = failureRow(user, normalizeIp(clientIp));
        if(isLocked(failure, LocalDateTime.now())){
            logFailure(user.getUsername(), failure.getId().getIp(), "locked");
            throw new AppException(ErrorCode.MFA_CODE_INVALID);
        }
        return failure;
    }

    /** mfaToken (5 phút, chỉ dùng cho /auth/mfa/**) → user còn hoạt động, token chưa bị thu hồi */
    private UserEntity userFromMfaToken(String mfaToken){
        JWTClaimsSet claims;
        try {
            claims = verify(mfaToken);
        } catch (AppException e) {
            throw new AppException(ErrorCode.MFA_TOKEN_INVALID);
        }
        if(!MFA_TOKEN_TYPE.equals(claims.getClaim(TOKEN_TYPE_CLAIM))
                || !(claims.getClaim("userId") instanceof Number userId)){
            throw new AppException(ErrorCode.MFA_TOKEN_INVALID);
        }
        UserEntity user = userRepository.findById(userId.intValue())
                .orElseThrow(() -> new AppException(ErrorCode.MFA_TOKEN_INVALID));
        if(!Boolean.TRUE.equals(user.getIsActive())
                || !(claims.getClaim(TOKEN_VERSION_CLAIM) instanceof Number ver) || ver.intValue() != user.getTokenVersion()){
            throw new AppException(ErrorCode.MFA_TOKEN_INVALID);
        }
        return user;
    }

    // đăng xuất mọi thiết bị: tăng Token_Version, mọi access/refresh token cũ của user mất hiệu lực
    @Transactional
    public void logout(Integer userId){
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.AUTHENTICATION_REQUIRED));
        user.revokeTokens();
        auditLogService.log(user, "LOGOUT", "Users", userId, null);
    }

    /** sai mật khẩu hoặc sai mã 6 số: tăng bộ đếm, đủ {@value #MAX_FAILED_LOGINS} lần thì khoá */
    private void recordFailure(UserEntity user, LoginFailureEntity failure, LocalDateTime now, String reason){
        if(failure.getUpdatedAt() == null){
            // dòng mới: dọn luôn các dòng không đổi trong 1 ngày để bảng không phình
            loginFailureRepository.deleteOlderThan(now.minusDays(1));
        }
        int failed = failure.getFailedCount() + 1;
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("ip", failure.getId().getIp());
        detail.put("reason", reason);
        if(failed >= MAX_FAILED_LOGINS){
            failure.setFailedCount(0);
            failure.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
            detail.put("lockedMinutes", LOCK_MINUTES);
        } else {
            failure.setFailedCount(failed);
            detail.put("failedCount", failed);
        }
        failure.setUpdatedAt(now);
        loginFailureRepository.save(failure);
        auditLogService.log(user, "LOGIN_FAILED", "Users", user.getUserId(), detail);
        logFailure(user.getUsername(), failure.getId().getIp(), failed >= MAX_FAILED_LOGINS ? "locked-now" : reason);
    }

    // chỉ nhận chuỗi giống IPv4/IPv6 (header do BFF gửi); khác thì gom chung vào "unknown"
    static String normalizeIp(String ip){
        if(ip == null){
            return "unknown";
        }
        String trimmed = ip.trim();
        return trimmed.matches("[0-9A-Fa-f:.]{2,45}") ? trimmed : "unknown";
    }

    // dòng log cố định định dạng để fail2ban đọc được; username do client gửi nên lọc ký tự lạ (chống log injection)
    private static void logFailure(String username, String clientIp, String reason){
        log.warn("LOGIN_FAILED ip={} user={} reason={}", safeLogValue(clientIp), safeLogValue(username), reason);
    }

    static String safeLogValue(String value){
        if(value == null){
            return "-";
        }
        String cleaned = value.replaceAll("[^A-Za-z0-9._@:-]", "_");
        return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
    }

    // refresh token hợp lệ + user còn hoạt động thì cấp cặp token mới (xoay vòng refresh token)
    @Transactional(readOnly = true)
    public AuthenticationResponse refresh(String refreshToken){
        JWTClaimsSet claims = verify(refreshToken);
        if(!REFRESH_TOKEN_TYPE.equals(claims.getClaim(TOKEN_TYPE_CLAIM))){
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        Object userId = claims.getClaim("userId");
        if(!(userId instanceof Number number)){
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        UserEntity user = userRepository.findById(number.intValue())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_TOKEN));
        if(!Boolean.TRUE.equals(user.getIsActive())){
            throw new AppException(ErrorCode.USER_INACTIVE);
        }
        // đã đăng xuất / bị buộc đăng xuất / đổi vai trò / bị khoá sau khi token này được cấp
        if(!(claims.getClaim(TOKEN_VERSION_CLAIM) instanceof Number ver) || ver.intValue() != user.getTokenVersion()){
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return issueTokens(user);
    }

    private AuthenticationResponse issueTokens(UserEntity user){
        return AuthenticationResponse.builder()
                .token(generateToken(user, null, validDuration))
                .refreshToken(generateToken(user, REFRESH_TOKEN_TYPE, refreshDuration))
                .expiresIn(validDuration)
                .authenticated(true)
                .build();
    }

    private JWTClaimsSet verify(String token){
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if(!JWSAlgorithm.HS512.equals(jwt.getHeader().getAlgorithm())
                    || !jwt.verify(new MACVerifier(signerKey.getBytes(StandardCharsets.UTF_8)))){
                throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if(!ISSUER.equals(claims.getIssuer())){
                throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
            Date expiry = claims.getExpirationTime();
            if(expiry == null || expiry.before(new Date())){
                throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
            return claims;
        } catch (ParseException | JOSEException e) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    /**
     * type null = access token (có "scope" = vai trò, dùng gọi API); "refresh" / "mfa" = token chỉ dùng cho
     * /auth/refresh hoặc /auth/mfa/**, mang claim "typ" và bị SecurityConfig từ chối khi gọi API.
     */
    private String generateToken(UserEntity user, String type, long duration){
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer(ISSUER)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(duration, ChronoUnit.SECONDS)))
                .jwtID(UUID.randomUUID().toString())
                .claim("userId", user.getUserId())
                .claim(TOKEN_VERSION_CLAIM, user.getTokenVersion());
        if(type != null){
            claims.claim(TOKEN_TYPE_CLAIM, type);
        } else if(user.getRole() != null){
            claims.claim("scope", user.getRole().getRoleName());
        }

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims.build());
        try {
            jwt.sign(new MACSigner(signerKey.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new AppException(ErrorCode.TOKEN_GENERATION_FAILED);
        }
    }
}
