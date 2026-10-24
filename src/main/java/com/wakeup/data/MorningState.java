package com.wakeup.data;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * What has happened this morning, plus a few app-wide settings.
 * Everything keyed by date resets on its own when the date changes.
 */
public final class MorningState {
    private static final String PREFS = "morning";
    public static final float START_GAIN = 0.3f;

    private MorningState() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String dateKey(long ms) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(ms));
    }

    public static String today() {
        return dateKey(System.currentTimeMillis());
    }

    // ---- confirmed awake -------------------------------------------------

    public static String confirmedDate(Context c) {
        return sp(c).getString("confirmed", "");
    }

    public static boolean confirmedToday(Context c) {
        return today().equals(confirmedDate(c));
    }

    public static void setConfirmed(Context c, int cancelled, String resultText) {
        sp(c).edit()
                .putString("confirmed", today())
                .putInt("cancelled", cancelled)
                .putLong("wakeMs", System.currentTimeMillis())
                .putString("resultDate", today())
                .putString("result", resultText)
                .apply();
    }

    public static int cancelled(Context c) {
        return sp(c).getInt("cancelled", 0);
    }

    public static long wakeMs(Context c) {
        return sp(c).getLong("wakeMs", 0);
    }

    // ---- result line for Home --------------------------------------------

    public static void setResult(Context c, String text) {
        sp(c).edit().putString("resultDate", today()).putString("result", text).apply();
    }

    public static String resultToday(Context c) {
        SharedPreferences p = sp(c);
        return today().equals(p.getString("resultDate", "")) ? p.getString("result", null) : null;
    }

    // ---- volume level reached today --------------------------------------

    public static float gain(Context c) {
        SharedPreferences p = sp(c);
        if (!today().equals(p.getString("gainDate", ""))) return START_GAIN;
        return Math.max(START_GAIN, p.getFloat("gain", START_GAIN));
    }

    public static void setGain(Context c, float g) {
        sp(c).edit().putString("gainDate", today()).putFloat("gain", g).apply();
    }

    // ---- rings stopped by solving a mission today ------------------------

    public static int stopped(Context c) {
        SharedPreferences p = sp(c);
        return today().equals(p.getString("stoppedDate", "")) ? p.getInt("stopped", 0) : 0;
    }

    public static void incStopped(Context c) {
        sp(c).edit().putString("stoppedDate", today()).putInt("stopped", stopped(c) + 1).apply();
    }

    // ---- QR token and phrase ---------------------------------------------

    public static String qrToken(Context c) {
        SharedPreferences p = sp(c);
        String t = p.getString("qrToken", null);
        if (t == null) {
            t = "wakeup:" + UUID.randomUUID().toString();
            p.edit().putString("qrToken", t).apply();
        }
        return t;
    }

    public static boolean qrReady(Context c) {
        return sp(c).getBoolean("qrReady", false);
    }

    public static void setQrReady(Context c) {
        sp(c).edit().putBoolean("qrReady", true).apply();
    }

    public static final String PHRASE = "I am awake and I am out of bed";

    // ---- settings --------------------------------------------------------

    public static boolean volumeGuard(Context c) {
        return sp(c).getBoolean("volumeGuard", true);
    }

    public static void setVolumeGuard(Context c, boolean v) {
        sp(c).edit().putBoolean("volumeGuard", v).apply();
    }

    public static boolean autoStartDone(Context c) {
        return sp(c).getBoolean("autoStart", false);
    }

    public static void setAutoStartDone(Context c, boolean v) {
        sp(c).edit().putBoolean("autoStart", v).apply();
    }
}
