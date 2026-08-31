package com.servicekit.security.revocation;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Triển khai mặc định của {@link TokenRevocationStore} khi tắt tính năng kiểm tra thu hồi (NONE).
 * Luôn trả về kết quả giả định là token không bị thu hồi.
 */
public class NoOpRevocationStore implements TokenRevocationStore {

    @Override
    public void revokeToken(String jti, Duration ttl) {
        // no-op
    }

    @Override
    public boolean isTokenRevoked(String jti) {
        return false;
    }

    @Override
    public void revokeAllTokensForUser(UUID userId, Instant cutoff) {
        // no-op
    }

    @Override
    public Instant getRevokeAllCutoff(UUID userId) {
        return null;
    }
}
