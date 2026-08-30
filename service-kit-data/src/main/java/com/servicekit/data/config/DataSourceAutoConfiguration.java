package com.servicekit.data.config;

import com.zaxxer.hikari.HikariDataSource;
import com.servicekit.data.properties.HikariDefaultsProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.AutoConfiguration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * Auto-Configuration cho HikariCP DataSource.
 *
 * Tự động kích hoạt khi có class DataSource trên classpath.
 * Áp dụng các giá trị mặc định tối ưu từ HikariDefaultsProperties.
 *
 * Để override, cấu hình trong application.yml:
 * <pre>
 *   service-kit:
 *     datasource:
 *       hikari:
 *         maximum-pool-size: 15
 *         leak-detection-threshold: 30000
 * </pre>
 *
 * P6Spy (Debug SQL) — Thêm dependency vào service muốn debug:
 * <pre>
 *   &lt;dependency&gt;
 *     &lt;groupId&gt;com.github.gavlyukovskiy&lt;/groupId&gt;
 *     &lt;artifactId&gt;p6spy-spring-boot-starter&lt;/artifactId&gt;
 *     &lt;version&gt;1.9.2&lt;/version&gt;
 *     &lt;scope&gt;runtime&lt;/scope&gt;  &lt;!-- Chỉ bật ở dev, không đưa lên production! --&gt;
 *   &lt;/dependency&gt;
 * </pre>
 *
 * Micrometer HikariCP Metrics — Thêm vào application.yml:
 * <pre>
 *   management:
 *     metrics:
 *       enable:
 *         hikaricp: true
 * </pre>
 */
@AutoConfiguration
@ConditionalOnClass(HikariDataSource.class)
@EnableConfigurationProperties(HikariDefaultsProperties.class)
@RequiredArgsConstructor
public class DataSourceAutoConfiguration {

    private final HikariDefaultsProperties properties;
    private final HikariDataSource hikariDataSource;

    @PostConstruct
    public void applyHikariDefaults() {
        hikariDataSource.setMaximumPoolSize(properties.getMaximumPoolSize());
        hikariDataSource.setMinimumIdle(properties.getMinimumIdle());
        hikariDataSource.setConnectionTimeout(properties.getConnectionTimeout());
        hikariDataSource.setIdleTimeout(properties.getIdleTimeout());
        hikariDataSource.setMaxLifetime(properties.getMaxLifetime());
        hikariDataSource.setLeakDetectionThreshold(properties.getLeakDetectionThreshold());
        if (properties.getPoolName() != null && !properties.getPoolName().isBlank()) {
            hikariDataSource.setPoolName(properties.getPoolName());
        }
    }
}

