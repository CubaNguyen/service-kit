package com.servicekit.security.token;

import java.util.Map;
import com.servicekit.security.context.AuthContext;

/**
 * Interface cho phép các microservices tùy biến claims của JWT trước khi generate.
 * Ví dụ: thêm storeId, branchCode, email... vào JWT.
 */
public interface TokenClaimsCustomizer {
    /**
     * Tùy chỉnh danh sách claims được nhét vào JWT.
     *
     * @param claims map chứa claims hiện tại (sẽ được ghi vào JWT)
     * @param context context chứa thông tin auth của user đang cần gen token
     */
    void customize(Map<String, Object> claims, AuthContext context);
}
