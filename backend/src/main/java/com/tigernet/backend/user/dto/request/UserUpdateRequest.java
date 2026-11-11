package com.tigernet.backend.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// không có username: username chỉ đặt lúc tạo (BACKLOG QT-01)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequest {
    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;
    @Email(message = "Email is invalid")
    private String email;
    private Boolean isActive;
    private Integer roleId;
    // để trống = giữ mật khẩu cũ; có giá trị = Quản trị đặt lại mật khẩu
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
