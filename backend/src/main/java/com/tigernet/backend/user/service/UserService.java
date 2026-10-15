package com.tigernet.backend.user.service;

import com.tigernet.backend.user.dto.request.UserCreationRequest;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    private UserEntity createRequest(UserCreationRequest request){
        User user = new User();

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setIsActive(request.getIsActive());
        user.setCreatedAt(request.getCreatedAt());
        user.setPassword(request.getPassword());
        user.setUsername(request.getUsername());
        user.setRoleId(request.getRoleId());

        return userRepository.save(user);
    }
}
