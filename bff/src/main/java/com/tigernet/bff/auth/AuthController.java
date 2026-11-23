package com.tigernet.bff.auth;

import com.tigernet.bff.client.BackendClient;
import com.tigernet.bff.client.BackendException;
import com.tigernet.bff.common.ApiResponse;
import com.tigernet.bff.common.Json;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Đăng nhập / làm mới token / đăng xuất. Backend cấp cặp JWT access + refresh; BFF cất vào cookie HttpOnly
 * (AuthCookies) và KHÔNG trả token trong body, nên JavaScript trên trang không bao giờ thấy token.
 * FE tự gọi /bff/auth/refresh khi gặp 401.
 */
@RestController
@RequestMapping("/bff/auth")
@RequiredArgsConstructor
public class AuthController {
    // Nginx ghi đè X-Real-IP bằng IP thật của client ($remote_addr) nên client không giả được khi đi qua Nginx
    private static final String REAL_IP_HEADER = "X-Real-IP";
    private static final String CLIENT_IP_HEADER = "X-Client-IP";
    private static final String INVALID_REFRESH =
            "{\"code\":1006,\"message\":\"Invalid or expired refresh token\"}";

    private final BackendClient backendClient;
    private final AuthCookies cookies;

    /** remember = "Ghi nhớ đăng nhập" (mặc định có) */
    public record LoginRequest(@NotBlank String username, @NotBlank String password, Boolean remember) {
    }

    /** body FE nhận được: không có token */
    public record SessionTokens(long expiresIn, boolean authenticated) {
    }

    @PostMapping("/login")
    ApiResponse<SessionTokens> login(@RequestBody @Valid LoginRequest request, HttpServletRequest http,
                                     HttpServletResponse response) {
        JsonNode tokens = backendClient.post("/auth/login",
                Map.of("username", request.username(), "password", request.password()), null,
                Map.of(CLIENT_IP_HEADER, clientIp(http)));
        return ApiResponse.success(store(tokens, !Boolean.FALSE.equals(request.remember()), response));
    }

    // refresh token lấy từ cookie (Path=/bff/auth); backend xoay vòng nên ghi lại cả hai cookie
    @PostMapping("/refresh")
    ApiResponse<SessionTokens> refresh(HttpServletRequest http, HttpServletResponse response) {
        String refreshToken = cookies.readRefresh(http);
        if (refreshToken == null) {
            throw new BackendException(401, INVALID_REFRESH.getBytes(StandardCharsets.UTF_8), "No refresh cookie");
        }
        try {
            JsonNode tokens = backendClient.post("/auth/refresh", Map.of("refreshToken", refreshToken), null);
            return ApiResponse.success(store(tokens, cookies.readRemember(http), response));
        } catch (BackendException e) {
            if (e.getStatus() == 400 || e.getStatus() == 401 || e.getStatus() == 403) {
                cookies.clear(response); // refresh token hết hạn / bị thu hồi: xoá cookie cũ
            }
            throw e;
        }
    }

    // thu hồi mọi token của user (đăng xuất mọi thiết bị); luôn xoá cookie dù backend lỗi
    @PostMapping("/logout")
    ApiResponse<Void> logout(HttpServletRequest http, HttpServletResponse response) {
        String authorization = http.getHeader(HttpHeaders.AUTHORIZATION); // CookieAuthFilter gắn từ cookie
        try {
            if (authorization != null) {
                backendClient.post("/auth/logout", null, authorization);
            }
        } catch (BackendException e) {
            // token đã hết hạn / bị thu hồi: phía server không còn gì phải thu hồi
        } finally {
            cookies.clear(response);
        }
        return ApiResponse.success(null);
    }

    private SessionTokens store(JsonNode tokens, boolean remember, HttpServletResponse response) {
        long expiresIn = Json.decimal(tokens, "expiresIn").longValue();
        cookies.write(response, Json.text(tokens, "token"), expiresIn, Json.text(tokens, "refreshToken"), remember);
        return new SessionTokens(expiresIn, true);
    }

    private static String clientIp(HttpServletRequest http) {
        String realIp = http.getHeader(REAL_IP_HEADER);
        return realIp != null && !realIp.isBlank() ? realIp.trim() : http.getRemoteAddr();
    }
}
