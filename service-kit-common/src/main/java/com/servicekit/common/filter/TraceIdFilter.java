package com.servicekit.common.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // Bắt buộc chạy đầu tiên
public class TraceIdFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Correlation-Id";
    private static final String MDC_TRACE_ID_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Trích xuất từ request (khi gọi chéo service) hoặc tự tạo mới
        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        // Đưa vào MDC để SLF4J tự động nhúng vào log pattern
        MDC.put(MDC_TRACE_ID_KEY, traceId);
        
        // Đính kèm vào response header để client tracking
        httpResponse.setHeader(TRACE_ID_HEADER, traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            // [Quan trọng] Dọn dẹp để tránh rò rỉ bộ nhớ MDC trên Thread Pool
            MDC.remove(MDC_TRACE_ID_KEY);
        }
    }
}