package com.servicekit.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class JsonUtils {

    // Fallback instance nếu JsonUtils được gọi trước khi Spring context khởi tạo xong
    private static ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonUtils() {}

    public static void setObjectMapper(ObjectMapper customMapper) {
        if (customMapper != null) {
            mapper = customMapper;
        }
    }

    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("[JsonUtils] Serialization error for object type: {}", object.getClass().getName(), e);
            throw new IllegalArgumentException("Error converting to JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("[JsonUtils] Deserialization error for target class: {}", clazz.getName(), e);
            throw new IllegalArgumentException("Error reading from JSON", e);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("[JsonUtils] Deserialization error for generic type reference", e);
            throw new IllegalArgumentException("Error reading from JSON", e);
        }
    }
}