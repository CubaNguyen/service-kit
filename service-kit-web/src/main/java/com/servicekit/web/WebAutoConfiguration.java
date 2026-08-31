package com.servicekit.web;

import com.servicekit.web.config.WebProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot Auto-Configuration entry point cho service-kit-web.
 *
 * <p>Được khai báo trong:
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 *
 * <p><b>Phạm vi (Scope): Inbound HTTP Server Only.</b> Module này xử lý
 * request đi VÀO service. Outbound HTTP client (gọi service khác qua
 * RestTemplate/Feign/WebClient) thuộc phạm vi module {@code service-kit-client}
 * (planned) — không thuộc phạm vi module này.
 */
@Configuration
@ComponentScan("com.servicekit.web")
@EnableConfigurationProperties(WebProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebAutoConfiguration {
}
