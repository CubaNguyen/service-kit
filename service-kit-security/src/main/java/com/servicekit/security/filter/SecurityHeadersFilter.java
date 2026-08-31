package com.servicekit.security.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Filter tự động thêm các HTTP Security Headers tiêu chuẩn vào HTTP Response
 * giúp bảo vệ ứng dụng khỏi các lỗ hổng phổ biến (XSS, Clickjacking, MIME-sniffing...).
 */
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (response instanceof HttpServletResponse httpResponse) {
            // Chặn clickjacking (không cho phép hiển thị trang trong iframe)
            httpResponse.setHeader("X-Frame-Options", "DENY");

            // Chặn MIME type sniffing (yêu cầu trình duyệt tuân thủ Content-Type được trả về)
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");

            // Kích hoạt XSS Filter của trình duyệt và tự động chặn trang nếu phát hiện tấn công
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

            // Ép buộc trình duyệt sử dụng kết nối HTTPS (HSTS) - 1 năm
            httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

            // Kiểm soát thông tin referrer gửi đi khi click link sang trang khác
            httpResponse.setHeader("Referrer-Policy", "no-referrer-when-downgrade");

            // Content-Security-Policy tối thiểu an toàn mặc định
            httpResponse.setHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none';");
        }

        chain.doFilter(request, response);
    }
}
