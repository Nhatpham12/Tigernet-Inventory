package com.tigernet.backend.user.controller;

import com.tigernet.backend.common.security.SecurityUtils;
import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.user.dto.request.UserCreationRequest;
import com.tigernet.backend.user.dto.request.UserUpdateRequest;
import com.tigernet.backend.user.dto.response.UserDeleteResponse;
import com.tigernet.backend.user.dto.response.UserResponse;
import com.tigernet.backend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// đọc danh sách: ADMIN, MANAGER; tạo/sửa/xóa: chỉ ADMIN (SecurityConfig)
@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping
    ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        return ApiResponse.success(userService.createUser(request));
    }

    @GetMapping //lấy toàn bộ người dùng
    ApiResponse<List<UserResponse>> getUsers() {
        return ApiResponse.success(userService.getUsers());
    }

    // người dùng đang đăng nhập (đọc userId từ token)
    @GetMapping("/me")
    ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.success(userService.getUserById(SecurityUtils.currentUserId()));
    }

    @GetMapping("/{userId}") //tìm người dùng theo ID
    ApiResponse<UserResponse> getUser(@PathVariable Integer userId) {
        return ApiResponse.success(userService.getUserById(userId));
    }

    @PutMapping("/{userId}")
    ApiResponse<UserResponse> updateUser(@PathVariable Integer userId, @RequestBody @Valid UserUpdateRequest request){
        return ApiResponse.success(userService.updateUser(userId, request));
    }

    // đã có lịch sử thì chỉ khóa tài khoản (result.deleted = false)
    @DeleteMapping("/{userId}")
    ApiResponse<UserDeleteResponse> deleteUser(@PathVariable Integer userId){
        UserDeleteResponse result = userService.deleteUser(userId);
        return ApiResponse.success(result.deleted() ? "User has been deleted" : "User has history and was locked instead", result);
    }
}
