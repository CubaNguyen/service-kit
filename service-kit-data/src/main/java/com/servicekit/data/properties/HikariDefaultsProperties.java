package com.servicekit.data.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "service-kit.datasource.hikari")
public class HikariDefaultsProperties {

    /**
     * Số lượng connection tối đa trong pool (mặc định: 10)
     */
    private int maximumPoolSize = 10;

    /**
     * Số lượng connection nhàn rỗi tối thiểu được duy trì (mặc định: 5)
     */
    private int minimumIdle = 5;

    /**
     * Thời gian (ms) tối đa connection nhàn rỗi được giữ trong pool (mặc định: 300,000ms = 5 phút)
     */
    private long idleTimeout = 300_000L;

    /**
     * Thời gian sống tối đa (ms) của một connection (mặc định: 1,800,000ms = 30 phút)
     */
    private long maxLifetime = 1_800_000L;

    /**
     * Thời gian (ms) client chờ lấy connection trước khi ném ngoại lệ (mặc định: 20,000ms = 20 giây)
     */
    private long connectionTimeout = 20_000L;

    /**
     * Thời gian (ms) connection bị mượn mà chưa trả trước khi cảnh báo rò rỉ (mặc định: 60,000ms = 1 phút)
     */
    private long leakDetectionThreshold = 60_000L;

    /**
     * Tên định danh của connection pool trong metrics/logs
     */
    private String poolName = "ServiceKitHikariPool";
}
