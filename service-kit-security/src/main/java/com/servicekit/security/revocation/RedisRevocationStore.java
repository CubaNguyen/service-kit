package com.servicekit.security.revocation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Triển khai {@link TokenRevocationStore} sử dụng Redis để chia sẻ danh sách thu hồi
 * (blacklist jti & user cutoff timestamp) giữa các instance của microservices.
 *
 * <p>Chỉ kích hoạt khi {@code service-kit.security.revocation-store} là {@code REDIS}
 * và {@code StringRedisTemplate} tồn tại trong Spring ApplicationContext.
 */
@RequiredArgsConstructor
public class RedisRevocationStore implements TokenRevocationStore {

    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_TOKEN_PREFIX = "blacklist:token:";
    private static final String BLACKLIST_USER_PREFIX = "blacklist:user:";

    @Override
    public void revokeToken(String jti, Duration ttl) {
        if (jti != null && ttl != null && !ttl.isNegative()) {
            redisTemplate.opsForValue().set(BLACKLIST_TOKEN_PREFIX + jti, "revoked", ttl);
        }
    }

    @Override
    public boolean isTokenRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_TOKEN_PREFIX + jti));
    }

    @Override
    public void revokeAllTokensForUser(UUID userId, Instant cutoff) {
        if (userId != null && cutoff != null) {
            redisTemplate.opsForValue().set(BLACKLIST_USER_PREFIX + userId, String.valueOf(cutoff.toEpochMilli()));
        }
    }

    @Override
    public Instant getRevokeAllCutoff(UUID userId) {
        if (userId == null) {
            return null;
        }
        String val = redisTemplate.opsForValue().get(BLACKLIST_USER_PREFIX + userId);
        if (val == null) {
            return null;
        }
        try {
            return Instant.ofEpochMilli(Long.parseLong(val));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
