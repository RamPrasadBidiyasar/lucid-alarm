package com.wakeup.model;

import org.json.JSONException;
import org.json.JSONObject;

/** One recurring wake-up routine. Days is a bit mask, bit 0 = Monday ... bit 6 = Sunday. */
public class Plan {
    public static final String MISSION_MATH = "math";
    public static final String MISSION_TYPING = "typing";
    public static final String MISSION_SHAKE = "shake";
    public static final String CONFIRM_QR = "qr";
    public static final String CONFIRM_PHRASE = "phrase";
    public static final String CONFIRM_NFC = "nfc";
    public static final String CONFIRM_HOLD = "hold";
    public static final String SOUND_SIREN = "siren";
    public static final String SOUND_BEACON = "beacon";
    public static final String SOUND_CHIME = "chime";

    public String id;
    public String name = "New routine";
    public int startMin = 6 * 60;
    public int stopMin = 9 * 60;
    public int interval = 5;
    public int days = 0x1F; // Mon to Fri
    public boolean enabled = true;
    public String mission = MISSION_MATH;
    public int difficulty = 1; // 0 easy, 1 medium, 2 hard
    public String confirm = CONFIRM_PHRASE;
    public String sound = SOUND_SIREN;
    public boolean quiet = true;
    public boolean vibration = true;

    public Plan() {
        id = Long.toHexString(System.nanoTime());
    }

    public boolean runsOn(int dowMonZero) {
        return ((days >> dowMonZero) & 1) == 1;
    }

    /** Number of rings in one morning, first and last included. */
    public int totalRings() {
        return (stopMin - startMin) / Math.max(1, interval) + 1;
    }

    public String missionLabel() {
        String n;
        if (MISSION_TYPING.equals(mission)) n = "Typing";
        else if (MISSION_SHAKE.equals(mission)) n = "Shake";
        else n = "Math";
        String[][] d = {{"Easy", "Medium", "Hard"}, {"Short", "Medium", "Long"}, {"30", "50", "100"}};
        int k = MISSION_TYPING.equals(mission) ? 1 : MISSION_SHAKE.equals(mission) ? 2 : 0;
        return n + " · " + d[k][Math.max(0, Math.min(2, difficulty))];
    }

    public String confirmLabel() {
        if (CONFIRM_PHRASE.equals(confirm)) return "Type phrase";
        if (CONFIRM_HOLD.equals(confirm)) return "Hold 5 sec";
        if (CONFIRM_NFC.equals(confirm)) return "Tap NFC tag";
        return "Scan code";
    }

    public String soundLabel() {
        if (SOUND_BEACON.equals(sound)) return "Pulse beacon";
        if (SOUND_CHIME.equals(sound)) return "Harmonic chime";
        return "Rising siren";
    }

    public String daysLabel() {
        if (days == 0x7F) return "Every day";
        if (days == 0x1F) return "Mon to Fri";
        if (days == 0x60) return "Sat, Sun";
        String[] n = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (runsOn(i)) {
                if (b.length() > 0) b.append(", ");
                b.append(n[i]);
            }
        }
        return b.length() == 0 ? "Never" : b.toString();
    }

    public JSONObject toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("name", name);
            o.put("start", startMin);
            o.put("stop", stopMin);
            o.put("interval", interval);
            o.put("days", days);
            o.put("enabled", enabled);
            o.put("mission", mission);
            o.put("difficulty", difficulty);
            o.put("confirm", confirm);
            o.put("sound", sound);
            o.put("quiet", quiet);
            o.put("vibration", vibration);
            return o;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static Plan fromJson(JSONObject o) {
        Plan p = new Plan();
        p.id = o.optString("id", p.id);
        p.name = o.optString("name", p.name);
        p.startMin = o.optInt("start", p.startMin);
        p.stopMin = o.optInt("stop", p.stopMin);
        p.interval = o.optInt("interval", p.interval);
        p.days = o.optInt("days", p.days);
        p.enabled = o.optBoolean("enabled", true);
        p.mission = o.optString("mission", p.mission);
        p.difficulty = o.optInt("difficulty", p.difficulty);
        p.confirm = o.optString("confirm", p.confirm);
        p.sound = o.optString("sound", p.sound);
        p.quiet = o.optBoolean("quiet", true);
        p.vibration = o.optBoolean("vibration", true);
        return p;
    }
}
