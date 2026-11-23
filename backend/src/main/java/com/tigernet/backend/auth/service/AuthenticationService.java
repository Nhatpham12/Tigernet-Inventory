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
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)

public class AuthenticationService {
    // claim "typ" phân biệt 2 loại token; SecurityConfig từ chối refresh token khi gọi API
    public static final String TOKEN_TYPE_CLAIM = "typ";
    public static final String REFRESH_TOKEN_TYPE = "refresh";
    public static final String ISSUER = "tigernet";
    // claim "ver" = Users.Token_Version lúc cấp token; SecurityConfig và refresh() so với giá trị hiện tại
    public static final String TOKEN_VERSION_CLAIM = "ver";

    static final int MAX_FAILED_LOGINS = 5;
    static final int LOCK_MINUTES = 15;
    // hash BCrypt (cùng cost với SecurityConfig) để so khi username không tồn tại, thời gian phản hồi như nhau
    private static final String DUMMY_HASH = new BCryptPasswordEncoder(10).encode(UUID.randomUUID().toString());

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    AuditLogService auditLogService;

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
     * Sai mật khẩu {@value #MAX_FAILED_LOGINS} lần liên tiếp thì khoá {@value #LOCK_MINUTES} phút.
     * Sai username, sai mật khẩu, đang bị khoá đều trả cùng 1 lỗi để không lộ username nào tồn tại.
     * noRollbackFor: bộ đếm sai và nhật ký vẫn được lưu dù hàm ném lỗi.
     */
    @Transactional(noRollbackFor = AppException.class)
    public AuthenticationResponse authenticate(AuthenticationRequest request, String clientIp){
        var found = userRepository.findByUsername(request.getUsername());
        if(found.isEmpty()){
            passwordEncoder.matches(request.getPassword(), DUMMY_HASH); // tốn thời gian như user có thật
            logFailure(request.getUsername(), clientIp, "unknown-user");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        UserEntity user = found.get();
        LocalDateTime now = LocalDateTime.now();

        if(user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)){
            // không kiểm tra mật khẩu thật khi đang khoá, nhưng vẫn tốn thời gian như nhau để không lộ trạng thái
            passwordEncoder.matches(request.getPassword(), DUMMY_HASH);
            logFailure(user.getUsername(), clientIp, "locked");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if(!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())){
            recordFailedPassword(user, now, clientIp);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if(!Boolean.TRUE.equals(user.getIsActive())){
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        AuthenticationResponse response = issueTokens(user);
        auditLogService.log(user, "LOGIN", "Users", user.getUserId(), null);
        return response;
    }

    // đăng xuất mọi thiết bị: tăng Token_Version, mọi access/refresh token cũ của user mất hiệu lực
    @Transactional
    public void logout(Integer userId){
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.AUTHENTICATION_REQUIRED));
        user.revokeTokens();
        auditLogService.log(user, "LOGOUT", "Users", userId, null);
    }

    private void recordFailedPassword(UserEntity user, LocalDateTime now, String clientIp){
        int failed = user.getFailedLoginCount() + 1;
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("ip", clientIp);
        if(failed >= MAX_FAILED_LOGINS){
            user.setFailedLoginCount(0);
            user.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
            detail.put("lockedMinutes", LOCK_MINUTES);
        } else {
            user.setFailedLoginCount(failed);
            detail.put("failedCount", failed);
        }
        auditLogService.log(user, "LOGIN_FAILED", "Users", user.getUserId(), detail);
        logFailure(user.getUsername(), clientIp, failed >= MAX_FAILED_LOGINS ? "locked-now" : "bad-password");
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
        // đã đăng xuất / đổi mật khẩu / đổi vai trò sau khi token này được cấp
        if(!(claims.getClaim(TOKEN_VERSION_CLAIM) instanceof Number ver) || ver.intValue() != user.getTokenVersion()){
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return issueTokens(user);
    }

    private AuthenticationResponse issueTokens(UserEntity user){
        return AuthenticationResponse.builder()
                .token(generateToken(user, false))
                .refreshToken(generateToken(user, true))
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

    private String generateToken(UserEntity user, boolean refresh){
        Instant now = Instant.now();
        long duration = refresh ? refreshDuration : validDuration;
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer(ISSUER)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(duration, ChronoUnit.SECONDS)))
                .jwtID(UUID.randomUUID().toString())
                .claim("userId", user.getUserId())
                .claim(TOKEN_VERSION_CLAIM, user.getTokenVersion());
        if(refresh){
            claims.claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE);
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
