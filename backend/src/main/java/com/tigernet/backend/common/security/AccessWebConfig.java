package com.tigernet.backend.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class AccessWebConfig implements WebMvcConfigurer {
    private final AccessInterceptor accessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // chạy trước AuditInterceptor: request bị từ chối không ghi nhật ký thao tác
        registry.addInterceptor(accessInterceptor).order(Ordered.HIGHEST_PRECEDENCE);
    }
}
