package com.tigernet.backend.auth.service;

import com.tigernet.backend.auth.dto.request.AuthenticationRequest;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.user.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)

public class AuthenticationService {
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;

    // đăng nhập thành công thì không ném lỗi, thất bại thì ném AppException
    public void authenticate(AuthenticationRequest request){
        // sai username và sai mật khẩu trả cùng 1 lỗi để không lộ username nào tồn tại
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        if(!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())){
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if(!Boolean.TRUE.equals(user.getIsActive())){
            throw new AppException(ErrorCode.USER_INACTIVE);
        }
    }
}
