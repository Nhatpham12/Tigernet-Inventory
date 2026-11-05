package com.tigernet.bff.auth;

import com.tigernet.bff.client.BackendClient;
import com.tigernet.bff.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Đăng nhập / làm mới token. Backend cấp cặp JWT access + refresh; FE lưu ở flutter_secure_storage
 * và tự gọi /bff/auth/refresh khi gặp 401.
 */
@RestController
@RequestMapping("/bff/auth")
@RequiredArgsConstructor
public class AuthController {
    private final BackendClient backendClient;

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    @PostMapping("/login")
    ApiResponse<JsonNode> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.success(backendClient.post("/auth/login",
                Map.of("username", request.username(), "password", request.password()), null));
    }

    @PostMapping("/refresh")
    ApiResponse<JsonNode> refresh(@RequestBody @Valid RefreshRequest request) {
        return ApiResponse.success(backendClient.post("/auth/refresh",
                Map.of("refreshToken", request.refreshToken()), null));
    }
}
