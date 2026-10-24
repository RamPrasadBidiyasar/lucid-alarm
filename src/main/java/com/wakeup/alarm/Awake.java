package com.wakeup.alarm;

import android.content.Context;
import com.wakeup.data.MorningState;
import com.wakeup.util.Fmt;

/** Confirming you are awake: cancels the rest of today and arms tomorrow. */
public final class Awake {
    private Awake() {}

    public static void confirm(Context c, String how) {
        int remaining = Scheduler.remainingToday(c);
        int stopped = MorningState.stopped(c);
        String text = "Awake at " + Fmt.time(System.currentTimeMillis());
        if (stopped > 0) text += " after " + stopped + (stopped == 1 ? " ring" : " rings");
        if ("emergency".equals(how)) text = "Emergency stop at " + Fmt.time(System.currentTimeMillis());
        MorningState.setConfirmed(c, remaining, text);
        Scheduler.scheduleNext(c);
        RingService.send(c, RingService.ACTION_STOP_ALL);
    }
}
