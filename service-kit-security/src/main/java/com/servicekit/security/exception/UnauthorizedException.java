package com.servicekit.security.exception;

/**
 * Exception ném ra khi request không có token hợp lệ hoặc chưa đăng nhập (HTTP 401).
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
}
