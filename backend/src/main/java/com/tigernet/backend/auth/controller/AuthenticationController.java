package com.tigernet.backend.auth.controller;

import com.tigernet.backend.common.security.NoPageCheck;

import com.tigernet.backend.auth.dto.request.AuthenticationRequest;
import com.tigernet.backend.auth.dto.request.RefreshTokenRequest;
import com.tigernet.backend.auth.dto.response.AuthenticationResponse;
import com.tigernet.backend.auth.service.AuthenticationService;
import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.common.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@NoPageCheck("Đăng nhập, làm mới token, đăng xuất; SecurityConfig quy định cần token hay không")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    // BFF gửi kèm IP của người dùng (lấy từ X-Real-IP do Nginx đặt). Backend không mở ra ngoài,
    // chỉ BFF gọi được, nên header này chỉ dùng để ghi log/nhật ký, không dùng để phân quyền.
    static final String CLIENT_IP_HEADER = "X-Client-IP";

    AuthenticationService authenticationService;

    @PostMapping("/login")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest request,
                                                     HttpServletRequest http){
        // thất bại thì service ném lỗi 401/403
        return ApiResponse.success(authenticationService.authenticate(request, clientIp(http)));
    }

    // đổi refresh token còn hạn lấy cặp access + refresh token mới
    @PostMapping("/refresh")
    ApiResponse<AuthenticationResponse> refresh(@RequestBody @Valid RefreshTokenRequest request){
        return ApiResponse.success(authenticationService.refresh(request.getRefreshToken()));
    }

    // cần access token (SecurityConfig); thu hồi mọi token của user đang đăng nhập
    @PostMapping("/logout")
    ApiResponse<Void> logout(){
        authenticationService.logout(SecurityUtils.currentUserId());
        return ApiResponse.success(null);
    }

    private static String clientIp(HttpServletRequest http){
        String forwarded = http.getHeader(CLIENT_IP_HEADER);
        return forwarded != null && !forwarded.isBlank() ? forwarded : http.getRemoteAddr();
    }
}
