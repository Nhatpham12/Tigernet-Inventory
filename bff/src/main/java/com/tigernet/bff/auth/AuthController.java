package com.tigernet.bff.auth;

import com.tigernet.bff.client.BackendClient;
import com.tigernet.bff.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Đăng nhập / làm mới token / đăng xuất. Backend cấp cặp JWT access + refresh; FE lưu ở flutter_secure_storage
 * và tự gọi /bff/auth/refresh khi gặp 401.
 */
@RestController
@RequestMapping("/bff/auth")
@RequiredArgsConstructor
public class AuthController {
    // Nginx ghi đè X-Real-IP bằng IP thật của client ($remote_addr) nên client không giả được khi đi qua Nginx
    private static final String REAL_IP_HEADER = "X-Real-IP";
    private static final String CLIENT_IP_HEADER = "X-Client-IP";

    private final BackendClient backendClient;

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    @PostMapping("/login")
    ApiResponse<JsonNode> login(@RequestBody @Valid LoginRequest request, HttpServletRequest http) {
        return ApiResponse.success(backendClient.post("/auth/login",
                Map.of("username", request.username(), "password", request.password()), null,
                Map.of(CLIENT_IP_HEADER, clientIp(http))));
    }

    @PostMapping("/refresh")
    ApiResponse<JsonNode> refresh(@RequestBody @Valid RefreshRequest request) {
        return ApiResponse.success(backendClient.post("/auth/refresh",
                Map.of("refreshToken", request.refreshToken()), null));
    }

    // thu hồi mọi token của user (đăng xuất mọi thiết bị)
    @PostMapping("/logout")
    ApiResponse<JsonNode> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return ApiResponse.success(backendClient.post("/auth/logout", null, authorization));
    }

    private static String clientIp(HttpServletRequest http) {
        String realIp = http.getHeader(REAL_IP_HEADER);
        return realIp != null && !realIp.isBlank() ? realIp.trim() : http.getRemoteAddr();
    }
}
