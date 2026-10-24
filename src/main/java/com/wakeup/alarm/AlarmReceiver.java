package com.wakeup.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Fires when a ring is due. Schedules the following ring first, then starts the sound. */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long time = intent.getLongExtra(Scheduler.EXTRA_TIME, System.currentTimeMillis());
        boolean test = intent.getBooleanExtra(Scheduler.EXTRA_TEST, false);
        if (!test) {
            Scheduler.scheduleNext(context, Math.max(System.currentTimeMillis(), time));
        }
        Intent svc = new Intent(context, RingService.class).setAction(RingService.ACTION_START);
        svc.putExtra(Scheduler.EXTRA_PLAN, intent.getStringExtra(Scheduler.EXTRA_PLAN));
        svc.putExtra(Scheduler.EXTRA_TIME, time);
        svc.putExtra(Scheduler.EXTRA_TEST, test);
        context.startForegroundService(svc);
    }
}
