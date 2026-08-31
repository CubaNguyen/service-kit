package com.servicekit.security.token;

import com.servicekit.security.context.AuthContext;

/**
 * Strategy interface định nghĩa các hành vi cấp phát và giải mã token.
 */
public interface TokenProvider {

    /**
     * Phát sinh token (JWT/Paseto...) từ thông tin của {@link AuthContext}.
     * Chỉ khả dụng khi auth-mode là FULL.
     *
     * @param context thông tin định danh & quyền hạn
     * @return chuỗi token hoàn chỉnh
     * @throws UnsupportedOperationException nếu auth-mode là VERIFY_ONLY
     */
    String generateToken(AuthContext context);

    /**
     * Giải mã và chuyển đổi chuỗi token thành {@link AuthContext}.
     *
     * @param token chuỗi token nhận được từ request header
     * @return {@link AuthContext} nếu token hợp lệ
     * @throws RuntimeException nếu giải mã lỗi hoặc token hết hạn
     */
    AuthContext parseToken(String token);

    /**
     * Kiểm tra tính hợp lệ cơ bản của token (chữ ký, thời gian hết hạn).
     *
     * @param token chuỗi token cần kiểm tra
     * @return true nếu token hợp lệ và chưa hết hạn, ngược lại false
     */
    boolean validateToken(String token);
}
