package com.tigernet.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 1xxx: user
    USER_NOT_FOUND(1001, "User not found", HttpStatus.NOT_FOUND),
    USERNAME_EXISTED(1002, "Username already exists", HttpStatus.CONFLICT),

    // 11xx: role
    ROLE_NOT_FOUND(1101, "Role not found", HttpStatus.NOT_FOUND),

    // 9xxx: lỗi chung
    INVALID_REQUEST(9001, "Invalid request data", HttpStatus.BAD_REQUEST),
    MALFORMED_JSON(9002, "Malformed JSON request", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER(9003, "Invalid parameter", HttpStatus.BAD_REQUEST),
    DATA_CONFLICT(9004, "Data conflicts with existing records", HttpStatus.CONFLICT),
    UNCATEGORIZED(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
