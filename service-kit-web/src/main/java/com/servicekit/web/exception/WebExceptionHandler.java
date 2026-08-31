package com.servicekit.web.exception;

import com.servicekit.common.exception.ErrorCode;
import com.servicekit.common.response.ApiResponse;
import com.servicekit.common.response.FieldErrorDetail;
import com.servicekit.common.util.TimeUtils;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * ControllerAdvice xử lý toàn bộ exception HTTP nâng cao của tầng Web,
 * đảm bảo mọi lỗi HTTP đều được map về đúng chuẩn {@link ApiResponse} envelope —
 * không để bất kỳ case nào trả về trang HTML Whitelabel hay JSON sai format.
 *
 * <h3>Phạm vi (Scope): Inbound HTTP Server Only</h3>
 * <p>Module này xử lý các request đi VÀO service (inbound). Các lỗi liên quan
 * đến gọi HTTP ra service khác (RestTemplate, Feign, WebClient) thuộc phạm vi
 * của module {@code service-kit-client} (planned) — không nhét vào đây.
 *
 * <h3>Quy ước Thứ Tự Ưu Tiên (@Order Hierarchy):</h3>
 * <ul>
 *   <li>{@code @Order(10)}: {@code SecurityExceptionHandler} (module {@code service-kit-security}) —
 *       Bắt {@code AuthenticationException} (401), {@code AccessDeniedException} (403).</li>
 *   <li>{@code @Order(20)}: {@link WebExceptionHandler} (module {@code service-kit-web}) —
 *       Bắt các lỗi HTTP hạ tầng web (400 validation param/type, 404 route, 405 method, 409 lock, 413 upload).</li>
 *   <li>{@code @Order(Ordered.LOWEST_PRECEDENCE)}: {@code GlobalExceptionHandler} (module {@code service-kit-common}) —
 *       Fallback kernel: bắt {@link MethodArgumentNotValidException} (body JSON),
 *       business exceptions và catch-all {@link Exception} (500).</li>
 * </ul>
 *
 * <h3>Guard @ConditionalOnWebApplication:</h3>
 * <p>Nhất quán với pattern của toàn bộ service-kit: class này chỉ được nạp
 * khi ứng dụng là Servlet Web Application, bảo vệ các Worker/Consumer Service.
 */
@Slf4j
@RestControllerAdvice
@Order(20) // Quy ước chuẩn: Security=10, Web=20, Common=LOWEST_PRECEDENCE
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebExceptionHandler {

    // ─────────────────────────────────────────────────────────────────────────
    // NHÓM 1: Concurrency — Optimistic Locking (HTTP 409)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Bắt lỗi Optimistic Locking Conflict và trả về HTTP 409 Conflict.
     *
     * <p>Bắt cả 2 loại exception tùy ngữ cảnh gọi:
     * <ul>
     *   <li>{@link ObjectOptimisticLockingFailureException} — qua Spring Data {@code repository.save()}</li>
     *   <li>{@link OptimisticLockException} — qua {@code EntityManager} trực tiếp</li>
     * </ul>
     */
    @ExceptionHandler({
            ObjectOptimisticLockingFailureException.class,
            OptimisticLockException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockConflict(
            Exception ex, HttpServletRequest request) {

        log.warn("[Optimistic Lock Conflict] URI: {} | Exception: {} | Message: {}",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ErrorCode.CONFLICT.getCode(),
                        "The resource was modified by another request. Please retry."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NHÓM 2: Routing — Route / Method không tồn tại (HTTP 404 / 405)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Bắt lỗi Route không tồn tại (HTTP 404 Not Found).
     *
     * <p>Spring Boot 3.4.x (Spring Framework 6.2+) tự động ném {@link NoResourceFoundException}
     * khi không tìm thấy Controller / static resource handler tương ứng.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            NoResourceFoundException ex, HttpServletRequest request) {

        log.warn("[Not Found] URI: {} | Resource: {}", request.getRequestURI(), ex.getResourcePath());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorCode.NOT_FOUND.getCode(),
                        "The requested resource was not found: " + request.getRequestURI()));
    }

    /**
     * Bắt lỗi HTTP Method không được hỗ trợ trên route tồn tại (HTTP 405 Method Not Allowed).
     *
     * <p>Ví dụ: route chỉ khai báo {@code @GetMapping} nhưng client gọi {@code POST}.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        log.warn("[Method Not Allowed] URI: {} | Method: {} | Supported: {}",
                request.getRequestURI(), ex.getMethod(), ex.getSupportedHttpMethods());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(405,
                        "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NHÓM 3: Request Data — Dữ liệu gửi lên sai định dạng / Validate tham số (HTTP 400)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Bắt lỗi validate tham số tại method-level khi dùng {@code @Validated} trên Controller
     * (ví dụ: {@code @Min(1) @RequestParam Integer page}, {@code @NotBlank @PathVariable String code}).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<FieldErrorDetail>>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<FieldErrorDetail> errors = ex.getConstraintViolations().stream()
                .map(violation -> {
                    String path = violation.getPropertyPath().toString();
                    String fieldName = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                    return FieldErrorDetail.builder()
                            .field(fieldName)
                            .rejectedValue(violation.getInvalidValue())
                            .message(violation.getMessage())
                            .build();
                })
                .toList();

        log.warn("[Constraint Violation] URI: {} | Invalid fields count: {}", request.getRequestURI(), errors.size());

        ApiResponse<List<FieldErrorDetail>> body = ApiResponse.<List<FieldErrorDetail>>builder()
                .code(ErrorCode.BAD_REQUEST.getCode())
                .message("Invalid request parameters")
                .data(errors)
                .timestamp(TimeUtils.nowEpochMilli())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Bắt lỗi validation method parameter built-in của Spring Boot 3.2+ (Spring 6.1+).
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<List<FieldErrorDetail>>> handleHandlerMethodValidation(
            HandlerMethodValidationException ex, HttpServletRequest request) {

        List<FieldErrorDetail> errors = ex.getValueResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream().map(error -> FieldErrorDetail.builder()
                        .field(result.getMethodParameter().getParameterName())
                        .rejectedValue(result.getArgument())
                        .message(error.getDefaultMessage())
                        .build()))
                .toList();

        log.warn("[Method Validation Error] URI: {} | Invalid fields count: {}", request.getRequestURI(), errors.size());

        ApiResponse<List<FieldErrorDetail>> body = ApiResponse.<List<FieldErrorDetail>>builder()
                .code(ErrorCode.BAD_REQUEST.getCode())
                .message("Invalid request parameters")
                .data(errors)
                .timestamp(TimeUtils.nowEpochMilli())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Bắt lỗi tham số đường dẫn hoặc query param sai kiểu dữ liệu (HTTP 400).
     *
     * <p>Ví dụ: route yêu cầu {@code UUID} nhưng client gửi {@code /api/products/abc}.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String paramName = ex.getName();
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String actualValue = String.valueOf(ex.getValue());

        log.warn("[Type Mismatch] URI: {} | Param: '{}' | Expected: {} | Got: '{}'",
                request.getRequestURI(), paramName, expectedType, actualValue);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.BAD_REQUEST.getCode(),
                        "Parameter '" + paramName + "' must be of type '" + expectedType
                                + "', but received: '" + actualValue + "'"));
    }

    /**
     * Bắt lỗi JSON body gửi lên bị lỗi cú pháp, không thể parse (HTTP 400).
     *
     * <p>Ví dụ: client gửi {@code {"name": "test",}} (dư dấu phẩy — JSON không hợp lệ).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJson(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("[Malformed JSON] URI: {} | Message: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.BAD_REQUEST.getCode(),
                        "Request body is malformed or contains invalid JSON syntax."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NHÓM 4: Upload — File quá lớn (HTTP 413)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Bắt lỗi upload file vượt quá giới hạn được cấu hình (HTTP 413 Payload Too Large).
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {

        log.warn("[Upload Too Large] URI: {} | Max: {} bytes", request.getRequestURI(), ex.getMaxUploadSize());

        String limitInfo = ex.getMaxUploadSize() > 0
                ? " (limit: " + (ex.getMaxUploadSize() / 1024 / 1024) + "MB)"
                : "";

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(413,
                        "Uploaded file exceeds the maximum allowed size" + limitInfo + "."));
    }
}
