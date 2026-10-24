package com.wakeup.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.wakeup.data.MorningState;
import com.wakeup.data.PlanStore;
import com.wakeup.model.Plan;
import com.wakeup.ui.screen.MainActivity;
import com.wakeup.util.Fmt;
import java.util.Calendar;
import java.util.List;

/**
 * Only the next ring is ever scheduled. When it fires, AlarmReceiver schedules the one after it,
 * so a crash or a killed process can never break the chain.
 */
public final class Scheduler {
    public static final String ACTION_RING = "com.wakeup.RING";
    public static final String EXTRA_PLAN = "plan";
    public static final String EXTRA_TIME = "time";
    public static final String EXTRA_TEST = "test";

    private static final int SEARCH_DAYS = 400; // enough to look past a long pause
    private static final int REQ_NEXT = 1;
    private static final int REQ_TEST = 2;

    private Scheduler() {}

    public static final class Next {
        public Plan plan;
        public long time;
    }

    private static Calendar midnight(long ms, int addDays) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_YEAR, addDays);
        return c;
    }

    private static long at(Calendar midnight, int minuteOfDay) {
        Calendar t = (Calendar) midnight.clone();
        t.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60);
        t.set(Calendar.MINUTE, minuteOfDay % 60);
        return t.getTimeInMillis();
    }

    private static int dowMonZero(Calendar c) {
        return (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
    }

    /** First ring of this plan strictly after the given time, or -1. Skips a day already confirmed awake. */
    public static long nextForPlan(Plan p, long after, String confirmedDate) {
        return nextForPlan(p, after, confirmedDate, "");
    }

    public static long nextForPlan(Plan p, long after, String confirmedDate, String skippedDate) {
        return nextForPlan(p, after, confirmedDate, skippedDate, "");
    }

    /** Also skips every day before pausedUntil (a yyyy-MM-dd key, "" when not paused). */
    public static long nextForPlan(Plan p, long after, String confirmedDate, String skippedDate,
                                   String pausedUntil) {
        if (p.interval < 1 || p.stopMin < p.startMin) return -1;
        for (int d = 0; d < SEARCH_DAYS; d++) {
            Calendar day = midnight(after, d);
            if (!p.runsOn(dowMonZero(day))) continue;
            String key = MorningState.dateKey(day.getTimeInMillis());
            if (key.equals(confirmedDate) || key.equals(skippedDate)) continue;
            if (pausedUntil.length() > 0 && key.compareTo(pausedUntil) < 0) continue;
            for (int m = p.startMin; m <= p.stopMin; m += p.interval) {
                long t = at(day, m);
                if (t > after) return t;
            }
        }
        return -1;
    }

    public static Next next(Context c, long after) {
        String confirmed = MorningState.confirmedDate(c);
        String skipped = MorningState.skippedDate(c);
        String paused = MorningState.pausedUntil(c);
        Next best = null;
        List<Plan> plans = PlanStore.all(c);
        for (Plan p : plans) {
            if (!p.enabled) continue;
            long t = nextForPlan(p, after, confirmed, skipped, paused);
            if (t > 0 && (best == null || t < best.time)) {
                best = new Next();
                best.plan = p;
                best.time = t;
            }
        }
        return best;
    }

    public static void scheduleNext(Context c) {
        scheduleNext(c, System.currentTimeMillis());
    }

    public static void scheduleNext(Context c, long after) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent existing = pending(c, REQ_NEXT, null, 0, false, PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(existing);
        Next n = next(c, after);
        if (n == null) {
            BedtimeCheck.cancel(c);
            return;
        }
        BedtimeCheck.schedule(c, n.time);
        PendingIntent pi = pending(c, REQ_NEXT, n.plan.id, n.time, false, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent show = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(n.time, show), pi);
    }

    /** One-off ring for the "test a real alarm" button. */
    public static void scheduleTest(Context c, long inMs) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        List<Plan> plans = PlanStore.all(c);
        String planId = plans.isEmpty() ? null : plans.get(0).id;
        long t = System.currentTimeMillis() + inMs;
        PendingIntent pi = pending(c, REQ_TEST, planId, t, true, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent show = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(t, show), pi);
    }

    private static PendingIntent pending(Context c, int req, String planId, long time, boolean test, int flags) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION_RING);
        i.putExtra(EXTRA_PLAN, planId);
        i.putExtra(EXTRA_TIME, time);
        i.putExtra(EXTRA_TEST, test);
        return PendingIntent.getBroadcast(c, req, i, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Rings still to come today across all enabled plans (used for "cancels all N remaining"). */
    public static int remainingToday(Context c) {
        long now = System.currentTimeMillis();
        Calendar day = midnight(now, 0);
        int count = 0;
        for (Plan p : PlanStore.all(c)) {
            if (!p.enabled || !p.runsOn(dowMonZero(day))) continue;
            for (int m = p.startMin; m <= p.stopMin; m += Math.max(1, p.interval)) {
                if (at(day, m) > now) count++;
            }
        }
        return count;
    }

    /** First ring on a later day than the one containing `ms`, or -1. For the "then Tue 6:00" line. */
    public static long firstOnLaterDay(Context c, long ms) {
        long after = midnight(ms, 1).getTimeInMillis() - 1;
        String confirmed = MorningState.confirmedDate(c);
        String skipped = MorningState.skippedDate(c);
        String paused = MorningState.pausedUntil(c);
        long best = -1;
        for (Plan p : PlanStore.all(c)) {
            if (!p.enabled) continue;
            long t = nextForPlan(p, after, confirmed, skipped, paused);
            if (t > 0 && (best < 0 || t < best)) best = t;
        }
        return best;
    }

    /** First ring on the day of a yyyy-MM-dd key, ignoring any skip. Used to word "Skipped Mon 6:00 AM". */
    public static long firstRingOn(Context c, String dateKey) {
        long dayStart = Fmt.dateKeyToMs(dateKey);
        if (dayStart == 0) return -1;
        Calendar day = midnight(dayStart, 0);
        long best = -1;
        for (Plan p : PlanStore.all(c)) {
            if (!p.enabled || !p.runsOn(dowMonZero(day))) continue;
            long t = at(day, p.startMin);
            if (best < 0 || t < best) best = t;
        }
        return best;
    }
}
