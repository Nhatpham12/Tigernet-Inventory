package com.tigernet.backend.common.security;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

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

    // token đang gọi API có một trong các quyền (vd SecurityConfig.ADMIN = "SCOPE_ADMIN")
    public static boolean hasAnyAuthority(String... authorities) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        Set<String> wanted = Set.of(authorities);
        return authentication.getAuthorities().stream().anyMatch(a -> wanted.contains(a.getAuthority()));
    }

    // Role_Name của token đang gọi API (claim "scope" → quyền "SCOPE_<role>"); null nếu không có
    public static String currentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a != null && a.startsWith(SCOPE_PREFIX))
                .map(a -> a.substring(SCOPE_PREFIX.length()))
                .findFirst()
                .orElse(null);
    }

    private static final String SCOPE_PREFIX = "SCOPE_";
}
