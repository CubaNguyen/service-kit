package com.servicekit.security.interceptor;

import com.servicekit.security.context.AuthContext;
import com.servicekit.security.context.AuthContextHolder;
import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * TaskDecorator giúp sao chép (propagate) bối cảnh bảo mật (AuthContext và SecurityContext)
 * từ luồng cha (Parent Thread) sang luồng con (Child Thread) khi thực hiện các tác vụ bất đồng bộ (@Async).
 */
public class AsyncContextPropagator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // 1. Capture context từ luồng cha trước khi spawn luồng con
        AuthContext authContext = AuthContextHolder.getContext();
        SecurityContext securityContext = SecurityContextHolder.getContext();

        return () -> {
            try {
                // 2. Thiết lập context cho luồng con
                if (authContext != null) {
                    AuthContextHolder.setContext(authContext);
                }
                if (securityContext != null) {
                    SecurityContextHolder.setContext(securityContext);
                }

                // 3. Thực thi task chính
                runnable.run();
            } finally {
                // 4. Đảm bảo dọn dẹp sạch sẽ luồng con sau khi hoàn thành
                AuthContextHolder.clearContext();
                SecurityContextHolder.clearContext();
            }
        };
    }
}
