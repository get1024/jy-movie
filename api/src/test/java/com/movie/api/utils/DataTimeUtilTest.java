package com.movie.api.utils;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import static org.junit.jupiter.api.Assertions.*;

class DataTimeUtilTest {
    @Test
    void keepsDatabaseFormatAndExplicitTimezone() {
        assertEquals("2026-09-30 20:00:00", DataTimeUtil.formatTimestamp(
                Instant.parse("2026-09-30T12:00:00Z"), ZoneId.of("Asia/Shanghai")));
        String timestamp = DataTimeUtil.formatTimestamp(Instant.parse("2026-09-30T12:00:00Z"), ZoneId.systemDefault());
        assertEquals(Instant.parse("2026-09-30T12:00:00Z").toEpochMilli(), DataTimeUtil.parseTimeStamp(timestamp));
    }

    @Test
    void failsOnMalformedTimestampsInsteadOfReturningEpoch() {
        assertThrows(DateTimeParseException.class, () -> DataTimeUtil.parseTimeStamp("2026-02-30 12:00:00"));
        assertThrows(DateTimeParseException.class, () -> DataTimeUtil.parseTimeStamp("not a date"));
    }
}
