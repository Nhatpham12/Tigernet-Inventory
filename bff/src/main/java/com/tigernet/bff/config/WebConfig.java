package com.tigernet.bff.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Flutter Web gọi BFF từ origin khác nên cần CORS; backend chỉ nhận gọi từ BFF (server-to-server)
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final BffProperties properties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/bff/**")
                .allowedOriginPatterns(properties.cors().allowedOriginPatterns().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition")
                .maxAge(3600);
    }
}
