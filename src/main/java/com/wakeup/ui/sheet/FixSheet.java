package com.wakeup.ui.sheet;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.wakeup.alarm.Reliability;
import com.wakeup.data.MorningState;
import com.wakeup.ui.UI;

/** Explains one missing setting, in three steps, and sends you to the right Android screen. */
public class FixSheet extends Sheet {

    public FixSheet(final Activity a, final int which, final Runnable onDone) {
        super(a);
        String title, desc;
        String[] steps;
        switch (which) {
            case Reliability.EXACT:
                title = "Exact alarms are off";
                desc = "Android may delay your alarm by several minutes.";
                steps = new String[]{"Open settings", "Turn on Alarms and reminders for WakeUp", "Come back. We re-check by ourselves"};
                break;
            case Reliability.FULLSCREEN:
                title = "Full-screen alarm is off";
                desc = "The alarm screen cannot appear over your lock screen.";
                steps = new String[]{"Open settings", "Allow full-screen notifications", "Come back. We re-check by ourselves"};
                break;
            case Reliability.NOTIFICATIONS:
                title = "Notifications are off";
                desc = "Android hides the alarm screen when notifications are blocked.";
                steps = new String[]{"Open settings", "Turn notifications on", "Come back. We re-check by ourselves"};
                break;
            case Reliability.BATTERY:
                title = "Battery optimisation is on";
                desc = "Android can stop WakeUp while you sleep. Your alarm may be late by several minutes, or skipped.";
                steps = new String[]{"Open settings", "Choose Allow, or Unrestricted", "Come back. We re-check by ourselves"};
                break;
            default:
                title = "Auto-start is off";
                desc = "Some phones stop background apps. Turn auto-start on for WakeUp.";
                steps = new String[]{"Open the app settings", "Find Auto-start, or Battery then Unrestricted", "Come back and tap I have done this"};
                break;
        }

        LinearLayout tag = new LinearLayout(a);
        tag.setGravity(Gravity.CENTER_VERTICAL);
        View dot = new View(a);
        dot.setBackground(UI.rr(a, UI.AMBER, 4));
        tag.addView(dot, new LinearLayout.LayoutParams(UI.dp(a, 8), UI.dp(a, 8)));
        TextView cap = UI.caption(a, "ALARM MAY NOT RING", UI.AMBER);
        cap.setPadding(UI.dp(a, 10), 0, 0, 0);
        tag.addView(cap);
        body.addView(tag);

        TextView t = UI.text(a, title, 28, UI.T1, UI.REG);
        t.setPadding(0, UI.dp(a, 12), 0, 0);
        body.addView(t);
        TextView d = UI.text(a, desc, 16, UI.T2, UI.REG);
        d.setLineSpacing(0, 1.2f);
        d.setPadding(0, UI.dp(a, 8), 0, UI.dp(a, 16));
        body.addView(d);

        for (int i = 0; i < steps.length; i++) {
            body.addView(UI.hairline(a));
            LinearLayout row = new LinearLayout(a);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(UI.dp(a, 56));
            TextView n = UI.text(a, String.valueOf(i + 1), 13, UI.T2, UI.BOLD);
            n.setGravity(Gravity.CENTER);
            n.setBackground(UI.outline(a, 0, UI.LINE2, 1f, 14));
            row.addView(n, new LinearLayout.LayoutParams(UI.dp(a, 28), UI.dp(a, 28)));
            TextView s = UI.text(a, steps[i], 16, UI.T1, UI.REG);
            s.setPadding(UI.dp(a, 12), 0, 0, 0);
            row.addView(s, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            body.addView(row);
        }
        body.addView(UI.hairline(a));

        if (which != Reliability.AUTOSTART) {
            LinearLayout wait = new LinearLayout(a);
            wait.setGravity(Gravity.CENTER_VERTICAL);
            wait.setPadding(0, UI.dp(a, 16), 0, 0);
            ProgressBar pb = new ProgressBar(a);
            pb.setIndeterminateTintList(ColorStateList.valueOf(UI.AMBER));
            wait.addView(pb, new LinearLayout.LayoutParams(UI.dp(a, 20), UI.dp(a, 20)));
            TextView w = UI.text(a, "Waiting for you to return…", 14, UI.T2, UI.REG);
            w.setPadding(UI.dp(a, 12), 0, 0, 0);
            wait.addView(w);
            body.addView(wait);
        }

        LinearLayout buttons = new LinearLayout(a);
        buttons.setPadding(0, UI.dp(a, 20), 0, 0);
        TextView no = UI.secondary(a, "Not now", 56);
        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        TextView open = UI.primary(a, "Open settings", 56);
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                Reliability.openSettings(a, which);
            }
        });
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        l1.rightMargin = UI.dp(a, 12);
        buttons.addView(no, l1);
        buttons.addView(open, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f));
        body.addView(buttons);

        if (which == Reliability.AUTOSTART) {
            TextView done = UI.text(a, "I have done this", 15, UI.AMBER, UI.BOLD);
            done.setGravity(Gravity.CENTER);
            done.setMinHeight(UI.dp(a, 52));
            done.setClickable(true);
            done.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MorningState.setAutoStartDone(a, true);
                    dismiss();
                    if (onDone != null) onDone.run();
                }
            });
            body.addView(done);
        } else {
            TextView note = UI.text(a, "Until this is fixed, Home keeps showing that the alarm is not reliable.", 13, UI.T3, UI.REG);
            note.setGravity(Gravity.CENTER);
            note.setPadding(0, UI.dp(a, 12), 0, 0);
            body.addView(note);
        }
    }
}
