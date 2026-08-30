package com.servicekit.security.context;

public final class AuthContextHolder {

    private static final ThreadLocal<AuthContext> CONTEXT = new ThreadLocal<>();

    private AuthContextHolder() {}

    public static void setContext(AuthContext authContext) {
        CONTEXT.set(authContext);
    }

    public static AuthContext getContext() {
        return CONTEXT.get();
    }

    public static String getUserId() {
        AuthContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getUserId() : null;
    }

    public static String getEmail() {
        AuthContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getEmail() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
