package com.servicekit.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Cấu hình tham số bảo mật của service-kit-security.
 * prefix: service-kit.security
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "service-kit.security")
public class SecurityProperties {

    /**
     * Chế độ hoạt động:
     * - FULL: Vừa ký phát sinh token (cần Private Key) vừa verify token (cần Public Key).
     * - VERIFY_ONLY: Chỉ verify token (chỉ cần Public Key).
     */
    private AuthMode authMode = AuthMode.VERIFY_ONLY;

    /**
     * Cơ chế lưu trữ danh sách token/user bị thu hồi (revocation store):
     * - NONE: Không kiểm tra thu hồi (NoOp).
     * - IN_MEMORY: Lưu trên bộ nhớ cục bộ instance (Guava / Cache).
     * - REDIS: Đồng bộ qua Redis (yêu cầu dự án import starter-data-redis).
     */
    private RevocationStoreType revocationStore = RevocationStoreType.NONE;

    private final Jwt jwt = new Jwt();

    /**
     * Danh sách các URL path được phép truy cập tự do (không cần token).
     */
    private List<String> permitAllUrls = new ArrayList<>();

    @Getter
    @Setter
    public static class Jwt {
        /**
         * Path tới file chứa RSA Private Key (định dạng PEM PKCS#8).
         * Chỉ bắt buộc khi authMode = FULL.
         */
        private String privateKeyPath;

        /**
         * Path tới file chứa RSA Public Key (định dạng PEM X.509/PKCS#8).
         * Bắt buộc với mọi authMode.
         */
        private String publicKeyPath;

        /**
         * Thuật toán ký số asymmetric. Mặc định: RS256.
         */
        private String algorithm = "RS256";

        /**
         * Thời gian hết hạn của Access Token (giây). Mặc định: 1 giờ (3600s).
         */
        private long expirationSeconds = 3600;
    }

    public enum AuthMode {
        FULL,
        VERIFY_ONLY
    }

    public enum RevocationStoreType {
        NONE,
        IN_MEMORY,
        REDIS
    }
}
