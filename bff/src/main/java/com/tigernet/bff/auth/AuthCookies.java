package com.tigernet.bff.auth;

import com.tigernet.bff.config.BffProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Token đăng nhập nằm trong cookie HttpOnly + SameSite=Strict: JavaScript (kể cả mã độc qua XSS) không đọc
 * được, trang web khác không gửi kèm được.
 * - access token: Path=/ (mọi API /bff/**), sống bằng thời hạn token.
 * - refresh token: Path=/bff/auth, chỉ gửi khi làm mới / đăng xuất.
 * Có Secure thì dùng tiền tố __Host- / __Secure- để trình duyệt ép thêm ràng buộc (không Domain, bắt buộc HTTPS).
 */
@Component
public class AuthCookies {
    static final String REFRESH_PATH = "/bff/auth";

    private final boolean secure;
    private final Duration refreshMaxAge;
    final String accessName;
    final String refreshName;
    // nhớ lựa chọn "Ghi nhớ đăng nhập" để lúc làm mới token cookie refresh giữ đúng thời hạn
    final String rememberName;

    public AuthCookies(BffProperties properties) {
        BffProperties.AuthCookie config = properties.authCookie();
        this.secure = config == null || config.secure();
        this.refreshMaxAge = config != null && config.refreshMaxAge() != null
                ? config.refreshMaxAge() : Duration.ofDays(7);
        this.accessName = secure ? "__Host-tn_at" : "tn_at";
        this.refreshName = secure ? "__Secure-tn_rt" : "tn_rt";
        this.rememberName = secure ? "__Secure-tn_rm" : "tn_rm";
    }

    /** remember = "Ghi nhớ đăng nhập": false thì cookie refresh hết khi đóng trình duyệt */
    public void write(HttpServletResponse response, String accessToken, long accessSeconds,
                      String refreshToken, boolean remember) {
        add(response, accessName, accessToken, "/", Duration.ofSeconds(accessSeconds));
        if (refreshToken != null) {
            Duration maxAge = remember ? refreshMaxAge : null;
            add(response, refreshName, refreshToken, REFRESH_PATH, maxAge);
            add(response, rememberName, remember ? "1" : "0", REFRESH_PATH, maxAge);
        }
    }

    public void clear(HttpServletResponse response) {
        add(response, accessName, "", "/", Duration.ZERO);
        add(response, refreshName, "", REFRESH_PATH, Duration.ZERO);
        add(response, rememberName, "", REFRESH_PATH, Duration.ZERO);
    }

    public boolean readRemember(HttpServletRequest request) {
        return "1".equals(read(request, rememberName));
    }

    public String readAccess(HttpServletRequest request) {
        return read(request, accessName);
    }

    public String readRefresh(HttpServletRequest request) {
        return read(request, refreshName);
    }

    private void add(HttpServletResponse response, String name, String value, String path, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(path);
        if (maxAge != null) {
            cookie.maxAge(maxAge); // null = cookie phiên, hết khi đóng trình duyệt
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.build().toString());
    }

    private static String read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
