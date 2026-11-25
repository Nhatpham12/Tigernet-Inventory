package com.tigernet.backend.audit.config;

import com.tigernet.backend.common.response.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Giữ lại kết quả của thao tác ghi để AuditInterceptor tóm tắt vào nhật ký (mã phiếu, tên, tổng tiền...).
 * Không đổi gì trong response trả về.
 */
@ControllerAdvice
public class AuditResponseCapture implements ResponseBodyAdvice<Object> {
    public static final String RESULT_ATTRIBUTE = AuditResponseCapture.class.getName() + ".result";

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType, Class converterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiResponse<?> api && api.getResult() != null
                && request instanceof ServletServerHttpRequest servlet
                && AuditInterceptor.WRITE_METHODS.contains(servlet.getServletRequest().getMethod())) {
            servlet.getServletRequest().setAttribute(RESULT_ATTRIBUTE, api.getResult());
        }
        return body;
    }
}
