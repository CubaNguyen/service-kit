package com.servicekit.security.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Wrapper tiện ích mã hóa mật khẩu, sử dụng thuật toán BCrypt mặc định từ Spring Security.
 */
public class PasswordEncoderUtil {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Mã hóa mật khẩu thô.
     *
     * @param rawPassword mật khẩu chưa mã hóa
     * @return chuỗi hash mật khẩu
     */
    public String encode(CharSequence rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * So khớp mật khẩu thô với chuỗi hash đã lưu trữ.
     *
     * @param rawPassword mật khẩu chưa mã hóa
     * @param encodedPassword chuỗi hash mật khẩu đã lưu
     * @return true nếu mật khẩu trùng khớp, ngược lại false
     */
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * Lấy thực thể PasswordEncoder gốc.
     */
    public PasswordEncoder getEncoder() {
        return passwordEncoder;
    }
}
