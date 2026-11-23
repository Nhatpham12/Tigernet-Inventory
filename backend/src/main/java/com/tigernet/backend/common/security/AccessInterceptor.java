package com.tigernet.backend.common.security;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.user.service.PageAccessPolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * Kiểm tra quyền theo trang/vai trò cho mọi API của ứng dụng, chạy sau SecurityConfig (đã xác thực token).
 * Mặc định từ chối: handler của TigerNet phải mang @RequiresPage, @RequiresRole hoặc @NoPageCheck
 * (AccessAnnotationCoverageTest kiểm tra lúc build). Annotation ở method ưu tiên hơn ở class.
 * Ném AppException(ACCESS_DENIED) để GlobalExceptionHandler trả 403 cùng format ApiResponse.
 */
@Component
@RequiredArgsConstructor
public class AccessInterceptor implements HandlerInterceptor {
    private static final String APP_PACKAGE = "com.tigernet.backend";

    private final PageAccessPolicy pageAccessPolicy;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)
                || !method.getBeanType().getPackageName().startsWith(APP_PACKAGE)) {
            return true; // tài nguyên tĩnh, trang lỗi của Spring...
        }
        Rules rules = Rules.of(method);
        if (rules.isEmpty()) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (rules.noCheck() != null) {
            return true;
        }
        String role = SecurityUtils.currentRole();
        if (PageAccessPolicy.ADMIN.equals(role)) {
            return true;
        }
        if (rules.roles() != null && Arrays.stream(rules.roles().value()).noneMatch(r -> r.equals(role))) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (rules.pages() != null) {
            pageAccessPolicy.requireAnyPage(rules.pages().value());
        }
        return true;
    }

    /** các annotation phân quyền của một handler (method ưu tiên hơn class) */
    record Rules(RequiresPage pages, RequiresRole roles, NoPageCheck noCheck) {
        static Rules of(HandlerMethod method) {
            return new Rules(find(method, RequiresPage.class), find(method, RequiresRole.class),
                    find(method, NoPageCheck.class));
        }

        boolean isEmpty() {
            return pages == null && roles == null && noCheck == null;
        }

        private static <A extends java.lang.annotation.Annotation> A find(HandlerMethod method, Class<A> type) {
            A onMethod = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), type);
            return onMethod != null ? onMethod : AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), type);
        }
    }
}
