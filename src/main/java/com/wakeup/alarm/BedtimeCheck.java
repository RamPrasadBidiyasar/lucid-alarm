package com.wakeup.alarm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import com.wakeup.R;
import com.wakeup.ui.screen.MainActivity;
import com.wakeup.util.Fmt;

/**
 * Eight hours before the next ring, checks the battery. It only speaks up when the phone is not
 * charging and is low, because a flat phone cannot ring.
 */
public final class BedtimeCheck {
    public static final String ACTION = "com.wakeup.BEDTIME";
    public static final int LOW_PERCENT = 30;

    private static final long LEAD_MS = 8 * 60 * 60 * 1000L;
    private static final int REQ = 5;
    private static final int NOTIF_ID = 9;
    private static final String CHANNEL = "bedtime";

    private BedtimeCheck() {}

    public static void schedule(Context c, long nextRing) {
        long t = nextRing - LEAD_MS;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pending(c));
        if (t > System.currentTimeMillis() + 60000) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pending(c));
        }
    }

    public static void cancel(Context c) {
        ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(pending(c));
    }

    /** Battery percent when it is low and the phone is not charging, otherwise -1. */
    public static int lowBattery(Context c) {
        Intent b = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b == null) return -1;
        int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
        if (charging || level < 0 || scale <= 0) return -1;
        int pct = Math.round(level * 100f / scale);
        return pct <= LOW_PERCENT ? pct : -1;
    }

    /** Runs when the check is due. */
    public static void run(Context c) {
        int pct = lowBattery(c);
        Scheduler.Next next = Scheduler.next(c, System.currentTimeMillis());
        if (pct < 0 || next == null) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Bedtime battery check",
                NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent open = PendingIntent.getActivity(c, 2, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        nm.notify(NOTIF_ID, new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_ring)
                .setContentTitle("Battery " + pct + "%")
                .setContentText("Plug in so WakeUp can ring at " + Fmt.time(next.time) + ".")
                .setAutoCancel(true)
                .setContentIntent(open)
                .build());
    }

    private static PendingIntent pending(Context c) {
        return PendingIntent.getBroadcast(c, REQ, new Intent(c, ReminderReceiver.class).setAction(ACTION),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
