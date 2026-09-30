package com.movie.api.utils;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class SeatSelectionTest {
    @Test
    void acceptsLegacySeatFormat() {
        assertEquals(Arrays.asList(1, 12, 30), SeatSelection.validate("1号12号30号", 30));
        assertEquals(Arrays.asList(1, 2), SeatSelection.parse("1号2"));
    }

    @Test
    void rejectsMissingDuplicateMalformedAndOutOfRangeSeats() {
        for (String seats : Arrays.asList(null, "", "号", "1号号2号", "0号", "-1号", "1号1号", "999999999999号")) {
            assertThrows(IllegalArgumentException.class, () -> SeatSelection.validate(seats, 50), String.valueOf(seats));
        }
        assertThrows(IllegalArgumentException.class, () -> SeatSelection.validate("51号", 50));
        assertThrows(IllegalArgumentException.class, () -> SeatSelection.validate("1号", null));
    }
}
