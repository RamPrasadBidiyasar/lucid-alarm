package com.wakeup.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Receives the stay-awake prompt, its deadline, and the bedtime battery check. */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (StayAwake.ACTION_PROMPT.equals(action)) {
            StayAwake.postPrompt(context);
        } else if (StayAwake.ACTION_DEADLINE.equals(action)) {
            StayAwake.resume(context);
        } else if (BedtimeCheck.ACTION.equals(action)) {
            BedtimeCheck.run(context);
        }
    }
}
