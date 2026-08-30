package com.servicekit.common.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice(basePackages = "com.servicekit") 
@Order(Ordered.LOWEST_PRECEDENCE) 
public class GlobalResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return !(returnType.hasMethodAnnotation(IgnoreResponseAdvice.class) || 
                 returnType.getDeclaringClass().isAnnotationPresent(IgnoreResponseAdvice.class));
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        
        int status = 200;
        if (response instanceof ServletServerHttpResponse servletResponse) {
            status = servletResponse.getServletResponse().getStatus();
        }

        if (status == HttpStatus.NO_CONTENT.value()) {
            return body;
        }

        if (body == null) {
            log.warn("[ServiceKit] Null body with non-204 status for method: {}", returnType.getExecutable().getName());
            return ApiResponse.success(null);
        }
        if (body instanceof ApiResponse) {
            return body;
        }

        if (body instanceof byte[] || body instanceof String) {
            log.debug("[ServiceKit] Bypassing auto-wrap for URI: {} due to type: {}", request.getURI(), body.getClass().getSimpleName());
            if (body instanceof String) {
                try {
                    return objectMapper.writeValueAsString(ApiResponse.success(body));
                } catch (JsonProcessingException e) {
                    log.error("[ServiceKit] Failed to serialize String response at URI: {}", request.getURI(), e);
                    throw new RuntimeException("Serialization error", e);
                }
            }
            return body;
        }
        return ApiResponse.success(body);
    }
}