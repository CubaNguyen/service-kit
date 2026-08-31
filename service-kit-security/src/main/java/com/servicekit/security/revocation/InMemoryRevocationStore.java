package com.servicekit.security.revocation;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Triển khai lưu trữ danh sách thu hồi token trên bộ nhớ cục bộ (RAM) của JVM instance.
 * Phù hợp cho môi trường local development, testing, hoặc các monolith đơn lẻ.
 */
public class InMemoryRevocationStore implements TokenRevocationStore {

    // jti -> Expiry time of the token
    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

    // userId -> Cutoff timestamp
    private final Map<UUID, Instant> userCutoffs = new ConcurrentHashMap<>();

    @Override
    public void revokeToken(String jti, Duration ttl) {
        if (jti != null && ttl != null) {
            blacklist.put(jti, Instant.now().plus(ttl));
        }
    }

    @Override
    public boolean isTokenRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        Instant expiry = blacklist.get(jti);
        if (expiry == null) {
            return false;
        }
        if (Instant.now().isAfter(expiry)) {
            blacklist.remove(jti); // Dọn dẹp token đã hết hạn tự nhiên
            return false;
        }
        return true;
    }

    @Override
    public void revokeAllTokensForUser(UUID userId, Instant cutoff) {
        if (userId != null && cutoff != null) {
            userCutoffs.put(userId, cutoff);
        }
    }

    @Override
    public Instant getRevokeAllCutoff(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userCutoffs.get(userId);
    }
}
