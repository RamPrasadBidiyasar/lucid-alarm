package com.wakeup.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import org.junit.Test;

public class FmtTest {

    private static long at(int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, day, hour, minute, 0);
        return c.getTimeInMillis();
    }

    @Test
    public void formatsTwelveHourTimes() {
        assertEquals("6:00 AM", Fmt.time(6 * 60));
        assertEquals("12:05 PM", Fmt.time(12 * 60 + 5));
        assertEquals("12:00 AM", Fmt.time(0));
        assertEquals("11:59 PM", Fmt.time(23 * 60 + 59));
    }

    @Test
    public void readsADateKeyAsMidnight() {
        assertEquals(at(2026, 10, 26, 0, 0), Fmt.dateKeyToMs("2026-10-26"));
        assertEquals(0L, Fmt.dateKeyToMs("not a date"));
    }

    @Test
    public void comparesCalendarDays() {
        assertTrue(Fmt.sameDay(at(2026, 10, 26, 0, 1), at(2026, 10, 26, 23, 59)));
        assertFalse(Fmt.sameDay(at(2026, 10, 26, 23, 59), at(2026, 10, 27, 0, 1)));
    }
}
