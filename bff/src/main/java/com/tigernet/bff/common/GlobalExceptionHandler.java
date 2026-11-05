package com.tigernet.bff.common;

import com.tigernet.bff.client.BackendException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // giữ nguyên status và body lỗi của backend (code/message/errors) để FE hiển thị đúng
    @ExceptionHandler(BackendException.class)
    ResponseEntity<byte[]> handleBackend(BackendException e) {
        return ResponseEntity.status(e.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(e.getBody());
    }

    // thiếu header Authorization: trả 401 giống backend để FE chuyển về màn đăng nhập
    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException e) {
        if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(e.getHeaderName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(1005, "Missing, invalid or expired access token", null));
        }
        return ResponseEntity.badRequest().body(new ApiResponse<>(9400, e.getMessage(), null));
    }
}
