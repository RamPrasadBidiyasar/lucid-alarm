package com.wakeup.ui.screen;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.wakeup.alarm.Reliability;
import com.wakeup.alarm.Scheduler;
import com.wakeup.data.MorningState;
import com.wakeup.ui.BaseActivity;
import com.wakeup.ui.Rows;
import com.wakeup.ui.UI;
import com.wakeup.ui.sheet.FixSheet;
import com.wakeup.ui.widget.IconView;
import com.wakeup.ui.widget.SwitchView;

/** Required Android settings first, optional ones after. Problems are amber, never red. */
public class SettingsActivity extends BaseActivity {
    private LinearLayout col;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = column();
        root.addView(appBar("Reliability and sound", IconView.CHEV_L, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        }));
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        col = column();
        col.setPadding(dp(24), dp(12), dp(24), dp(16));
        scroll.addView(col, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bottom = column();
        bottom.setPadding(dp(24), dp(12), dp(24), dp(12));
        TextView test = UI.secondary(this, "Test a real alarm in 1 minute", 56);
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Scheduler.scheduleTest(SettingsActivity.this, 60000);
                Toast.makeText(SettingsActivity.this, "Test alarm in 1 minute. Lock your phone now.", Toast.LENGTH_LONG).show();
            }
        });
        bottom.addView(test, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView hint = UI.text(this, "Lock your phone first. This is the only proof it works.", 13, UI.T3, UI.REG);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(8), 0, 0);
        bottom.addView(hint);
        root.addView(bottom);

        show(root, UI.BG, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        col.removeAllViews();
        int issues = Reliability.issueCount(this);

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        View dot = new View(this);
        dot.setBackground(UI.rr(this, issues > 0 ? UI.AMBER : UI.OK, 5));
        head.addView(dot, new LinearLayout.LayoutParams(dp(10), dp(10)));
        TextView title = UI.text(this, issues == 0 ? "All set" : issues + (issues == 1 ? " thing to fix" : " things to fix"), 32, UI.T1, UI.LIGHT);
        title.setPadding(dp(12), 0, 0, 0);
        head.addView(title);
        col.addView(head);
        TextView sub = UI.text(this, issues == 0
                ? "Everything Android needs is allowed. Run a test alarm to be sure."
                : "Until they are fixed, your alarm may be late or may not ring at all.", 15, UI.T2, UI.REG);
        sub.setLineSpacing(0, 1.2f);
        sub.setPadding(0, dp(8), 0, 0);
        col.addView(sub);

        col.addView(Rows.section(this, "REQUIRED", false));
        status("Exact alarms", "Rings land on the minute", Reliability.EXACT);
        status("Full-screen alarm", "Shows over the lock screen", Reliability.FULLSCREEN);
        status("Notifications", "Needed to open the alarm screen", Reliability.NOTIFICATIONS);
        status("Battery optimisation", "Android may stop WakeUp overnight", Reliability.BATTERY);
        if (Reliability.needsAutoStart()) status("Auto-start", "Needed on some phone brands", Reliability.AUTOSTART);

        col.addView(Rows.section(this, "OPTIONAL", false));
        Rows.add(col, "Alarm sound", "Rising siren", null, null);
        Rows.add(col, "Volume guard", "Restores volume if you turn it down",
                new SwitchView(this, MorningState.volumeGuard(this), new SwitchView.OnChange() {
                    @Override
                    public void onChange(boolean c) {
                        MorningState.setVolumeGuard(SettingsActivity.this, c);
                    }
                }), null);
        Rows.add(col, "Wake-up QR code", MorningState.qrReady(this) ? "Ready" : "Not created yet",
                Rows.chevron(this), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(SettingsActivity.this, QrSetupActivity.class));
                    }
                });
        col.addView(UI.hairline(this));
    }

    private void status(String title, String sub, final int which) {
        View trailing;
        View.OnClickListener click = null;
        if (Reliability.ok(this, which)) {
            LinearLayout ok = new LinearLayout(this);
            ok.setGravity(Gravity.CENTER_VERTICAL);
            ok.addView(new IconView(this, IconView.CHECK, UI.OK).stroke(2.4f), new LinearLayout.LayoutParams(dp(18), dp(18)));
            TextView t = UI.text(this, "Allowed", 14, UI.T2, UI.REG);
            t.setPadding(dp(6), 0, 0, 0);
            ok.addView(t);
            trailing = ok;
        } else {
            TextView fix = UI.text(this, "Fix", 15, UI.AMBER, UI.BOLD);
            fix.setGravity(Gravity.CENTER_VERTICAL);
            fix.setMinHeight(dp(48));
            fix.setPadding(dp(12), 0, dp(4), 0);
            trailing = fix;
            click = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new FixSheet(SettingsActivity.this, which, new Runnable() {
                        @Override
                        public void run() {
                            render();
                        }
                    }).show();
                }
            };
        }
        Rows.add(col, title, sub, trailing, click);
    }
}
