package com.tigernet.backend.common.exception;

import com.tigernet.backend.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.ErrorResponse;
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
    ResponseEntity<ApiResponse<Void>> handlingAppException(AppException exception){
        return build(exception.getErrorCode());
    }

    // request body không qua được @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handlingValidation(MethodArgumentNotValidException exception){
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        ApiResponse<Void> body = ApiResponse.error(errorCode);
        body.setErrors(errors);
        return ResponseEntity.status(errorCode.getStatus()).body(body);
    }

    // JSON sai cú pháp hoặc sai kiểu dữ liệu
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> handlingMalformedJson(HttpMessageNotReadableException exception){
        return build(ErrorCode.MALFORMED_JSON);
    }

    // path variable / request param sai kiểu, vd GET /users/abc
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiResponse<Void>> handlingTypeMismatch(MethodArgumentTypeMismatchException exception){
        ErrorCode errorCode = ErrorCode.INVALID_PARAMETER;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), "Invalid value for parameter '" + exception.getName() + "'"));
    }

    // vi phạm unique / khóa ngoại trong DB
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> handlingDataIntegrity(DataIntegrityViolationException exception){
        log.warn("Data integrity violation: {}", exception.getMostSpecificCause().getMessage());
        return build(ErrorCode.DATA_CONFLICT);
    }

    // 2 giao dịch cùng sửa 1 bản ghi có @Version (Inventory): giao dịch sau bị từ chối
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiResponse<Void>> handlingOptimisticLock(ObjectOptimisticLockingFailureException exception){
        return build(ErrorCode.CONCURRENT_UPDATE);
    }

    // mọi lỗi không lường trước: ghi log đầy đủ, không trả chi tiết cho client
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handlingException(Exception exception){
        // lỗi Spring đã có sẵn status (404 sai URL, 405 sai method, 415...) thì giữ nguyên status đó
        if(exception instanceof ErrorResponse springError){
            HttpStatusCode status = springError.getStatusCode();
            return ResponseEntity.status(status)
                    .body(ApiResponse.error(status.value(), springError.getBody().getDetail()));
        }
        log.error("Unhandled exception", exception);
        return build(ErrorCode.UNCATEGORIZED);
    }

    private ResponseEntity<ApiResponse<Void>> build(ErrorCode errorCode){
        return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.error(errorCode));
    }
}
