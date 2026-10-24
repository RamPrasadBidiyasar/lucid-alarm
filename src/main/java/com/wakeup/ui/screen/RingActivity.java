package com.wakeup.ui.screen;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.wakeup.alarm.Awake;
import com.wakeup.alarm.RingService;
import com.wakeup.alarm.Scheduler;
import com.wakeup.data.MorningState;
import com.wakeup.data.PlanStore;
import com.wakeup.model.Plan;
import com.wakeup.ui.BaseActivity;
import com.wakeup.ui.Paste;
import com.wakeup.ui.UI;
import com.wakeup.ui.sheet.Sheet;
import com.wakeup.ui.widget.IconView;
import com.wakeup.ui.widget.PulseView;
import com.wakeup.ui.widget.VolumeBars;
import com.wakeup.util.Fmt;

/** The ringing screen: full-bleed amber, huge time, two clearly scoped actions. */
public class RingActivity extends BaseActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView time, ap, caption, hold;
    private VolumeBars bars;
    private boolean leaving;
    private int holdLeft;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setupLockscreen();
        build();
    }

    private void build() {
        FrameLayout stack = new FrameLayout(this);
        PulseView pulse = new PulseView(this, UI.ON_AMBER);
        stack.addView(pulse, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(560)));

        LinearLayout col = column();
        col.setPadding(dp(24), 0, dp(24), dp(24));
        stack.addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        caption = UI.caption(this, "", UI.ON_AMBER);
        top.addView(caption, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(new IconView(this, IconView.SPEAKER, UI.ON_AMBER), new LinearLayout.LayoutParams(dp(22), dp(22)));
        col.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setGravity(Gravity.CENTER);
        timeRow.setPadding(0, dp(36), 0, 0);
        time = UI.text(this, "6:00", 150, UI.ON_AMBER, UI.LIGHT);
        time.setLetterSpacing(-0.05f);
        time.setMaxLines(1);
        time.setGravity(Gravity.CENTER);
        time.setAutoSizeTextTypeUniformWithConfiguration(70, 150, 2, TypedValue.COMPLEX_UNIT_SP);
        timeRow.addView(time, new LinearLayout.LayoutParams(0, dp(170), 1f));
        ap = UI.text(this, "AM", 24, UI.ON_AMBER, UI.REG);
        LinearLayout.LayoutParams apl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        apl.gravity = Gravity.BOTTOM;
        apl.bottomMargin = dp(30);
        apl.leftMargin = dp(8);
        timeRow.addView(ap, apl);
        col.addView(timeRow);

        bars = new VolumeBars(this, UI.ON_AMBER);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(150), dp(28));
        bp.gravity = Gravity.CENTER_HORIZONTAL;
        bp.topMargin = dp(36);
        col.addView(bars, bp);
        TextView louder = UI.text(this, "Getting louder until you stop it", 14, UI.ON_AMBER, UI.BOLD);
        louder.setGravity(Gravity.CENTER);
        louder.setPadding(0, dp(12), 0, 0);
        col.addView(louder, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        col.addView(UI.spacer(this, 1f));

        // Path 1: stops this ring only
        TextView s1 = UI.caption(this, "STOPS THIS RING ONLY", UI.ON_AMBER);
        s1.setGravity(Gravity.CENTER);
        s1.setPadding(0, 0, 0, dp(8));
        col.addView(s1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout solve = new LinearLayout(this);
        solve.setGravity(Gravity.CENTER);
        solve.setMinimumHeight(dp(72));
        solve.setBackground(UI.press(UI.rr(this, UI.ON_AMBER, 500), UI.rr(this, 0xFF000000, 500)));
        solve.addView(new IconView(this, IconView.CALC, UI.AMBER).stroke(1.9f), new LinearLayout.LayoutParams(dp(26), dp(26)));
        TextView st = UI.text(this, "Solve to stop", 22, UI.AMBER, UI.BOLD);
        st.setPadding(dp(12), 0, 0, 0);
        solve.addView(st);
        solve.setClickable(true);
        solve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UI.haptic(v, UI.H_TICK);
                startActivity(new Intent(RingActivity.this, MissionActivity.class));
            }
        });
        col.addView(solve, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Path 2: cancels everything left today
        View rule = UI.hairline(this, UI.ON_AMBER);
        rule.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2));
        rl.topMargin = dp(20);
        col.addView(rule, rl);

        int rem = Scheduler.remainingToday(this);
        String scope = rem > 0 ? "CANCELS ALL " + rem + " REMAINING TODAY" : "FINAL RING OF TODAY";
        TextView s2 = UI.caption(this, scope, UI.ON_AMBER);
        s2.setGravity(Gravity.CENTER);
        s2.setPadding(0, dp(16), 0, dp(8));
        col.addView(s2, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Plan plan = RingService.planId != null ? PlanStore.get(this, RingService.planId) : null;
        String upText = "I'm up. Confirm awake";
        if (plan != null) {
            if (Plan.CONFIRM_PHRASE.equals(plan.confirm)) upText = "I'm up. Type phrase";
            else if (Plan.CONFIRM_HOLD.equals(plan.confirm)) upText = "I'm up. Hold 5 sec";
            else if (Plan.CONFIRM_NFC.equals(plan.confirm)) upText = "I'm up. Tap tag";
            else upText = "I'm up. Scan code";
        }
        TextView up = UI.pill(this, upText, 0, 0x22000000, UI.ON_AMBER, UI.ON_AMBER, 56, 17);
        up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UI.haptic(v, UI.H_TICK);
                Intent i = new Intent(RingActivity.this, ScanActivity.class);
                if (RingService.planId != null) i.putExtra("planId", RingService.planId);
                startActivity(i);
            }
        });
        col.addView(up, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        hold = UI.text(this, "Hold 10 seconds for emergency stop", 13, UI.ON_AMBER, UI.MED);
        hold.setGravity(Gravity.CENTER);
        hold.setMinHeight(dp(48));
        hold.setPaintFlags(hold.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        hold.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    holdLeft = 10;
                    handler.postDelayed(holdTick, 1000);
                    hold.setText("Keep holding… 10");
                    return true;
                }
                if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                    handler.removeCallbacks(holdTick);
                    hold.setText("Hold 10 seconds for emergency stop");
                    return true;
                }
                return true;
            }
        });
        col.addView(hold, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        show(stack, UI.AMBER, true);
    }

    private final Runnable holdTick = new Runnable() {
        @Override
        public void run() {
            holdLeft--;
            if (holdLeft <= 0) {
                hold.setText("Hold 10 seconds for emergency stop");
                emergencySheet();
            } else {
                hold.setText("Keep holding… " + holdLeft);
                handler.postDelayed(this, 1000);
            }
        }
    };

    private void emergencySheet() {
        final Sheet s = new Sheet(this);
        s.body.addView(UI.text(this, "Emergency stop", 24, UI.T1, UI.REG));
        TextView d = UI.text(this, "Type this phrase to cancel the rest of today. The morning is logged as an emergency stop.", 15, UI.T2, UI.REG);
        d.setLineSpacing(0, 1.2f);
        d.setPadding(0, dp(8), 0, dp(12));
        s.body.addView(d);
        TextView phrase = UI.text(this, MorningState.PHRASE, 18, UI.AMBER, UI.LIGHT);
        phrase.setPadding(0, 0, 0, dp(12));
        s.body.addView(phrase);
        final EditText in = new EditText(this);
        in.setTextColor(UI.T1);
        in.setHint("Type it here");
        in.setHintTextColor(UI.T3);
        in.setSingleLine(true);
        in.setBackground(UI.outline(this, 0, UI.LINE2, 1f, 14));
        in.setPadding(dp(16), dp(12), dp(16), dp(12));
        Paste.block(in);
        s.body.addView(in, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        final TextView go = UI.primary(this, "Stop everything today", 56);
        go.setAlpha(0.4f);
        go.setClickable(false);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gp.topMargin = dp(16);
        s.body.addView(go, gp);
        in.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence a, int b, int c, int d) {}
            public void onTextChanged(CharSequence a, int b, int c, int d) {}
            public void afterTextChanged(Editable e) {
                boolean ok = e.toString().trim().equalsIgnoreCase(MorningState.PHRASE);
                go.setAlpha(ok ? 1f : 0.4f);
                go.setClickable(ok);
            }
        });
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!in.getText().toString().trim().equalsIgnoreCase(MorningState.PHRASE)) return;
                s.dismiss();
                Awake.confirm(RingActivity.this, "emergency");
                startActivity(new Intent(RingActivity.this, ClearedActivity.class));
                finish();
            }
        });
        s.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        leaving = false;
        handler.post(refresh);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refresh);
        super.onPause();
    }

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            if (!RingService.ringing) {
                if (!leaving) {
                    leaving = true;
                    startActivity(new Intent(RingActivity.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    finish();
                }
                return;
            }
            long now = System.currentTimeMillis();
            int mod = Fmt.minuteOfDay(now);
            time.setText(Fmt.hm(mod));
            ap.setText(Fmt.ampm(mod));
            caption.setText((RingService.planName + " · RING " + RingService.ringIndex + " OF " + RingService.ringTotal).toUpperCase());
            bars.setLevel(RingService.gain);
            handler.postDelayed(this, 250);
        }
    };

    @Override
    public void onBackPressed() {
        // A ringing alarm cannot be backed out of.
    }
}
