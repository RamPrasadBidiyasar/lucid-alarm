package com.wakeup.ui.sheet;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.wakeup.data.MorningState;
import com.wakeup.ui.UI;
import com.wakeup.util.Fmt;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Minimal 7-day accountability dot matrix and recent morning log. */
public class HistorySheet extends Sheet {

    public HistorySheet(final Context c) {
        super(c);

        body.addView(UI.text(c, "Last 7 days", 24, UI.T1, UI.REG));
        TextView sub = UI.text(c, "Green: up quickly. Amber ring: needed 2 or more rings. Grey ring: missed or emergency.", 14, UI.T2, UI.REG);
        sub.setPadding(0, UI.dp(c, 4), 0, UI.dp(c, 20));
        body.addView(sub);

        List<MorningState.HistoryItem> history = MorningState.getHistory(c);

        // 7-day columns
        LinearLayout matrix = new LinearLayout(c);
        matrix.setOrientation(LinearLayout.HORIZONTAL);
        matrix.setGravity(Gravity.CENTER);
        matrix.setPadding(0, UI.dp(c, 8), 0, UI.dp(c, 16));

        long now = System.currentTimeMillis();
        String[] daysLetters = new String[7];
        String[] dateKeys = new String[7];

        // Step back by calendar day, not by 24 hours, so clock changes cannot skip or repeat a day.
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) {
            dateKeys[i] = MorningState.dateKey(cal.getTimeInMillis());
            daysLetters[i] = new SimpleDateFormat("EEEEE", Locale.getDefault()).format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        for (int i = 0; i < 7; i++) {
            final String k = dateKeys[i];
            MorningState.HistoryItem match = null;
            for (MorningState.HistoryItem item : history) {
                if (k.equals(item.date)) {
                    match = item;
                    break;
                }
            }

            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);

            // Explicit wrap_content: a vertical LinearLayout would otherwise stretch the letter to the
            // full column width and leave it sitting left of its dot.
            boolean isToday = i == 6;
            TextView d = UI.text(c, daysLetters[i], 13, isToday ? UI.T1 : UI.T3, isToday ? UI.BOLD : UI.MED);
            d.setGravity(Gravity.CENTER);
            col.addView(d, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            View dot = new View(c);
            int dotColor = UI.LINE2;
            boolean isRing = false;

            if (match != null) {
                if ("emergency".equals(match.status) || "missed".equals(match.status)) {
                    dotColor = UI.T3;
                    isRing = true;
                } else if (match.rings > 1) {
                    dotColor = UI.AMBER;
                    isRing = true;
                } else {
                    dotColor = UI.OK;
                }
            }

            FrameLayout dotBox = new FrameLayout(c);
            if (isRing) {
                dot.setBackground(UI.outline(c, 0, dotColor, 2f, 8));
            } else {
                dot.setBackground(UI.rr(c, dotColor, 8));
            }
            // Days with nothing logged are a small quiet dot so the recorded mornings stand out.
            int size = match == null ? 6 : 12;
            dotBox.addView(dot, new FrameLayout.LayoutParams(UI.dp(c, size), UI.dp(c, size), Gravity.CENTER));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(UI.dp(c, 24), UI.dp(c, 24));
            p.topMargin = UI.dp(c, 8);
            col.addView(dotBox, p);

            matrix.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        body.addView(matrix);

        body.addView(UI.hairline(c));

        // Recent items
        if (history.isEmpty()) {
            TextView empty = UI.text(c, "No completed mornings logged yet.", 14, UI.T3, UI.REG);
            empty.setPadding(0, UI.dp(c, 16), 0, UI.dp(c, 16));
            body.addView(empty);
        } else {
            int shown = Math.min(4, history.size());
            for (int i = 0; i < shown; i++) {
                MorningState.HistoryItem it = history.get(i);
                LinearLayout r = new LinearLayout(c);
                r.setGravity(Gravity.CENTER_VERTICAL);
                r.setMinimumHeight(UI.dp(c, 44));

                TextView tDate = UI.text(c, Fmt.dayLabel(it.time > 0 ? it.time : now), 14, UI.T2, UI.REG);
                r.addView(tDate, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                int color = "emergency".equals(it.status) || "missed".equals(it.status) ? UI.T2
                        : it.rings > 1 ? UI.AMBER : UI.OK;
                TextView tText = UI.text(c, it.text, 14, color, UI.MED);
                r.addView(tText);

                body.addView(r);
                if (i < shown - 1) body.addView(UI.hairline(c));
            }
        }

        body.addView(UI.hairline(c));

        LinearLayout btnRow = new LinearLayout(c);
        btnRow.setPadding(0, UI.dp(c, 20), 0, 0);
        TextView done = UI.primary(c, "Done", 56);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        btnRow.addView(done, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(btnRow);
    }
}
