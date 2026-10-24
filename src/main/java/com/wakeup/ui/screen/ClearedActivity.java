package com.wakeup.ui.screen;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.wakeup.alarm.Scheduler;
import com.wakeup.data.MorningState;
import com.wakeup.ui.BaseActivity;
import com.wakeup.ui.UI;
import com.wakeup.ui.widget.CheckView;
import com.wakeup.ui.widget.IconView;
import com.wakeup.util.Fmt;

/** The morning is done: remaining rings cancelled, tomorrow armed. Restrained, with a drawn checkmark. */
public class ClearedActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setupLockscreen();
        LinearLayout root = column();

        LinearLayout top = column();
        top.setPadding(dp(24), dp(80), dp(24), 0);
        CheckView check = new CheckView(this, UI.OK);
        top.addView(check, new LinearLayout.LayoutParams(dp(120), dp(120)));
        check.play();

        TextView h = UI.text(this, "You're up.", 60, UI.T1, UI.LIGHT);
        h.setLetterSpacing(-0.03f);
        h.setPadding(0, dp(32), 0, 0);
        top.addView(h);

        long wake = MorningState.wakeMs(this);
        int stopped = MorningState.stopped(this);
        String sub = "Awake at " + Fmt.time(wake > 0 ? wake : System.currentTimeMillis()) + ".";
        if (stopped > 0) sub += " You stopped " + stopped + (stopped == 1 ? " ring." : " rings.");
        TextView s = UI.text(this, sub, 18, UI.T2, UI.REG);
        s.setPadding(0, dp(8), 0, 0);
        top.addView(s);
        root.addView(top);

        LinearLayout list = column();
        list.setPadding(dp(24), dp(44), dp(24), 0);
        int cancelled = MorningState.cancelled(this);
        list.addView(UI.hairline(this));
        String cancelledTitle = cancelled > 0
                ? cancelled + (cancelled == 1 ? " alarm cancelled" : " alarms cancelled")
                : "Morning routine complete";
        String cancelledSub = cancelled > 0
                ? "The rest of today is quiet"
                : "All rings finished for today";
        list.addView(item(IconView.CHECK, UI.T2, cancelledTitle, cancelledSub));
        Scheduler.Next next = Scheduler.next(this, System.currentTimeMillis());
        list.addView(UI.hairline(this));
        list.addView(item(IconView.CLOCK, UI.AMBER, next == null ? "No routine armed" : "Tomorrow is armed",
                next == null ? "Turn on a routine to arm your alarm"
                        : Fmt.dayShort(next.time) + " · " + Fmt.time(next.time) + " · " + next.plan.name));
        list.addView(UI.hairline(this));
        root.addView(list);
        long prompt = MorningState.stayPromptAt(this);
        if (prompt > 0) {
            TextView checkNote = UI.text(this, "A quick check comes at " + Fmt.time(prompt) + ". Tap I'm up to stay done.", 14, UI.T3, UI.REG);
            checkNote.setPadding(dp(24), dp(16), dp(24), 0);
            root.addView(checkNote);
        }

        root.addView(UI.spacer(this, 1f));

        LinearLayout bottom = column();
        bottom.setPadding(dp(24), 0, dp(24), dp(24));
        TextView done = UI.primary(this, "Done", 64);
        done.setTextSize(18);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goHome();
            }
        });
        bottom.addView(done, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(bottom);

        show(root, UI.BG, false);
        UI.rise(h, 900);
        UI.rise(s, 1050);
        UI.rise(list, 1200);
        UI.rise(bottom, 1350);
    }

    private View item(int icon, int color, String title, String sub) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(68));
        row.addView(new IconView(this, icon, color), new LinearLayout.LayoutParams(dp(24), dp(24)));
        LinearLayout t = column();
        t.setPadding(dp(16), 0, 0, 0);
        t.addView(UI.text(this, title, 16, UI.T1, UI.BOLD));
        TextView d = UI.text(this, sub, 14, UI.T2, UI.REG);
        d.setPadding(0, dp(2), 0, 0);
        t.addView(d);
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private void goHome() {
        startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    @Override
    public void onBackPressed() {
        goHome();
    }
}
