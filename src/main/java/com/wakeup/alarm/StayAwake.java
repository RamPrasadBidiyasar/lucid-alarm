package com.wakeup.alarm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.wakeup.R;
import com.wakeup.data.MorningState;
import com.wakeup.data.PlanStore;
import com.wakeup.model.Plan;
import com.wakeup.ui.screen.MainActivity;
import com.wakeup.ui.screen.StayAwakeActivity;
import com.wakeup.util.Fmt;
import java.util.Calendar;

/**
 * Five minutes after you confirm you are awake, one prompt asks "Still up?". Ignore it for a
 * minute and the rings resume. A second alarm enforces the deadline, so it works even if the
 * prompt screen never opens.
 */
public final class StayAwake {
    public static final String ACTION_PROMPT = "com.wakeup.STAY_PROMPT";
    public static final String ACTION_DEADLINE = "com.wakeup.STAY_DEADLINE";
    public static final long DELAY_MS = 5 * 60 * 1000L;
    public static final long WINDOW_MS = 60 * 1000L;

    private static final int REQ_PROMPT = 3;
    private static final int REQ_DEADLINE = 4;
    private static final int NOTIF_ID = 8;
    private static final String CHANNEL = "check";

    private StayAwake() {}

    public static boolean isPending(Context c) {
        return MorningState.stayPromptAt(c) > 0;
    }

    public static void schedule(Context c) {
        long prompt = System.currentTimeMillis() + DELAY_MS;
        long deadline = prompt + WINDOW_MS;
        MorningState.setStayPending(c, prompt, deadline);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, prompt, receiver(c, REQ_PROMPT, ACTION_PROMPT));
        PendingIntent show = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(deadline, show),
                receiver(c, REQ_DEADLINE, ACTION_DEADLINE));
    }

    /** Stops the check: either you tapped "I'm up", or the day ended some other way. */
    public static void cancel(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.cancel(receiver(c, REQ_PROMPT, ACTION_PROMPT));
        am.cancel(receiver(c, REQ_DEADLINE, ACTION_DEADLINE));
        MorningState.clearStayPending(c);
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIF_ID);
    }

    /** You did not answer in time: the day is not confirmed any more, so the rings come back. */
    public static void resume(Context c) {
        if (!isPending(c)) return;
        cancel(c);
        MorningState.clearConfirmed(c);
        long now = System.currentTimeMillis();
        MorningState.setResult(c, "Back to sleep at " + Fmt.time(now) + ". Rings resumed.");
        Scheduler.scheduleNext(c);
        Plan p = planInWindow(c, now);
        if (p == null) return;
        Intent svc = new Intent(c, RingService.class).setAction(RingService.ACTION_START);
        svc.putExtra(Scheduler.EXTRA_PLAN, p.id);
        svc.putExtra(Scheduler.EXTRA_TIME, now);
        svc.putExtra(Scheduler.EXTRA_TEST, false);
        c.startForegroundService(svc);
    }

    /** Called by the receiver when the prompt is due. Shows over the lock screen. */
    public static void postPrompt(Context c) {
        if (!isPending(c)) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Stay-awake check", NotificationManager.IMPORTANCE_HIGH);
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);
        PendingIntent open = PendingIntent.getActivity(c, 1,
                new Intent(c, StayAwakeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_ring)
                .setContentTitle("Still up?")
                .setContentText("Tap within a minute or the rings start again.")
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setTimeoutAfter(WINDOW_MS)
                .setContentIntent(open)
                .setFullScreenIntent(open, true)
                .build();
        nm.notify(NOTIF_ID, n);
    }

    private static Plan planInWindow(Context c, long now) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        int dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int minute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        for (Plan p : PlanStore.all(c)) {
            if (p.enabled && p.runsOn(dow) && minute >= p.startMin && minute <= p.stopMin) return p;
        }
        return null;
    }

    private static PendingIntent receiver(Context c, int req, String action) {
        Intent i = new Intent(c, ReminderReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(c, req, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
