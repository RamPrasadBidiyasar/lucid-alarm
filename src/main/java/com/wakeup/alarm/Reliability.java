package com.wakeup.alarm;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import com.wakeup.data.MorningState;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Checks the Android settings that decide whether a ring actually happens.
 * Newer APIs are called by reflection so the project compiles against platform 30.
 */
public final class Reliability {
    public static final int EXACT = 0, FULLSCREEN = 1, NOTIFICATIONS = 2, BATTERY = 3, AUTOSTART = 4;

    private Reliability() {}

    public static boolean ok(Context c, int which) {
        switch (which) {
            case EXACT:
                return boolCall(c.getSystemService(AlarmManager.class), "canScheduleExactAlarms", true);
            case FULLSCREEN:
                return boolCall(c.getSystemService(NotificationManager.class), "canUseFullScreenIntent", true);
            case NOTIFICATIONS:
                return c.getSystemService(NotificationManager.class).areNotificationsEnabled();
            case BATTERY:
                PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
                return pm.isIgnoringBatteryOptimizations(c.getPackageName());
            case AUTOSTART:
                return !needsAutoStart() || MorningState.autoStartDone(c);
            default:
                return true;
        }
    }

    public static int issueCount(Context c) {
        int n = 0;
        for (int i = 0; i <= AUTOSTART; i++) if (!ok(c, i)) n++;
        return n;
    }

    /** Only some phone brands hide an auto-start switch. */
    public static boolean needsAutoStart() {
        String m = Build.MANUFACTURER.toLowerCase(Locale.US);
        return m.contains("xiaomi") || m.contains("oppo") || m.contains("vivo") || m.contains("realme")
                || m.contains("oneplus") || m.contains("huawei") || m.contains("honor") || m.contains("samsung");
    }

    private static boolean boolCall(Object target, String method, boolean fallback) {
        try {
            Method m = target.getClass().getMethod(method);
            return (Boolean) m.invoke(target);
        } catch (Exception e) {
            return fallback;
        }
    }

    public static void openSettings(Activity a, int which) {
        String pkg = a.getPackageName();
        Uri uri = Uri.parse("package:" + pkg);
        Intent i;
        switch (which) {
            case EXACT:
                i = new Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM", uri);
                break;
            case FULLSCREEN:
                i = new Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT", uri);
                break;
            case NOTIFICATIONS:
                i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg);
                break;
            case BATTERY:
                i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, uri);
                break;
            default:
                i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri);
                break;
        }
        try {
            a.startActivity(i);
        } catch (RuntimeException e) {
            a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri));
        }
    }
}
