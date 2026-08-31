package com.servicekit.security.exception;

import com.servicekit.common.exception.ErrorCode;
import com.servicekit.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ControllerAdvice xử lý toàn bộ các exception liên quan đến bảo mật (Xác thực & Phân quyền),
 * trả về HTTP Status phù hợp cùng cấu trúc {@link ApiResponse} envelope chuẩn.
 *
 * <p>Quy ước `@Order(10)` để được ưu tiên kiểm tra TRƯỚC `WebExceptionHandler` (Order 20)
 * và `GlobalExceptionHandler` (LOWEST_PRECEDENCE) của common.
 */
@Slf4j
@RestControllerAdvice
@Order(10) // Xét đầu tiên trong toàn bộ advice chain
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityExceptionHandler {

    /**
     * Bắt lỗi xác thực không thành công (HTTP 401 Unauthorized).
     */
    @ExceptionHandler({
            UnauthorizedException.class,
            AuthenticationException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(Exception ex, HttpServletRequest request) {
        log.warn("[Security - Unauthorized] URI: {} | Message: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.UNAUTHORIZED.getCode(), ex.getMessage()));
    }

    /**
     * Bắt lỗi không đủ thẩm quyền truy cập tài nguyên (HTTP 403 Forbidden).
     */
    @ExceptionHandler({
            ForbiddenException.class,
            AccessDeniedException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleForbidden(Exception ex, HttpServletRequest request) {
        log.warn("[Security - Forbidden] URI: {} | Message: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.FORBIDDEN.getCode(), "Access denied: " + ex.getMessage()));
    }
}
