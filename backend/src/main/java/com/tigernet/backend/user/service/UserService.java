package com.tigernet.backend.user.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.user.dto.request.UserCreationRequest;
import com.tigernet.backend.user.dto.request.UserUpdateRequest;
import com.tigernet.backend.user.entity.RoleEntity;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.user.repository.RoleRepository;
import com.tigernet.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserEntity createUser(UserCreationRequest request){
        UserEntity user = new UserEntity();

        if(userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_EXISTED);
        }
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setIsActive(request.getIsActive());
        user.setCreatedAt(LocalDateTime.now()); // server tự set thời điểm tạo
        user.setPasswordHash(passwordEncoder.encode(request.getPassword())); // mã hóa BCrypt trước khi lưu
        user.setUsername(request.getUsername());
        if(request.getRoleId() != null){
            user.setRole(findRoleById(request.getRoleId()));
        }

        return userRepository.save(user);
    }

    public List<UserEntity> getUsers(){
        return userRepository.findAll();
    }

    public UserEntity getUserById(Integer userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    public UserEntity updateUser(Integer userId, UserUpdateRequest request){
        UserEntity user = getUserById(userId);
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setIsActive(request.getIsActive());
        if(request.getRoleId() != null){
            user.setRole(findRoleById(request.getRoleId()));
        }

        return userRepository.save(user);
    }

    public void deleteUser(Integer userId) {
        if(!userRepository.existsById(userId)){
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        userRepository.deleteById(userId);
    }

    private RoleEntity findRoleById(Integer roleId){
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
    }
}
