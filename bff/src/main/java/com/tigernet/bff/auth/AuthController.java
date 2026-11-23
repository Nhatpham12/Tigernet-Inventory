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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    /**
     * Body FE nhận được: không có token gọi API. Quản trị còn bước mã 6 số thì authenticated = false và có
     * mfaToken (5 phút, chỉ dùng cho /bff/auth/mfa/**); recoveryCodes chỉ có ngay khi vừa bật MFA.
     */
    public record SessionTokens(long expiresIn, boolean authenticated, boolean mfaRequired, boolean mfaSetupRequired,
                                String mfaToken, List<String> recoveryCodes) {
    }

    public record MfaTokenRequest(@NotBlank String mfaToken) {
    }

    public record MfaCodeRequest(@NotBlank String mfaToken, String code, String recoveryCode, Boolean remember) {
    }

    @PostMapping("/login")
    ApiResponse<SessionTokens> login(@RequestBody @Valid LoginRequest request, HttpServletRequest http,
                                     HttpServletResponse response) {
        JsonNode result = backendClient.post("/auth/login",
                Map.of("username", request.username(), "password", request.password()), null,
                Map.of(CLIENT_IP_HEADER, clientIp(http)));
        return ApiResponse.success(store(result, !Boolean.FALSE.equals(request.remember()), response));
    }

    // Quản trị cài MFA lần đầu: trả khoá + link otpauth:// để FE vẽ QR
    @PostMapping("/mfa/setup")
    ApiResponse<JsonNode> mfaSetup(@RequestBody @Valid MfaTokenRequest request) {
        return ApiResponse.success(backendClient.post("/auth/mfa/setup", Map.of("mfaToken", request.mfaToken()), null));
    }

    @PostMapping("/mfa/confirm")
    ApiResponse<SessionTokens> mfaConfirm(@RequestBody @Valid MfaCodeRequest request, HttpServletRequest http,
                                          HttpServletResponse response) {
        return mfaStep("/auth/mfa/confirm", request, http, response);
    }

    @PostMapping("/mfa/verify")
    ApiResponse<SessionTokens> mfaVerify(@RequestBody @Valid MfaCodeRequest request, HttpServletRequest http,
                                         HttpServletResponse response) {
        return mfaStep("/auth/mfa/verify", request, http, response);
    }

    private ApiResponse<SessionTokens> mfaStep(String path, MfaCodeRequest request, HttpServletRequest http,
                                               HttpServletResponse response) {
        Map<String, Object> body = new HashMap<>();
        body.put("mfaToken", request.mfaToken());
        body.put("code", request.code());
        body.put("recoveryCode", request.recoveryCode());
        JsonNode result = backendClient.post(path, body, null, Map.of(CLIENT_IP_HEADER, clientIp(http)));
        return ApiResponse.success(store(result, !Boolean.FALSE.equals(request.remember()), response));
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

    /** có token thì cất vào cookie; chưa có (Quản trị còn bước MFA) thì chỉ chuyển tiếp thông tin bước MFA */
    private SessionTokens store(JsonNode result, boolean remember, HttpServletResponse response) {
        String accessToken = Json.text(result, "token");
        List<String> recoveryCodes = null;
        if (result.path("recoveryCodes").isArray()) {
            recoveryCodes = new ArrayList<>();
            for (JsonNode code : result.path("recoveryCodes")) {
                recoveryCodes.add(code.asText());
            }
        }
        if (accessToken == null) {
            return new SessionTokens(0, false, result.path("mfaRequired").asBoolean(false),
                    result.path("mfaSetupRequired").asBoolean(false), Json.text(result, "mfaToken"), null);
        }
        long expiresIn = Json.decimal(result, "expiresIn").longValue();
        cookies.write(response, accessToken, expiresIn, Json.text(result, "refreshToken"), remember);
        return new SessionTokens(expiresIn, true, false, false, null, recoveryCodes);
    }

    private static String clientIp(HttpServletRequest http) {
        String realIp = http.getHeader(REAL_IP_HEADER);
        return realIp != null && !realIp.isBlank() ? realIp.trim() : http.getRemoteAddr();
    }
}
