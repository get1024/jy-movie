package com.movie.api.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/** Legacy database formats, with strict parsing instead of silently returning the epoch. */
public final class DataTimeUtil {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private DataTimeUtil() {
    }

    public static String getNowTimeString() {
        return formatTimestamp(Instant.now(), ZoneId.systemDefault());
    }

    public static String formatTimestamp(Instant instant, ZoneId zone) {
        return TIMESTAMP.format(instant.atZone(zone));
    }

    public static long parseTimeStamp(String value) {
        return LocalDateTime.parse(value, TIMESTAMP).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static boolean isAfterNow(String value) {
        return LocalDate.parse(value, DATE).atStartOfDay(ZoneId.systemDefault()).toInstant().isAfter(Instant.now());
    }
}
