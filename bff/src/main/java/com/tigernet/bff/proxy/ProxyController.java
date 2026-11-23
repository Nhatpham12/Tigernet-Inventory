package com.tigernet.bff.proxy;

import com.tigernet.bff.client.BackendClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Set;

/**
 * Chuyển tiếp /bff/api/** sang backend (bỏ tiền tố /bff/api). Dùng cho các thao tác ghi (tạo/sửa/xóa phiếu...)
 * và các API chưa cần ghép dữ liệu, để FE chỉ có một địa chỉ duy nhất là BFF.
 */
@RestController
@RequiredArgsConstructor
public class ProxyController {
    static final String PREFIX = "/bff/api";
    // header của backend được trả lại cho FE (phục vụ tải file in Excel/PDF)
    private static final Set<String> FORWARDED_RESPONSE_HEADERS = Set.of(HttpHeaders.CONTENT_DISPOSITION);
    // đăng nhập chỉ đi qua /bff/auth/** (Nginx giới hạn tốc độ ở đó); actuator chỉ dùng nội bộ
    private static final Set<String> BLOCKED_PREFIXES = Set.of("/auth", "/actuator");

    private final BackendClient backendClient;

    @RequestMapping(PREFIX + "/**")
    ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                 @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                 @RequestBody(required = false) byte[] body) {
        String path = request.getRequestURI().substring(request.getContextPath().length() + PREFIX.length());
        if (isBlocked(path)) {
            return ResponseEntity.notFound().build();
        }
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        request.getParameterMap().forEach((key, values) -> query.addAll(key, java.util.List.of(values)));

        MediaType contentType = request.getContentType() == null ? null : MediaType.parseMediaType(request.getContentType());
        BackendClient.RawResponse response = backendClient.exchange(
                HttpMethod.valueOf(request.getMethod()), path, query, authorization, contentType, body);

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(response.status());
        if (response.contentType() != null) {
            builder.contentType(response.contentType());
        }
        FORWARDED_RESPONSE_HEADERS.forEach(name -> {
            String value = response.headers().getFirst(name);
            if (value != null) {
                builder.header(name, value);
            }
        });
        return builder.body(response.body());
    }

    /**
     * Path ở đây là URI thô (chưa decode). Chặn mọi ký tự '%', '\', ';' (backend không có path variable
     * dạng chuỗi nên request hợp lệ không cần) để không vòng qua được bằng /%61uth, /auth%2f, /auth;x...
     * rồi so prefix sau khi gộp '//' và đưa về chữ thường.
     */
    static boolean isBlocked(String path) {
        if (path.isEmpty() || path.contains("..") || path.contains("%") || path.contains("\\") || path.contains(";")) {
            return true;
        }
        String normalized = path.replaceAll("/{2,}", "/").toLowerCase(Locale.ROOT);
        return BLOCKED_PREFIXES.stream().anyMatch(p -> normalized.equals(p) || normalized.startsWith(p + "/"));
    }
}
