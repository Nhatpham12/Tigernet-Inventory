package com.tigernet.backend.user.dto.request;

import com.tigernet.backend.user.entity.RoleEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequest {
    private String fullName;
    private String email;
    private Boolean isActive;
    private Integer roleId;

}
