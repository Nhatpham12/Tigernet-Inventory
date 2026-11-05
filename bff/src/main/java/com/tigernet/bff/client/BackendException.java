package com.tigernet.bff.client;

import lombok.Getter;

// backend trả lỗi (4xx/5xx) hoặc không gọi được; GlobalExceptionHandler trả nguyên status + body cho FE
@Getter
public class BackendException extends RuntimeException {
    private final int status;
    private final byte[] body;

    public BackendException(int status, byte[] body, String message) {
        super(message);
        this.status = status;
        this.body = body;
    }
}
