package com.servicekit.web;

import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot Auto-Configuration entry point cho service-kit-web.
 * Được khai báo trong META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 * để Spring Boot tự động nạp khi module được thêm vào classpath.
 */
@Configuration
public class WebAutoConfiguration {
    // Bean registration handled by component scan of @RestControllerAdvice
    // All beans in com.servicekit.web are auto-discovered via this config
}
