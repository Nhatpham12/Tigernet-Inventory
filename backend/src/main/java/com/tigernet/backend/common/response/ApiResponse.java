package com.tigernet.backend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tigernet.backend.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // trường null thì không trả ra JSON
public class ApiResponse<T> {
    @Builder.Default
    private int code = 1000;            // 1000 = thành công
    private String message;
    private T result;                   // dữ liệu khi thành công
    private Map<String, String> errors; // lỗi theo từng field khi validate

    public static <T> ApiResponse<T> success(T result) {
        return ApiResponse.<T>builder().result(result).build();
    }

    public static <T> ApiResponse<T> success(String message, T result) {
        return ApiResponse.<T>builder().message(message).result(result).build();
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }

    public static ApiResponse<Void> error(int code, String message) {
        return ApiResponse.<Void>builder().code(code).message(message).build();
    }
}
