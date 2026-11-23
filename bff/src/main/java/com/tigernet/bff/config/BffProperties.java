package com.tigernet.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "tigernet")
public record BffProperties(Backend backend, Cors cors, AuthCookie authCookie) {
    public record Backend(String baseUrl, Duration connectTimeout, Duration readTimeout) {
    }

    public record Cors(List<String> allowedOriginPatterns) {
    }

    /**
     * Cookie giữ token. secure = true bắt buộc khi chạy HTTPS (mặc định); chỉ đặt false khi máy chủ
     * chạy HTTP thuần (chưa có chứng chỉ), vì trình duyệt bỏ cookie Secure trên HTTP (trừ localhost).
     * refreshMaxAge: thời hạn cookie refresh khi chọn "Ghi nhớ đăng nhập" (khớp jwt.refresh-duration backend).
     */
    public record AuthCookie(boolean secure, Duration refreshMaxAge) {
    }
}
