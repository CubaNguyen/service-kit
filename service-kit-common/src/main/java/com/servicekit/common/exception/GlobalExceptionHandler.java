package com.servicekit.common.exception;

import com.servicekit.common.response.ApiResponse;
import com.servicekit.common.response.FieldErrorDetail;
import com.servicekit.common.util.TimeUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.http.ResponseEntity;

import java.util.List;

@Slf4j
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionHandler {

    /**
     * Bắt và bóc tách chi tiết lỗi Validate từ @Valid, @Validated
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<List<FieldErrorDetail>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<FieldErrorDetail> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> FieldErrorDetail.builder()
                        .field(error.getField())
                        .rejectedValue(error.getRejectedValue())
                        .message(error.getDefaultMessage())
                        .build())
                .toList();

        log.warn("[Validation Error] URI: {} | Invalid fields count: {}", request.getRequestURI(), errors.size());

        return ApiResponse.<List<FieldErrorDetail>>builder()
                .code(ErrorCode.BAD_REQUEST.getCode())
                .message("Invalid request data")
                .data(errors)
                .timestamp(TimeUtils.nowEpochMilli())
                .build();
    }

    // Lỗi logic nghiệp vụ
    @ExceptionHandler(BaseBusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BaseBusinessException ex, HttpServletRequest request) {
        log.warn("[Business Error] URI: {} | Code: {} | Message: {}", 
                request.getRequestURI(), ex.getErrorCode().getCode(), ex.getMessage());
                
        ApiResponse<Void> body = ApiResponse.error(ex.getErrorCode().getCode(), ex.getMessage());
        return new ResponseEntity<>(body, ex.getErrorCode().getHttpStatus()); 
    }

    // Lỗi hệ thống bất ngờ
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownException(Exception ex, HttpServletRequest request) {
        log.error("[System Error] Unhandled exception at URI: {}", request.getRequestURI(), ex);
        
        ApiResponse<Void> body = ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}