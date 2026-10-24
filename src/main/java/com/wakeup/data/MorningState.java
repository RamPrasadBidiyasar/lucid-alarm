package com.wakeup.data;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * What has happened this morning, plus a few app-wide settings.
 * Everything keyed by date resets on its own when the date changes.
 */
public final class MorningState {
    private static final String PREFS = "morning";
    public static final float START_GAIN = 0.08f;

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

    /** Undo "confirmed awake": used when the stay-awake check is ignored and the rings resume. */
    public static void clearConfirmed(Context c) {
        sp(c).edit().remove("confirmed").remove("resultDate").remove("result").apply();
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

    // ---- skipped date (holiday / sick day) ------------------------------

    public static String skippedDate(Context c) {
        return sp(c).getString("skippedDate", "");
    }

    public static boolean isSkipped(Context c, String dateKey) {
        return dateKey != null && dateKey.length() > 0 && dateKey.equals(skippedDate(c));
    }

    public static void setSkippedDate(Context c, String dateKey) {
        sp(c).edit().putString("skippedDate", dateKey).apply();
    }

    public static void clearSkippedDate(Context c) {
        sp(c).edit().remove("skippedDate").apply();
    }

    // ---- pause until a date (holiday) ------------------------------------

    /** First day alarms ring again, as yyyy-MM-dd, or "" when not paused. */
    public static String pausedUntil(Context c) {
        return sp(c).getString("pausedUntil", "");
    }

    public static void setPausedUntil(Context c, String dateKey) {
        sp(c).edit().putString("pausedUntil", dateKey).apply();
    }

    public static void clearPausedUntil(Context c) {
        sp(c).edit().remove("pausedUntil").apply();
    }

    // ---- stay-awake check ---------------------------------------------------

    public static boolean stayAwake(Context c) {
        return sp(c).getBoolean("stayAwake", true);
    }

    public static void setStayAwake(Context c, boolean on) {
        sp(c).edit().putBoolean("stayAwake", on).apply();
    }

    /** When the pending check will prompt, or 0 when none is pending. */
    public static long stayPromptAt(Context c) {
        return sp(c).getLong("stayPromptAt", 0);
    }

    public static long stayDeadline(Context c) {
        return sp(c).getLong("stayDeadline", 0);
    }

    public static void setStayPending(Context c, long promptAt, long deadline) {
        sp(c).edit().putLong("stayPromptAt", promptAt).putLong("stayDeadline", deadline).apply();
    }

    public static void clearStayPending(Context c) {
        sp(c).edit().remove("stayPromptAt").remove("stayDeadline").apply();
    }

    // ---- 7-day accountability history ------------------------------------

    public static class HistoryItem {
        public String date;
        public String status; // "awake", "emergency", "missed"
        public int rings;
        public long time;
        public String text;
    }

    public static void logHistory(Context c, String status, int rings, long time, String text) {
        List<HistoryItem> items = getHistory(c);
        String todayKey = today();
        for (int i = items.size() - 1; i >= 0; i--) {
            if (todayKey.equals(items.get(i).date)) items.remove(i);
        }
        HistoryItem h = new HistoryItem();
        h.date = todayKey;
        h.status = status;
        h.rings = rings;
        h.time = time;
        h.text = text;
        items.add(0, h);
        while (items.size() > 7) items.remove(items.size() - 1);

        JSONArray arr = new JSONArray();
        for (HistoryItem item : items) {
            try {
                JSONObject o = new JSONObject();
                o.put("date", item.date);
                o.put("status", item.status);
                o.put("rings", item.rings);
                o.put("time", item.time);
                o.put("text", item.text);
                arr.put(o);
            } catch (JSONException ignored) {}
        }
        sp(c).edit().putString("historyLog", arr.toString()).apply();
    }

    public static List<HistoryItem> getHistory(Context c) {
        List<HistoryItem> list = new ArrayList<HistoryItem>();
        String raw = sp(c).getString("historyLog", null);
        if (raw == null) return list;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                HistoryItem item = new HistoryItem();
                item.date = o.optString("date", "");
                item.status = o.optString("status", "awake");
                item.rings = o.optInt("rings", 1);
                item.time = o.optLong("time", 0);
                item.text = o.optString("text", "");
                list.add(item);
            }
        } catch (JSONException ignored) {}
        return list;
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
