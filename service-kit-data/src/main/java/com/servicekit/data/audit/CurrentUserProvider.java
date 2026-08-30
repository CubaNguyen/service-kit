package com.servicekit.data.audit;

/**
 * SPI Interface cho phép tầng bên trên (service-kit-security) tiêm logic đọc userId
 * vào RevisionListener mà không cần service-kit-data phụ thuộc vào spring-security.
 *
 * Cách đăng ký:
 * Trong service-kit-security (hoặc bất kỳ bean nào của Application):
 * <pre>
 *   {@literal @}Bean
 *   public CurrentUserProvider currentUserProvider() {
 *       return () -> {
 *           Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 *           return (auth != null && auth.isAuthenticated()) ? auth.getName() : null;
 *       };
 *   }
 * </pre>
 *
 * Nếu không có provider được đăng ký, RevisionListener fallback về "SYSTEM".
 */
@FunctionalInterface
public interface CurrentUserProvider {

    /**
     * Trả về ID/username của người dùng đang thực hiện request.
     * Trả về null nếu không xác định được (Background Job, Scheduler...).
     */
    String getCurrentUserId();
}
