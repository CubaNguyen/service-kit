package com.servicekit.security.annotation;

import java.lang.annotation.*;

/**
 * Annotation ràng buộc quyền truy cập theo vai trò (Role) trên Class hoặc Method.
 *
 * <p>Ví dụ: {@code @RequireRole("ADMIN")}, {@code @RequireRole({"ADMIN", "MANAGER"})}
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * Danh sách các vai trò được phép truy cập.
     * Chỉ cần người dùng sở hữu ÍT NHẤT một trong các vai trò này (OR condition) là được phép truy cập.
     */
    String[] value();
}
