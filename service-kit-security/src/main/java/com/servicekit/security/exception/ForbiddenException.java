package com.servicekit.security.exception;

/**
 * Exception ném ra khi người dùng không có đủ quyền/vai trò để truy cập tài nguyên (HTTP 403).
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
