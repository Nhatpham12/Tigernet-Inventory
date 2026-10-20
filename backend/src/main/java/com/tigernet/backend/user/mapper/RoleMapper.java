package com.tigernet.backend.user.mapper;

import com.tigernet.backend.common.config.MapstructConfig;
import com.tigernet.backend.user.dto.response.RoleResponse;
import com.tigernet.backend.user.entity.RoleEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = MapstructConfig.class)
public interface RoleMapper {
    RoleResponse toRoleResponse(RoleEntity role);

    List<RoleResponse> toRoleResponses(List<RoleEntity> roles);
}
