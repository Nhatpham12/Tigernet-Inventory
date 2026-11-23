package com.tigernet.bff.auth;

import com.tigernet.bff.config.BffProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

/**
 * Cho mọi request /bff/**:
 * 1. Chống CSRF: request ghi (POST/PUT/PATCH/DELETE) phải có header {@value #CSRF_HEADER}: 1 (trang web khác
 *    không tự thêm header này được nếu không qua CORS preflight) và Origin (nếu có) phải cùng host hoặc thuộc
 *    BFF_ALLOWED_ORIGINS. Cookie đã SameSite=Strict, đây là lớp chặn thứ hai.
 * 2. Đọc access token từ cookie HttpOnly, gắn thành header Authorization để các controller (và backend) dùng
 *    như cũ. Request đã có Authorization (client không phải trình duyệt) thì giữ nguyên.
 */
@Component
public class CookieAuthFilter extends OncePerRequestFilter {
    public static final String CSRF_HEADER = "X-TigerNet-Request";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final AuthCookies cookies;
    private final CorsConfiguration allowedOrigins = new CorsConfiguration();

    public CookieAuthFilter(AuthCookies cookies, BffProperties properties) {
        this.cookies = cookies;
        properties.cors().allowedOriginPatterns().forEach(allowedOrigins::addAllowedOriginPattern);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/bff/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!SAFE_METHODS.contains(request.getMethod()) && !passesCsrfCheck(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"code\":1007,\"message\":\"Missing or invalid request origin\"}");
            return;
        }
        String token = request.getHeader(HttpHeaders.AUTHORIZATION) == null ? cookies.readAccess(request) : null;
        chain.doFilter(token == null ? request : new BearerRequest(request, "Bearer " + token), response);
    }

    boolean passesCsrfCheck(HttpServletRequest request) {
        if (!"1".equals(request.getHeader(CSRF_HEADER))) {
            return false;
        }
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null) {
            return true; // trình duyệt luôn gửi Origin với request ghi; không có = client không phải trình duyệt
        }
        if (allowedOrigins.checkOrigin(origin) != null) {
            return true;
        }
        try {
            String host = URI.create(origin).getHost();
            return host != null && host.equalsIgnoreCase(request.getServerName()); // cùng trang (qua Nginx)
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** request gốc + header Authorization lấy từ cookie */
    private static final class BearerRequest extends HttpServletRequestWrapper {
        private final String authorization;

        BearerRequest(HttpServletRequest request, String authorization) {
            super(request);
            this.authorization = authorization;
        }

        @Override
        public String getHeader(String name) {
            return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name) ? authorization : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)
                    ? Collections.enumeration(java.util.List.of(authorization)) : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            java.util.List<String> names = Collections.list(super.getHeaderNames());
            if (names.stream().noneMatch(HttpHeaders.AUTHORIZATION::equalsIgnoreCase)) {
                names.add(HttpHeaders.AUTHORIZATION);
            }
            return Collections.enumeration(names);
        }
    }
}
