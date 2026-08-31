package com.servicekit.common.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {

    // Chuẩn ISO-8601 UTC: 2026-08-29T07:33:01.123Z
    private static final DateTimeFormatter UTC_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private TimeUtils() {}

    private static java.time.Clock clock = java.time.Clock.systemUTC();

    /**
     * Cho phép ghi đè Clock trong Unit Test (mock time).
     */
    public static void setClock(java.time.Clock newClock) {
        clock = newClock;
    }

    /**
     * Lấy thời gian hiện tại dưới dạng Epoch Milliseconds 13 chữ số (UTC)
     */
    public static long nowEpochMilli() {
        return Instant.now(clock).toEpochMilli();
    }

    /**
     * Convert 13-digit epoch milliseconds to human-readable UTC string
     * Example: 1724916781123 -> "2026-08-29T07:33:01.123Z"
     */
    public static String toUtcString(Long epochMilli) {
        if (epochMilli == null) {
            return null;
        }
        return UTC_FORMATTER.format(Instant.ofEpochMilli(epochMilli));
    }

    /**
     * Chuyển chuỗi UTC ISO-8601 ngược lại thành 13 chữ số
     */
    public static Long parseUtcToEpochMilli(String utcString) {
        if (utcString == null || utcString.isBlank()) {
            return null;
        }
        return Instant.parse(utcString).toEpochMilli();
    }
}