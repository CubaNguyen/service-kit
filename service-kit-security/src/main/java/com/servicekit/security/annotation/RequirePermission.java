package com.servicekit.security.annotation;

import java.lang.annotation.*;

/**
 * Annotation ràng buộc quyền truy cập theo chi tiết hành động (Permission/Privilege) trên Class hoặc Method.
 *
 * <p>Ví dụ: {@code @RequirePermission("product:write")}
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * Danh sách các permission được phép truy cập.
     * Chỉ cần người dùng sở hữu ÍT NHẤT một trong các permission này (OR condition) là được phép truy cập.
     */
    String[] value();
}
