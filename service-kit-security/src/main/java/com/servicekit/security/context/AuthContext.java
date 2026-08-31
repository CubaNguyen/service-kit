package com.servicekit.security.context;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * POJO/Record chứa thông tin định danh, phân quyền và dữ liệu kiểm tra thu hồi (revocation) của user.
 */
public record AuthContext(
        UUID userId,
        String tenantId,       // nullable — tự động gán từ Tenant context
        Set<String> roles,
        Set<String> permissions,
        String jti,            // Bắt buộc nếu dùng revocation, dùng để nhận diện & khóa token đơn lẻ
        Instant issuedAt       // Bắt buộc nếu dùng revoke-all-cutoff
) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}
