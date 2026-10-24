package com.wakeup.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.wakeup.data.MorningState;

/** Android forgets alarms on reboot, on clock changes and on app updates. Re-arm the next ring. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Scheduler.scheduleNext(context);
        if (StayAwake.isPending(context)) {
            long deadline = MorningState.stayDeadline(context);
            long now = System.currentTimeMillis();
            if (now >= deadline) {
                StayAwake.resume(context);
            } else {
                StayAwake.schedule(context);
            }
        }
    }
}
