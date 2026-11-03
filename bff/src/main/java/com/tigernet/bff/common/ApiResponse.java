package com.tigernet.bff.common;

import com.fasterxml.jackson.annotation.JsonInclude;

// cùng format với ApiResponse của backend để Flutter chỉ cần một cách đọc
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int code, String message, T result) {
    public static <T> ApiResponse<T> success(T result) {
        return new ApiResponse<>(1000, null, result);
    }
}
