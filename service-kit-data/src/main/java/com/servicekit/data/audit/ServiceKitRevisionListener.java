package com.servicekit.data.audit;

import org.hibernate.envers.RevisionListener;

/**
 * Listener gán người dùng hiện tại vào mỗi Revision của Hibernate Envers.
 *
 * Cơ chế hoạt động:
 * - Đọc userId từ CurrentUserProvider được đăng ký vào CurrentUserHolder.
 * - Nếu không có provider → fallback về "SYSTEM" (Background Job, Migration...).
 *
 * Luồng tích hợp với service-kit-security:
 * <pre>
 *   service-kit-security định nghĩa Bean CurrentUserProvider {
 *       return () -> SecurityContextHolder.getContext().getAuthentication().getName();
 *   }
 *
 *   Bean đó gọi CurrentUserHolder.setProvider(provider) khi khởi động.
 * </pre>
 *
 * @see CurrentUserProvider
 * @see CurrentUserHolder
 */
public class ServiceKitRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        ServiceKitRevisionEntity revision = (ServiceKitRevisionEntity) revisionEntity;
        revision.setModifiedBy(resolveCurrentUserId());
    }

    private String resolveCurrentUserId() {
        try {
            CurrentUserProvider provider = CurrentUserHolder.getProvider();
            if (provider != null) {
                String userId = provider.getCurrentUserId();
                return (userId != null && !userId.isBlank()) ? userId : "SYSTEM";
            }
        } catch (Exception ignored) {
            // Provider bị lỗi → an toàn fallback
        }
        return "SYSTEM";
    }
}
