package com.wakeup.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import com.wakeup.R;
import com.wakeup.data.MorningState;
import com.wakeup.data.PlanStore;
import com.wakeup.model.Plan;
import com.wakeup.ui.screen.RingActivity;
import com.wakeup.util.Fmt;

/**
 * Owns the sound while an alarm rings: rising volume, the quiet window, vibration, volume guard.
 * The activities only read the public statics and send commands. Nothing here waits for any UI.
 */
public class RingService extends Service {
    public static final String ACTION_START = "com.wakeup.START";
    public static final String ACTION_QUIET = "com.wakeup.QUIET";
    public static final String ACTION_STOP_RING = "com.wakeup.STOP_RING";
    public static final String ACTION_STOP_ALL = "com.wakeup.STOP_ALL";
    public static final String EXTRA_SECONDS = "seconds";

    public static final long QUIET_MS = 60000;
    private static final float RAMP_BASE = 0.92f / 90f; // 90 seconds to reach full volume
    private static final int NOTIF_ID = 7;
    private static final String CHANNEL = "ring";

    public static volatile boolean ringing;
    public static volatile float gain = MorningState.START_GAIN;
    public static volatile long quietUntil;
    public static volatile int ringIndex;
    public static volatile int ringTotal;
    public static volatile String planId;
    public static volatile String planName = "";
    public static volatile long ringTimeMs;
    /** True for a preview or a test alarm. Confirming a test must not cancel the real day. */
    public static volatile boolean test;

    private SirenPlayer siren;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private AudioManager audio;
    private int originalVolume = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastTick;
    private long lastSaved;
    private long stopAt = Long.MAX_VALUE;
    private BroadcastReceiver volumeGuard;

    /** Safe to call from any foreground component. */
    public static void send(Context c, String action) {
        try {
            c.startService(new Intent(c, RingService.class).setAction(action));
        } catch (RuntimeException ignored) {
        }
    }

    public static void startQuiet(Context c) {
        try {
            c.startService(new Intent(c, RingService.class).setAction(ACTION_QUIET));
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_START.equals(action)) {
            handleStart(intent);
        } else if (ACTION_QUIET.equals(action)) {
            if (ringing) quietUntil = System.currentTimeMillis() + QUIET_MS;
        } else if (ACTION_STOP_RING.equals(action)) {
            if (ringing && !test) MorningState.incStopped(this);
            stopEverything();
        } else if (ACTION_STOP_ALL.equals(action)) {
            stopEverything();
        } else if (!ringing) {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void handleStart(Intent intent) {
        String pid = intent.getStringExtra(Scheduler.EXTRA_PLAN);
        long t = intent.getLongExtra(Scheduler.EXTRA_TIME, System.currentTimeMillis());
        boolean isTest = intent.getBooleanExtra(Scheduler.EXTRA_TEST, false);
        test = isTest;
        Plan p = pid == null ? null : PlanStore.get(this, pid);
        if (p == null) p = new Plan();

        planId = p.id;
        if (!isTest) MorningState.setLastPlanId(this, p.id);
        planName = isTest ? "Test alarm" : p.name;
        ringTimeMs = t;
        if (isTest) {
            ringIndex = 1;
            ringTotal = 1;
            stopAt = Long.MAX_VALUE;
        } else {
            ringIndex = Math.max(1, (Fmt.minuteOfDay(t) - p.startMin) / Math.max(1, p.interval) + 1);
            ringTotal = p.totalRings();
            // After the last ring, give it one more interval, then end the morning as missed.
            stopAt = ringIndex >= ringTotal ? t + p.interval * 60000L : Long.MAX_VALUE;
        }
        quietUntil = 0;

        startForeground(NOTIF_ID, buildNotification());

        if (!ringing) {
            ringing = true;
            gain = MorningState.gain(this);
            lastTick = System.currentTimeMillis();
            lastSaved = lastTick;

            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wakeup:ring");
            wakeLock.acquire(20 * 60 * 1000L);

            audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            originalVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM);
            audio.setStreamVolume(AudioManager.STREAM_ALARM, audio.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);

            siren = new SirenPlayer();
            siren.setSound(p.sound);
            siren.setGain(gain);
            siren.start();

            if (p.vibration) {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 300, 500, 300, 900, 600}, 0));
            }

            registerVolumeGuard();
            handler.post(tick);
        }
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!ringing) return;
            long now = System.currentTimeMillis();
            float dt = (now - lastTick) / 1000f;
            lastTick = now;
            boolean quiet = now < quietUntil;
            if (!quiet) {
                // Starts extra gentle below 30% volume, then gently accelerates to 100%
                float rate = gain < 0.30f ? RAMP_BASE * 0.5f : RAMP_BASE;
                gain = Math.min(1f, gain + rate * dt);
            }
            if (siren != null) siren.setGain(quiet ? 0f : gain);
            if (now - lastSaved > 3000) {
                MorningState.setGain(RingService.this, gain);
                lastSaved = now;
            }
            if (now >= stopAt) {
                String miss = "Missed. Rings ended at " + Fmt.time(now);
                MorningState.setResult(RingService.this, miss);
                MorningState.logHistory(RingService.this, "missed", 0, now, miss);
                StayAwake.cancel(RingService.this);
                stopEverything();
                return;
            }
            handler.postDelayed(this, 200);
        }
    };

    private void registerVolumeGuard() {
        volumeGuard = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!MorningState.volumeGuard(context) || audio == null) return;
                int max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                if (audio.getStreamVolume(AudioManager.STREAM_ALARM) < max) {
                    audio.setStreamVolume(AudioManager.STREAM_ALARM, max, 0);
                }
            }
        };
        registerReceiver(volumeGuard, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));
    }

    private void stopEverything() {
        if (ringing) {
            MorningState.setGain(this, gain);
            if (siren != null) siren.stop();
            siren = null;
            if (vibrator != null) vibrator.cancel();
            vibrator = null;
            if (volumeGuard != null) {
                try {
                    unregisterReceiver(volumeGuard);
                } catch (IllegalArgumentException ignored) {
                }
                volumeGuard = null;
            }
            if (audio != null && originalVolume >= 0) {
                audio.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0);
            }
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
            wakeLock = null;
        }
        ringing = false;
        handler.removeCallbacks(tick);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (ringing) stopEverything();
        super.onDestroy();
    }

    private Notification buildNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Alarm ringing", NotificationManager.IMPORTANCE_HIGH);
        ch.setSound(null, null);
        ch.enableVibration(false);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);

        Intent open = new Intent(this, RingActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_ring)
                .setContentTitle("WakeUp · Ring " + ringIndex + " of " + ringTotal)
                .setContentText("Tap to solve and stop this ring")
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setContentIntent(pi)
                .setFullScreenIntent(pi, true)
                .build();
    }
}
