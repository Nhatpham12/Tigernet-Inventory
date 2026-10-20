package com.tigernet.backend.common.security;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    // lấy userId từ claim "userId" của token đang gọi API
    public static Integer currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            Object userId = jwt.getClaim("userId");
            if (userId instanceof Number number) {
                return number.intValue();
            }
        }
        throw new AppException(ErrorCode.AUTHENTICATION_REQUIRED);
    }
}
