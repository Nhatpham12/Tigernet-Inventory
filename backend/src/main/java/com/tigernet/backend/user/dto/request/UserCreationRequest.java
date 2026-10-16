package com.tigernet.backend.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserCreationRequest {
    private String fullName;
    private String email;
    private Boolean isActive;
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password; // mật khẩu thô người dùng gõ vào
    private String username;
    private Integer roleId;
}
