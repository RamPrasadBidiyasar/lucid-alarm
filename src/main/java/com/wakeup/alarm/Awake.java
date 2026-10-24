package com.wakeup.alarm;

import android.content.Context;
import com.wakeup.data.MorningState;
import com.wakeup.util.Fmt;

/** Confirming you are awake: cancels the rest of today, arms tomorrow, and starts the stay-awake check. */
public final class Awake {
    private Awake() {}

    public static void confirm(Context c, String how) {
        if (RingService.test) {
            // A preview or test alarm: stop the sound but leave the real day untouched.
            RingService.send(c, RingService.ACTION_STOP_ALL);
            return;
        }
        long now = System.currentTimeMillis();
        int remaining = Scheduler.remainingToday(c);
        int stopped = MorningState.stopped(c);
        boolean emergency = "emergency".equals(how);
        String text = emergency
                ? "Emergency stop at " + Fmt.time(now)
                : "Awake at " + Fmt.time(now) + (stopped > 0 ? " after " + stopped + (stopped == 1 ? " ring" : " rings") : "");
        MorningState.setConfirmed(c, remaining, text);
        MorningState.logHistory(c, how, stopped, now, text);
        Scheduler.scheduleNext(c);
        RingService.send(c, RingService.ACTION_STOP_ALL);
        // Nothing left to ring means nothing to fall back asleep to.
        if (!emergency && remaining > 0 && MorningState.stayAwake(c)) StayAwake.schedule(c);
    }
}
