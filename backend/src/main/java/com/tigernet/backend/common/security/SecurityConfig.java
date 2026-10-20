package com.tigernet.backend.common.security;

import com.tigernet.backend.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // các endpoint không cần token (/users tạm mở cho dev để tạo được user đầu tiên)
    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/**",
            "/users/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    @Value("${jwt.signer-key}")
    private String signerKey;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST API không dùng cookie session nên không cần CSRF
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                // đọc header "Authorization: Bearer <token>" do /auth/login cấp
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder()))
                        .authenticationEntryPoint(authenticationEntryPoint()));

        return http.build();
    }

    // cùng khóa và thuật toán với AuthenticationService.generateToken
    @Bean
    JwtDecoder jwtDecoder() {
        SecretKeySpec key = new SecretKeySpec(signerKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS512).build();
    }

    // thiếu token / token sai / hết hạn: trả 401 cùng format ApiResponse thay vì body rỗng
    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> {
            ErrorCode errorCode = ErrorCode.AUTHENTICATION_REQUIRED;
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"code\":" + errorCode.getCode()
                    + ",\"message\":\"" + errorCode.getMessage() + "\"}");
        };
    }

    // dùng chung cho mã hóa (UserService) và kiểm tra mật khẩu (AuthenticationService)
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
