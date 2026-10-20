package com.tigernet.backend.user.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.security.SecurityUtils;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.user.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CurrentUserService {
    UserRepository userRepository;

    // user đang đăng nhập, dùng để ghi Created_By / Approved_By...
    public UserEntity getCurrentUser() {
        return userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.AUTHENTICATION_REQUIRED));
    }
}
