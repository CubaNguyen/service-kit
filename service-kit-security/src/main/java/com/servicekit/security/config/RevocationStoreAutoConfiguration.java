package com.servicekit.security.config;

import com.servicekit.security.revocation.InMemoryRevocationStore;
import com.servicekit.security.revocation.NoOpRevocationStore;
import com.servicekit.security.revocation.RedisRevocationStore;
import com.servicekit.security.revocation.TokenRevocationStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Auto-Configuration nạp bean {@link TokenRevocationStore} phù hợp dựa trên cấu hình properties.
 */
@Configuration
public class RevocationStoreAutoConfiguration {

    /**
     * Nạp NoOp Store nếu cấu hình là NONE hoặc không cấu hình (mặc định).
     */
    @Bean
    @ConditionalOnProperty(name = "service-kit.security.revocation-store", havingValue = "NONE", matchIfMissing = true)
    @ConditionalOnMissingBean(TokenRevocationStore.class)
    public TokenRevocationStore noOpRevocationStore() {
        return new NoOpRevocationStore();
    }

    /**
     * Nạp InMemory Store nếu cấu hình là IN_MEMORY.
     */
    @Bean
    @ConditionalOnProperty(name = "service-kit.security.revocation-store", havingValue = "IN_MEMORY")
    @ConditionalOnMissingBean(TokenRevocationStore.class)
    public TokenRevocationStore inMemoryRevocationStore() {
        return new InMemoryRevocationStore();
    }

    /**
     * Chỉ nạp Redis Store khi đồng thời thỏa mãn:
     * 1. Thuộc tính revocation-store cấu hình là REDIS.
     * 2. Thư viện Spring Data Redis có sẵn trên classpath.
     */
    @Configuration
    @ConditionalOnProperty(name = "service-kit.security.revocation-store", havingValue = "REDIS")
    @ConditionalOnClass(name = "org.springframework.data.redis.core.RedisTemplate")
    public static class RedisRevocationConfiguration {

        @Bean
        @ConditionalOnMissingBean(TokenRevocationStore.class)
        public TokenRevocationStore redisRevocationStore(StringRedisTemplate redisTemplate) {
            return new RedisRevocationStore(redisTemplate);
        }
    }
}
