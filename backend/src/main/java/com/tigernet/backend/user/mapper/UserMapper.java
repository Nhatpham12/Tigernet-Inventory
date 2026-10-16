package com.tigernet.backend.user.mapper;

import com.tigernet.backend.user.dto.request.UserCreationRequest;
import com.tigernet.backend.user.dto.request.UserUpdateRequest;
import com.tigernet.backend.user.dto.response.UserResponse;
import com.tigernet.backend.user.entity.UserEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // role, passwordHash, createdAt do service tự xử lý
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    UserEntity toUser(UserCreationRequest request);

    @Mapping(target = "roleId", source = "role.id")
    @Mapping(target = "roleName", source = "role.roleName")
    UserResponse toUserResponse(UserEntity user);

    List<UserResponse> toUserResponses(List<UserEntity> users);

    // trường nào request gửi null thì giữ nguyên giá trị cũ
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "username", ignore = true)
    void updateUser(@MappingTarget UserEntity user, UserUpdateRequest request);
}
