package com.tigernet.backend.auth.controller;

import com.tigernet.backend.auth.dto.request.AuthenticationRequest;
import com.tigernet.backend.auth.dto.response.AuthenticationResponse;
import com.tigernet.backend.auth.service.AuthenticationService;
import com.tigernet.backend.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;
    @PostMapping("/login")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest request){
        // thất bại thì service ném lỗi 401/403
        return ApiResponse.success(authenticationService.authenticate(request));
    }
}
