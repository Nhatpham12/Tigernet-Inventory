package com.tigernet.backend.user.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.user.dto.response.RoleResponse;
import com.tigernet.backend.user.mapper.RoleMapper;
import com.tigernet.backend.user.repository.RoleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 5 vai trò cố định được seed sẵn, chỉ cho đọc
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class RoleService {
    RoleRepository roleRepository;
    RoleMapper roleMapper;

    public List<RoleResponse> getRoles() {
        return roleMapper.toRoleResponses(roleRepository.findAll());
    }

    public RoleResponse getRole(Integer roleId) {
        return roleMapper.toRoleResponse(roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND)));
    }
}
