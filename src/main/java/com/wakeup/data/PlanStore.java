package com.wakeup.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.wakeup.alarm.Scheduler;
import com.wakeup.model.Plan;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

/** Plans live in SharedPreferences as a JSON array. */
public final class PlanStore {
    private static final String PREFS = "plans";
    private static final String KEY = "list";

    private PlanStore() {}

    public static List<Plan> all(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = sp.getString(KEY, null);
        List<Plan> out = new ArrayList<Plan>();
        if (raw == null) {
            out = defaults();
            save(c, out);
            return out;
        }
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) out.add(Plan.fromJson(a.getJSONObject(i)));
        } catch (JSONException e) {
            out = defaults();
        }
        return out;
    }

    public static void save(Context c, List<Plan> plans) {
        JSONArray a = new JSONArray();
        for (Plan p : plans) a.put(p.toJson());
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply();
    }

    public static Plan get(Context c, String id) {
        for (Plan p : all(c)) if (p.id.equals(id)) return p;
        return null;
    }

    public static void upsert(Context c, Plan plan) {
        List<Plan> all = all(c);
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(plan.id)) {
                all.set(i, plan);
                found = true;
            }
        }
        if (!found) all.add(plan);
        save(c, all);
        Scheduler.scheduleNext(c);
    }

    public static void delete(Context c, String id) {
        List<Plan> all = all(c);
        for (int i = all.size() - 1; i >= 0; i--) if (all.get(i).id.equals(id)) all.remove(i);
        save(c, all);
        Scheduler.scheduleNext(c);
    }

    private static List<Plan> defaults() {
        List<Plan> l = new ArrayList<Plan>();
        Plan w = new Plan();
        w.id = "weekdays";
        w.name = "Weekdays";
        l.add(w);

        Plan e = new Plan();
        e.id = "weekend";
        e.name = "Weekend";
        e.startMin = 8 * 60;
        e.stopMin = 10 * 60 + 30;
        e.interval = 10;
        e.days = 0x60;
        e.mission = Plan.MISSION_TYPING;
        e.difficulty = 0;
        l.add(e);

        Plan x = new Plan();
        x.id = "exam";
        x.name = "Exam week";
        x.startMin = 5 * 60 + 30;
        x.stopMin = 8 * 60;
        x.interval = 3;
        x.enabled = false;
        x.mission = Plan.MISSION_SHAKE;
        x.difficulty = 1;
        l.add(x);
        return l;
    }
}
