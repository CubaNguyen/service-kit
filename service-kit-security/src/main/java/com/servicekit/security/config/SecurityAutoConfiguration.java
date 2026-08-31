package com.servicekit.security.config;

import com.servicekit.security.aspect.AuthorizationAspect;
import com.servicekit.security.filter.SecurityHeadersFilter;
import com.servicekit.security.filter.TokenAuthenticationFilter;
import com.servicekit.security.revocation.TokenRevocationStore;
import com.servicekit.security.token.TokenProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Lớp cấu hình tự động (Auto-Configuration) chính cho module bảo mật service-kit-security.
 *
 * <p>Cấu hình Spring Security:
 * - Vô hiệu hóa CSRF & Quản lý Session trạng thái (Stateless).
 * - Cho phép truy cập ẩn danh đối với các URL được cấu hình tại {@code permitAllUrls}.
 * - Thêm filter xác thực token {@link TokenAuthenticationFilter} vào chuỗi filter.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@Import({TokenProviderAutoConfiguration.class, RevocationStoreAutoConfiguration.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenAuthenticationFilter tokenAuthenticationFilter(
            TokenProvider tokenProvider, TokenRevocationStore revocationStore) {
        return new TokenAuthenticationFilter(tokenProvider, revocationStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationAspect authorizationAspect() {
        return new AuthorizationAspect();
    }

    @Bean
    @ConditionalOnMissingBean
    public com.servicekit.security.interceptor.AsyncContextPropagator asyncSecurityContextDecorator() {
        return new com.servicekit.security.interceptor.AsyncContextPropagator();
    }

    @Bean
    @ConditionalOnMissingBean
    public com.servicekit.security.util.PasswordEncoderUtil passwordEncoderUtil() {
        return new com.servicekit.security.util.PasswordEncoderUtil();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            TokenAuthenticationFilter tokenAuthenticationFilter,
            SecurityProperties securityProperties) throws Exception {

        String[] permitUrls = securityProperties.getPermitAllUrls().toArray(new String[0]);

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                if (permitUrls.length > 0) {
                    auth.requestMatchers(permitUrls).permitAll();
                }
                // Cho phép mặc định endpoint /error để Spring MVC chuyển tiếp bắt lỗi nâng cao ở WebExceptionHandler
                auth.requestMatchers("/error").permitAll();
                auth.anyRequest().authenticated();
            })
            .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Tự động cấu hình Feign client interceptor để chuyển tiếp JWT token khi gọi downstream service
     * chỉ khi dự án sử dụng OpenFeign.
     */
    @Configuration
    @ConditionalOnClass(feign.RequestInterceptor.class)
    public static class FeignInterceptorConfiguration {
        @Bean
        public feign.RequestInterceptor feignAuthInterceptor() {
            return new com.servicekit.security.interceptor.FeignAuthInterceptor();
        }
    }
}
