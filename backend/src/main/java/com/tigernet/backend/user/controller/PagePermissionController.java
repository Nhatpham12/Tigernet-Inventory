package com.tigernet.backend.user.controller;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.common.security.SecurityUtils;
import com.tigernet.backend.user.dto.request.PagePermissionRequest;
import com.tigernet.backend.user.dto.response.PagePermissionResponse;
import com.tigernet.backend.user.service.PagePermissionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

// /users/me/** cho mọi user đã đăng nhập; /users/{id}/** chỉ ADMIN (SecurityConfig)
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PagePermissionController {
    PagePermissionService pagePermissionService;

    @GetMapping("/me/page-permissions")
    ApiResponse<PagePermissionResponse> getMine() {
        return ApiResponse.success(pagePermissionService.get(SecurityUtils.currentUserId()));
    }

    @GetMapping("/{userId}/page-permissions")
    ApiResponse<PagePermissionResponse> get(@PathVariable Integer userId) {
        return ApiResponse.success(pagePermissionService.get(userId));
    }

    @PutMapping("/{userId}/page-permissions")
    ApiResponse<PagePermissionResponse> set(@PathVariable Integer userId,
                                            @RequestBody @Valid PagePermissionRequest request) {
        return ApiResponse.success(pagePermissionService.set(userId, request.getPages()));
    }

    @DeleteMapping("/{userId}/page-permissions")
    ApiResponse<PagePermissionResponse> reset(@PathVariable Integer userId) {
        return ApiResponse.success(pagePermissionService.reset(userId));
    }
}
