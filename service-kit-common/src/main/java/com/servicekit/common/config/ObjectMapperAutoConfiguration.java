package com.servicekit.common.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ObjectMapperAutoConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customJacksonConfig() {
        return builder -> {
            // Spring Boot tự động đăng ký JavaTimeModule, ta chỉ cần chỉnh feature
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            
            // Loại bỏ các trường null để tối ưu payload
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            
            // Nếu sau này UtcTimestamp phức tạp hơn, có thể đăng ký custom Serializer tại đây:
            // builder.serializerByType(UtcTimestamp.class, new UtcTimestampSerializer());
        };
    }
}