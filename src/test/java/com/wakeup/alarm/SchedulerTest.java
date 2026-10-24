package com.wakeup.alarm;

import static org.junit.Assert.assertEquals;

import com.wakeup.model.Plan;

import java.util.Calendar;

import org.junit.Test;

/** The ring-time maths is plain Java, so it runs without a phone. */
public class SchedulerTest {

    /** Month is 1 to 12. 2026-10-25 is a Sunday, 2026-10-26 a Monday. */
    private static long at(int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, day, hour, minute, 0);
        return c.getTimeInMillis();
    }

    private static Plan weekdays() {
        Plan p = new Plan();
        p.startMin = 6 * 60;
        p.stopMin = 9 * 60;
        p.interval = 5;
        p.days = 0x1F; // Monday to Friday
        return p;
    }

    @Test
    public void sundayNightRingsMondayAtSix() {
        long next = Scheduler.nextForPlan(weekdays(), at(2026, 10, 25, 22, 0), "");
        assertEquals(at(2026, 10, 26, 6, 0), next);
    }

    @Test
    public void nextRingFollowsTheInterval() {
        long next = Scheduler.nextForPlan(weekdays(), at(2026, 10, 26, 6, 1), "");
        assertEquals(at(2026, 10, 26, 6, 5), next);
    }

    @Test
    public void afterTheLastRingTheNextDayStarts() {
        long next = Scheduler.nextForPlan(weekdays(), at(2026, 10, 26, 9, 0), "");
        assertEquals(at(2026, 10, 27, 6, 0), next);
    }

    @Test
    public void aConfirmedMorningIsSkipped() {
        long next = Scheduler.nextForPlan(weekdays(), at(2026, 10, 26, 6, 11), "2026-10-26");
        assertEquals(at(2026, 10, 27, 6, 0), next);
    }

    @Test
    public void fridayNightSkipsTheWeekend() {
        long next = Scheduler.nextForPlan(weekdays(), at(2026, 10, 30, 22, 0), "");
        assertEquals(at(2026, 11, 2, 6, 0), next);
    }

    @Test
    public void weekendPlanStartsOnSaturday() {
        Plan p = weekdays();
        p.startMin = 8 * 60;
        p.stopMin = 10 * 60 + 30;
        p.interval = 10;
        p.days = 0x60; // Saturday and Sunday
        long next = Scheduler.nextForPlan(p, at(2026, 10, 30, 22, 0), "");
        assertEquals(at(2026, 10, 31, 8, 0), next);
    }

    @Test
    public void aPlanWithNoDaysNeverRings() {
        Plan p = weekdays();
        p.days = 0;
        assertEquals(-1L, Scheduler.nextForPlan(p, at(2026, 10, 25, 22, 0), ""));
    }

    @Test
    public void aSkippedDayIsPassedOver() {
        long next = Scheduler.nextForPlan(weekdays(), at(2026, 10, 25, 22, 0), "", "2026-10-26");
        assertEquals(at(2026, 10, 27, 6, 0), next);
    }

    @Test
    public void aPauseHoldsAlarmsUntilTheChosenDay() {
        long next = Scheduler.nextForPlan(weekdays(), at(2026, 10, 25, 22, 0), "", "", "2026-11-02");
        assertEquals(at(2026, 11, 2, 6, 0), next);
    }

    @Test
    public void aPauseLongerThanAWeekIsStillFound() {
        long next = Scheduler.nextForPlan(weekdays(), at(2026, 10, 25, 22, 0), "", "", "2026-12-14");
        assertEquals(at(2026, 12, 14, 6, 0), next);
    }

    @Test
    public void skipAndConfirmCanOverlap() {
        long next = Scheduler.nextForPlan(weekdays(), at(2026, 10, 26, 6, 11), "2026-10-26", "2026-10-27");
        assertEquals(at(2026, 10, 28, 6, 0), next);
    }

    @Test
    public void countsEveryRingOfAMorning() {
        assertEquals(37, weekdays().totalRings());
    }
}
