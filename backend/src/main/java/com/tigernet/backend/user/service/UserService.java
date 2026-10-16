package com.tigernet.backend.user.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.user.dto.request.UserCreationRequest;
import com.tigernet.backend.user.dto.request.UserUpdateRequest;
import com.tigernet.backend.user.dto.response.UserResponse;
import com.tigernet.backend.user.entity.RoleEntity;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.user.mapper.UserMapper;
import com.tigernet.backend.user.repository.RoleRepository;
import com.tigernet.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserResponse createUser(UserCreationRequest request){
        if(userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_EXISTED);
        }

        UserEntity user = userMapper.toUser(request);
        user.setCreatedAt(LocalDateTime.now()); // server tự set thời điểm tạo
        user.setPasswordHash(passwordEncoder.encode(request.getPassword())); // mã hóa BCrypt trước khi lưu
        if(request.getRoleId() != null){
            user.setRole(findRoleById(request.getRoleId()));
        }

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers(){
        return userMapper.toUserResponses(userRepository.findAll());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Integer userId){
        return userMapper.toUserResponse(findUserById(userId));
    }

    public UserResponse updateUser(Integer userId, UserUpdateRequest request){
        UserEntity user = findUserById(userId);
        userMapper.updateUser(user, request); // chỉ cập nhật các trường không null
        if(request.getRoleId() != null){
            user.setRole(findRoleById(request.getRoleId()));
        }

        return userMapper.toUserResponse(userRepository.save(user));
    }

    public void deleteUser(Integer userId) {
        if(!userRepository.existsById(userId)){
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        userRepository.deleteById(userId);
    }

    private UserEntity findUserById(Integer userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private RoleEntity findRoleById(Integer roleId){
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
    }
}
