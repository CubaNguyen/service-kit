package com.servicekit.common.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class UtcTimestamp implements Serializable, Comparable<UtcTimestamp> {

    // Format trực quan, rõ ràng, không ký tự lạ: 2026-08-29 14:35:51 UTC
    private static final DateTimeFormatter READABLE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private final Long value;

    public UtcTimestamp(Long epochMilli) {
        this.value = epochMilli;
    }

    /**
     * Tạo timestamp tại thời điểm hiện tại
     */
    public static UtcTimestamp now() {
        return new UtcTimestamp(Instant.now().toEpochMilli());
    }

    /**
     * Tự động chuyển đổi khi Jackson đọc số 13 chữ số từ JSON
     */
    @JsonCreator
    public static UtcTimestamp of(Long epochMilli) {
        return epochMilli == null ? null : new UtcTimestamp(epochMilli);
    }

    /**
     * Jackson sẽ xuất trường này ra đúng số 13 chữ số (Long)
     */
    @JsonValue
    public Long getValue() {
        return value;
    }

    /**
     * Tự động kích hoạt khi in log qua SLF4J hoặc System.out
     * Kết quả hiển thị: 2026-08-29 14:35:51 UTC (1787988941000)
     */
    @Override
    public String toString() {
        if (value == null) {
            return "null";
        }
        String readable = READABLE_FORMATTER.format(Instant.ofEpochMilli(value));
        return readable + " (" + value + ")";
    }

    @Override
    public int compareTo(UtcTimestamp o) {
        if (o == null || o.value == null) return 1;
        if (this.value == null) return -1;
        return this.value.compareTo(o.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UtcTimestamp that = (UtcTimestamp) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}