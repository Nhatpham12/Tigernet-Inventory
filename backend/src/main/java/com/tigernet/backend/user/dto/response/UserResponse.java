package com.tigernet.backend.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private Integer userId;
    private String fullName;
    private String email;
    private String username;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private Integer roleId;
    private String roleName;
    // không có password: không bao giờ trả mật khẩu ra API
}
