package com.tigernet.backend.common.exception;

import com.tigernet.backend.common.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // lỗi nghiệp vụ do service chủ động ném ra
    @ExceptionHandler(AppException.class)
    ResponseEntity<ErrorResponse> handlingAppException(AppException exception){
        return build(exception.getErrorCode());
    }

    // request body không qua được @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handlingValidation(MethodArgumentNotValidException exception){
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(errorCode.getStatus())
                .body(new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), errors));
    }

    // JSON sai cú pháp hoặc sai kiểu dữ liệu
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handlingMalformedJson(HttpMessageNotReadableException exception){
        return build(ErrorCode.MALFORMED_JSON);
    }

    // path variable / request param sai kiểu, vd GET /users/abc
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> handlingTypeMismatch(MethodArgumentTypeMismatchException exception){
        ErrorCode errorCode = ErrorCode.INVALID_PARAMETER;
        return ResponseEntity.status(errorCode.getStatus())
                .body(new ErrorResponse(errorCode.getCode(), "Invalid value for parameter '" + exception.getName() + "'"));
    }

    // vi phạm unique / khóa ngoại trong DB
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handlingDataIntegrity(DataIntegrityViolationException exception){
        log.warn("Data integrity violation: {}", exception.getMostSpecificCause().getMessage());
        return build(ErrorCode.DATA_CONFLICT);
    }

    // mọi lỗi không lường trước: ghi log đầy đủ, không trả chi tiết cho client
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handlingException(Exception exception){
        // lỗi Spring đã có sẵn status (404 sai URL, 405 sai method, 415...) thì giữ nguyên status đó
        if(exception instanceof org.springframework.web.ErrorResponse springError){
            HttpStatusCode status = springError.getStatusCode();
            return ResponseEntity.status(status)
                    .body(new ErrorResponse(status.value(), springError.getBody().getDetail()));
        }
        log.error("Unhandled exception", exception);
        return build(ErrorCode.UNCATEGORIZED);
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode errorCode){
        return ResponseEntity.status(errorCode.getStatus())
                .body(new ErrorResponse(errorCode.getCode(), errorCode.getMessage()));
    }
}
