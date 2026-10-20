package com.tigernet.backend.auth.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)

public class AuthenticationService {
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;

    @NonFinal
    @Value("${jwt.signer-key}")
    String signerKey;

    @NonFinal
    @Value("${jwt.valid-duration}")
    long validDuration; // giây

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

        return AuthenticationResponse.builder()
                .token(generateToken(user))
                .authenticated(true)
                .build();
    }

    private String generateToken(UserEntity user){
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("tigernet")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(validDuration, ChronoUnit.SECONDS)))
                .jwtID(UUID.randomUUID().toString()) // để sau này có thể thu hồi (logout) từng token
                .claim("userId", user.getUserId());
        if(user.getRole() != null){
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
