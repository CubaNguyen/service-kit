package com.servicekit.web.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

/**
 * Cấu hình Pagination Guard — giới hạn {@code pageSize} tối đa để chống DB overload.
 *
 * <p><b>Cơ chế an toàn (Customizer Pattern):</b> Sử dụng bean chuẩn
 * {@link PageableHandlerMethodArgumentResolverCustomizer} của Spring Data Web.
 * {@code SpringDataWebAutoConfiguration} của Spring Boot sẽ tự động thu thập
 * customizer này và áp dụng {@code setMaxPageSize()} lên resolver mặc định,
 * bảo đảm an toàn 100% bất kể thứ tự khởi tạo bean và không đè hay thay thế bean gốc.
 *
 * <p>Chỉ kích hoạt khi {@code PageableHandlerMethodArgumentResolverCustomizer}
 * có trên classpath (tức service đã import {@code service-kit-data} hoặc Spring Data).
 *
 * <p>Cấu hình qua {@code application.yml}:
 * <pre>
 * service-kit:
 *   web:
 *     pagination:
 *       max-page-size: 200  # Mặc định: 100
 * </pre>
 */
@Configuration
@ConditionalOnClass(PageableHandlerMethodArgumentResolverCustomizer.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class PaginationGuardConfiguration {

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer serviceKitPageableCustomizer(WebProperties webProperties) {
        return resolver -> resolver.setMaxPageSize(webProperties.getPagination().getMaxPageSize());
    }
}
