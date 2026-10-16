package com.tigernet.backend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private int code;
    private String message;
    private Map<String, String> errors; // lỗi theo từng field khi validate, null thì không trả về

    public ErrorResponse(int code, String message) {
        this(code, message, null);
    }
}
