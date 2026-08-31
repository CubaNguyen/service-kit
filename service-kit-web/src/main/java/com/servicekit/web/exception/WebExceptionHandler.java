package com.servicekit.web.exception;

import com.servicekit.common.exception.ErrorCode;
import com.servicekit.common.response.ApiResponse;
import com.servicekit.common.util.TimeUtils;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ControllerAdvice xử lý các exception HTTP nâng cao của tầng Web,
 * đặc biệt là lỗi Optimistic Locking Conflict (HTTP 409).
 *
 * <p><b>Thiết kế ưu tiên:</b> @Order(10) đảm bảo class này được Spring xét TRƯỚC
 * {@code GlobalExceptionHandler} ở service-kit-common (không có @Order → mặc định
 * {@code Ordered.LOWEST_PRECEDENCE}). Nhờ đó, các exception được handle ở đây
 * sẽ không bị rơi xuống catch-all của common và trả về lỗi 500.
 *
 * <p><b>Guard:</b> @ConditionalOnWebApplication(SERVLET) giữ nhất quán với
 * pattern của toàn bộ service-kit — class này chỉ nạp khi ứng dụng là
 * Servlet Web Application.
 *
 * <h3>Tại sao bắt cả 2 exception?</h3>
 * <ul>
 *   <li>{@link ObjectOptimisticLockingFailureException} — lộ ra khi dùng
 *       {@code repository.save(entity)} qua Spring Data JPA (phổ biến nhất).</li>
 *   <li>{@link OptimisticLockException} — lộ ra khi gọi thẳng {@code EntityManager}
 *       hoặc JPQL update có đụng field {@code @Version}.</li>
 * </ul>
 *
 * <p><b>TODO — Cần verify bằng integration test trước khi release:</b><br>
 * Khi Hibernate defer flush đến commit của {@code JpaTransactionManager.doCommit()},
 * exception thật sự lộ ra tại {@code @ExceptionHandler} có thể được bọc trong
 * {@code TransactionSystemException}. Cần viết concurrent integration test
 * (2 transaction cùng load + save 1 entity) để in ra {@code ex.getClass()} thật
 * và xác nhận handler này bắt đúng — trước khi triển khai production.
 */
@Slf4j
@RestControllerAdvice
@Order(10) // Xét TRƯỚC GlobalExceptionHandler của common (không có @Order → LOWEST_PRECEDENCE)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebExceptionHandler {

    /**
     * Bắt lỗi Optimistic Locking Conflict và trả về HTTP 409.
     *
     * <p>Bắt cả 2 loại exception có thể xảy ra tùy ngữ cảnh gọi:
     * <ul>
     *   <li>{@link ObjectOptimisticLockingFailureException} — qua Spring Data repository</li>
     *   <li>{@link OptimisticLockException} — qua EntityManager trực tiếp</li>
     * </ul>
     *
     * <p><b>Response body:</b> Dùng lại {@link ErrorCode#CONFLICT} và {@link ApiResponse}
     * chuẩn của service-kit-common — không tạo response shape riêng để client
     * không phải xử lý 2 kiểu error body khác nhau.
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

        ApiResponse<Void> body = ApiResponse.error(
                ErrorCode.CONFLICT.getCode(),
                "The resource was modified by another request. Please retry."
        );

        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }
}
