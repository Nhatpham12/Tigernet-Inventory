package com.tigernet.backend.user.dto.request;

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
    private String password; // mật khẩu thô người dùng gõ vào
    private String username;
    private Integer roleId;
}
