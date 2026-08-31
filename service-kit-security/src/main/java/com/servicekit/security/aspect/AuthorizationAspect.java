package com.servicekit.security.aspect;

import com.servicekit.security.annotation.RequirePermission;
import com.servicekit.security.annotation.RequireRole;
import com.servicekit.security.context.AuthContext;
import com.servicekit.security.context.AuthContextHolder;
import com.servicekit.security.exception.ForbiddenException;
import com.servicekit.security.exception.UnauthorizedException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * AOP Aspect chịu trách nhiệm chặn các phương thức được đánh dấu {@link RequireRole}
 * hoặc {@link RequirePermission} để kiểm tra quyền hạn của user trong {@link AuthContextHolder}.
 */
@Aspect
public class AuthorizationAspect {

    @Before("@annotation(com.servicekit.security.annotation.RequireRole) || @within(com.servicekit.security.annotation.RequireRole)")
    public void checkRole(JoinPoint joinPoint) {
        AuthContext context = AuthContextHolder.getContext();
        if (context == null) {
            throw new UnauthorizedException("Authentication is required to access this resource");
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 1. Lấy annotation từ method trước, nếu không có thì lấy từ class level
        RequireRole annotation = method.getAnnotation(RequireRole.class);
        if (annotation == null) {
            annotation = method.getDeclaringClass().getAnnotation(RequireRole.class);
        }

        // 2. Kiểm tra vai trò
        if (annotation != null) {
            boolean hasAnyRole = Arrays.stream(annotation.value())
                    .anyMatch(context::hasRole);
            if (!hasAnyRole) {
                throw new ForbiddenException("Access denied. Required role(s): " + Arrays.toString(annotation.value()));
            }
        }
    }

    @Before("@annotation(com.servicekit.security.annotation.RequirePermission) || @within(com.servicekit.security.annotation.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        AuthContext context = AuthContextHolder.getContext();
        if (context == null) {
            throw new UnauthorizedException("Authentication is required to access this resource");
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 1. Lấy annotation từ method trước, nếu không có thì lấy từ class level
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);
        if (annotation == null) {
            annotation = method.getDeclaringClass().getAnnotation(RequirePermission.class);
        }

        // 2. Kiểm tra quyền hạn
        if (annotation != null) {
            boolean hasAnyPermission = Arrays.stream(annotation.value())
                    .anyMatch(context::hasPermission);
            if (!hasAnyPermission) {
                throw new ForbiddenException("Access denied. Required permission(s): " + Arrays.toString(annotation.value()));
            }
        }
    }
}
