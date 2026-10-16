package com.tigernet.backend.user.controller;

import com.tigernet.backend.user.dto.request.UserCreationRequest;
import com.tigernet.backend.user.dto.request.UserUpdateRequest;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping
    UserEntity createUser(@RequestBody UserCreationRequest request) {
        return userService.createUser(request);
    }

    @GetMapping //lấy toàn bộ người dùng
    List<UserEntity> getUsers() {
        return userService.getUsers();
    }

    @GetMapping("/{userId}") //tìm người dùng theo ID
    UserEntity getUser(@PathVariable Integer userId) {
        return userService.getUserById(userId);
    }

    @PutMapping("/{userId}")
    UserEntity updateUser(@PathVariable Integer userId, @RequestBody UserUpdateRequest request){
        return userService.updateUser(userId, request);
    }
    @DeleteMapping("/{userId}")
    String deleteUser(@PathVariable Integer userId){
        userService.deleteUser(userId);
        return "User has been deleted";
    }
}
