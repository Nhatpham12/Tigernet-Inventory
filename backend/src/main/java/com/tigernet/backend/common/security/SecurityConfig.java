package com.tigernet.backend.common.security;

import com.tigernet.backend.auth.service.AuthenticationService;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // claim "scope" của access token = Role_Name, Spring Security đổi thành quyền "SCOPE_<role>"
    public static final String ADMIN = "SCOPE_ADMIN";
    public static final String MANAGER = "SCOPE_MANAGER";

    // các endpoint không cần token
    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    @Value("${jwt.signer-key}")
    private String signerKey;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http
                // REST API không dùng cookie session nên không cần CSRF
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // đăng xuất cần biết ai đang gọi (đặt trước /auth/** công khai)
                        .requestMatchers(HttpMethod.POST, "/auth/logout").authenticated()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // thông tin + quyền xem trang của chính mình
                        .requestMatchers("/users/me", "/users/me/**").authenticated()
                        // BFF đọc kho được phân công của user đang đăng nhập khi mở app;
                        // UserWarehouseService chỉ cho xem của chính mình (Quản trị/Quản lý xem được mọi người)
                        .requestMatchers(HttpMethod.GET, "/users/*/warehouses").authenticated()
                        // Quản lý xem được danh sách nhân viên, chỉ Quản trị được tạo/sửa/xóa/phân quyền
                        .requestMatchers(HttpMethod.GET, "/users").hasAnyAuthority(ADMIN, MANAGER)
                        .requestMatchers("/users", "/users/**").hasAuthority(ADMIN)
                        .requestMatchers("/audit-logs", "/audit-logs/**").hasAuthority(ADMIN)
                        // còn lại: cần đăng nhập; quyền theo trang/vai trò của từng API do AccessInterceptor kiểm tra
                        // (@RequiresPage, @RequiresRole, @NoPageCheck trên controller; mặc định từ chối)
                        .anyRequest().authenticated())
                // đọc header "Authorization: Bearer <token>" do /auth/login cấp
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler()));

        return http.build();
    }

    // cùng khóa và thuật toán với AuthenticationService.generateToken
    @Bean
    JwtDecoder jwtDecoder(UserRepository userRepository) {
        SecretKeySpec key = new SecretKeySpec(signerKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS512).build();
        // refresh token chỉ dùng cho /auth/refresh, không được dùng làm access token
        OAuth2TokenValidator<Jwt> notRefreshToken = jwt -> AuthenticationService.REFRESH_TOKEN_TYPE
                .equals(jwt.getClaimAsString(AuthenticationService.TOKEN_TYPE_CLAIM))
                ? invalid("Refresh token cannot access APIs")
                : OAuth2TokenValidatorResult.success();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(AuthenticationService.ISSUER), notRefreshToken,
                tokenVersionValidator(userRepository)));
        return decoder;
    }

    /**
     * Token bị thu hồi khi user đăng xuất, đổi mật khẩu, đổi vai trò hoặc bị khoá: claim "ver" phải bằng
     * Users.Token_Version hiện tại và user còn hoạt động. Mỗi request tốn 1 truy vấn theo khoá chính.
     */
    public static OAuth2TokenValidator<Jwt> tokenVersionValidator(UserRepository userRepository) {
        return jwt -> {
            if (!(jwt.getClaim("userId") instanceof Number userId)
                    || !(jwt.getClaim(AuthenticationService.TOKEN_VERSION_CLAIM) instanceof Number ver)) {
                return invalid("Token is missing required claims");
            }
            boolean current = userRepository.findActiveTokenVersion(userId.intValue())
                    .map(v -> v == ver.intValue())
                    .orElse(false);
            return current ? OAuth2TokenValidatorResult.success() : invalid("Token has been revoked");
        };
    }

    private static OAuth2TokenValidatorResult invalid(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }

    // thiếu token / token sai / hết hạn: trả 401 cùng format ApiResponse thay vì body rỗng
    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) ->
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.AUTHENTICATION_REQUIRED);
    }

    // có token nhưng vai trò không đủ quyền: 403 cùng format ApiResponse
    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) ->
                writeError(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.ACCESS_DENIED);
    }

    private static void writeError(HttpServletResponse response, int status, ErrorCode errorCode) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":" + errorCode.getCode()
                + ",\"message\":\"" + errorCode.getMessage() + "\"}");
    }

    // dùng chung cho mã hóa (UserService) và kiểm tra mật khẩu (AuthenticationService)
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
