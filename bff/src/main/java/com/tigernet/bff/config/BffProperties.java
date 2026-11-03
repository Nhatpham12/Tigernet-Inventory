package com.tigernet.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "tigernet")
public record BffProperties(Backend backend, Cors cors) {
    public record Backend(String baseUrl, Duration connectTimeout, Duration readTimeout) {
    }

    public record Cors(List<String> allowedOriginPatterns) {
    }
}
