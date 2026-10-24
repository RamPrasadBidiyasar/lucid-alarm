package com.wakeup.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Small formatting helpers shared by the screens. */
public final class Fmt {
    private Fmt() {}

    /** 6:00 style, 12 hour clock, no suffix. */
    public static String hm(int minuteOfDay) {
        int h = (minuteOfDay / 60) % 12;
        if (h == 0) h = 12;
        return h + ":" + two(minuteOfDay % 60);
    }

    public static String ampm(int minuteOfDay) {
        return (minuteOfDay / 60) >= 12 ? "PM" : "AM";
    }

    public static String time(int minuteOfDay) {
        return hm(minuteOfDay) + " " + ampm(minuteOfDay);
    }

    public static int minuteOfDay(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    public static String time(long ms) {
        return time(minuteOfDay(ms));
    }

    public static String two(int v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }

    public static String dayShort(long ms) {
        return new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date(ms));
    }

    /** Start of the day for a yyyy-MM-dd key, or 0 if it cannot be read. */
    public static long dateKeyToMs(String key) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(key).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    /** "Mon 3 Nov". */
    public static String dayMonth(long ms) {
        return new SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(new Date(ms));
    }

    /** True when both times fall on the same calendar day. */
    public static boolean sameDay(long a, long b) {
        Calendar x = Calendar.getInstance();
        Calendar y = Calendar.getInstance();
        x.setTimeInMillis(a);
        y.setTimeInMillis(b);
        return dayNumber(x) == dayNumber(y);
    }

    /** "TODAY · SUN", "TOMORROW · MON" or "WED 12 NOV". */
    public static String dayLabel(long ms) {
        Calendar now = Calendar.getInstance();
        Calendar t = Calendar.getInstance();
        t.setTimeInMillis(ms);
        int diff = dayNumber(t) - dayNumber(now);
        String d = dayShort(ms).toUpperCase(Locale.getDefault());
        if (diff <= 0) return "TODAY · " + d;
        if (diff == 1) return "TOMORROW · " + d;
        return new SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(new Date(ms)).toUpperCase(Locale.getDefault());
    }

    private static int dayNumber(Calendar c) {
        return c.get(Calendar.YEAR) * 400 + c.get(Calendar.DAY_OF_YEAR);
    }

    /** "in 7 h 20 min" or "in 4 min". */
    public static String until(long ms) {
        long mins = Math.max(0, (ms - System.currentTimeMillis() + 59999) / 60000);
        long h = mins / 60;
        long m = mins % 60;
        if (h == 0) return "in " + m + " min";
        if (h >= 24) return "in " + (h / 24) + " d " + (h % 24) + " h";
        return "in " + h + " h " + m + " min";
    }
}
