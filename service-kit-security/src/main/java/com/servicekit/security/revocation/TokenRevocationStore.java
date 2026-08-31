package com.servicekit.security.revocation;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Strategy interface định nghĩa cơ chế lưu trữ và kiểm tra token bị thu hồi.
 */
public interface TokenRevocationStore {

    /**
     * Thu hồi 1 token/phiên cụ thể (blacklist jti).
     *
     * @param jti JWT ID của token cần thu hồi
     * @param ttl thời gian sống còn lại của token (để dọn dẹp bộ nhớ store khi hết hạn tự nhiên)
     */
    void revokeToken(String jti, Duration ttl);

    /**
     * Kiểm tra xem token này đã bị thu hồi đơn lẻ hay chưa.
     *
     * @param jti JWT ID của token
     * @return true nếu token nằm trong blacklist, ngược lại false
     */
    boolean isTokenRevoked(String jti);

    /**
     * Thu hồi toàn bộ token hiện có của user (ví dụ: khi user đổi mật khẩu).
     *
     * @param userId ID của người dùng
     * @param cutoff mốc thời gian cutoff; mọi token phát sinh trước mốc này bị coi là không hợp lệ
     */
    void revokeAllTokensForUser(UUID userId, Instant cutoff);

    /**
     * Lấy mốc thời gian cutoff thu hồi của người dùng.
     *
     * @param userId ID của người dùng
     * @return mốc cutoff nếu có, ngược lại null
     */
    Instant getRevokeAllCutoff(UUID userId);
}
