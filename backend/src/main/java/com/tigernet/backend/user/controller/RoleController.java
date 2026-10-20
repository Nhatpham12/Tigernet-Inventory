package com.tigernet.backend.user.controller;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.user.dto.response.RoleResponse;
import com.tigernet.backend.user.service.RoleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleController {
    RoleService roleService;

    @GetMapping
    ApiResponse<List<RoleResponse>> getRoles() {
        return ApiResponse.success(roleService.getRoles());
    }

    @GetMapping("/{roleId}")
    ApiResponse<RoleResponse> getRole(@PathVariable Integer roleId) {
        return ApiResponse.success(roleService.getRole(roleId));
    }
}
