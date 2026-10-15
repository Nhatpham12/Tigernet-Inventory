package com.tigernet.backend.user.service;

import com.tigernet.backend.user.dto.request.UserCreationRequest;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public UserEntity createUser(UserCreationRequest request){
        UserEntity user = new UserEntity();

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setIsActive(request.getIsActive());
        user.setCreatedAt(request.getCreatedAt());
        user.setPasswordHash(request.getPassword());
        user.setUsername(request.getUsername());
        user.setRole(request.getRoleId());

        return userRepository.save(user);
    }

    public List<UserEntity> getUsers(){
        return userRepository.findAll();
    }

    public UserEntity getUserById(Integer userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
