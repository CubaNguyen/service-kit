package com.servicekit.security.context;

/**
 * Quản lý {@link AuthContext} trong phạm vi của một Thread (ThreadLocal).
 */
public class AuthContextHolder {

    private static final ThreadLocal<AuthContext> contextHolder = new ThreadLocal<>();

    public static void setContext(AuthContext context) {
        contextHolder.set(context);
    }

    public static AuthContext getContext() {
        return contextHolder.get();
    }

    public static void clearContext() {
        contextHolder.remove();
    }
}
