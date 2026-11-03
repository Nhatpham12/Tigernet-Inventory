package com.tigernet.backend.auth.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)

public class AuthenticationService {
    // claim "typ" phân biệt 2 loại token; SecurityConfig từ chối refresh token khi gọi API
    public static final String TOKEN_TYPE_CLAIM = "typ";
    public static final String REFRESH_TOKEN_TYPE = "refresh";

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;

    @NonFinal
    @Value("${jwt.signer-key}")
    String signerKey;

    @NonFinal
    @Value("${jwt.valid-duration}")
    long validDuration; // giây

    @NonFinal
    @Value("${jwt.refresh-duration:604800}")
    long refreshDuration; // giây, mặc định 7 ngày

    // đăng nhập thành công thì trả token, thất bại thì ném AppException
    @Transactional(readOnly = true) // giữ session để đọc role (LAZY) khi tạo token
    public AuthenticationResponse authenticate(AuthenticationRequest request){
        // sai username và sai mật khẩu trả cùng 1 lỗi để không lộ username nào tồn tại
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        if(!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())){
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if(!Boolean.TRUE.equals(user.getIsActive())){
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        return issueTokens(user);
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
            if(!jwt.verify(new MACVerifier(signerKey.getBytes(StandardCharsets.UTF_8)))){
                throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
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
                .issuer("tigernet")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(duration, ChronoUnit.SECONDS)))
                .jwtID(UUID.randomUUID().toString()) // để sau này có thể thu hồi (logout) từng token
                .claim("userId", user.getUserId());
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
