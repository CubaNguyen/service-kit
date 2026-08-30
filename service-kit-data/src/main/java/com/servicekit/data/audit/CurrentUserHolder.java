package com.servicekit.data.audit;

/**
 * Static holder lưu trữ CurrentUserProvider được đăng ký bởi tầng trên (service-kit-security).
 *
 * Giải pháp này tránh phụ thuộc compile-time vào spring-security trong service-kit-data.
 * Hibernate Envers tạo RevisionListener bằng reflection (không phải Spring IoC),
 * nên không thể inject Bean trực tiếp — dùng static holder là cách tiêu chuẩn.
 *
 * Cách đăng ký từ service-kit-security:
 * <pre>
 *   {@literal @}Component
 *   public class EnversSecurityBridge implements InitializingBean {
 *
 *       {@literal @}Override
 *       public void afterPropertiesSet() {
 *           CurrentUserHolder.setProvider(() -> {
 *               Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 *               if (auth != null && auth.isAuthenticated()
 *                       && !"anonymousUser".equals(auth.getPrincipal())) {
 *                   return auth.getName();
 *               }
 *               return null;
 *           });
 *       }
 *   }
 * </pre>
 */
public class CurrentUserHolder {

    private static volatile CurrentUserProvider provider;

    private CurrentUserHolder() {}

    public static void setProvider(CurrentUserProvider userProvider) {
        provider = userProvider;
    }

    public static CurrentUserProvider getProvider() {
        return provider;
    }
}
