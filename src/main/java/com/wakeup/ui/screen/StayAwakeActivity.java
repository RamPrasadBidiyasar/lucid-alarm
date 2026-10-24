package com.wakeup.ui.screen;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.wakeup.alarm.SirenPlayer;
import com.wakeup.alarm.StayAwake;
import com.wakeup.data.MorningState;
import com.wakeup.ui.BaseActivity;
import com.wakeup.ui.UI;
import com.wakeup.util.Fmt;

/** "Still up?": one tap within a minute, or the rings start again. */
public class StayAwakeActivity extends BaseActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SirenPlayer chime;
    private TextView count;
    private View fill;
    private FrameLayout track;
    private boolean finished;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!StayAwake.isPending(this)) {
            finish();
            return;
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        LinearLayout root = column();
        LinearLayout top = column();
        top.setPadding(dp(24), dp(72), dp(24), 0);
        top.addView(UI.caption(this, "STAY-AWAKE CHECK", UI.AMBER));
        TextView h = UI.text(this, "Still up?", 56, UI.T1, UI.LIGHT);
        h.setLetterSpacing(-0.02f);
        h.setPadding(0, dp(16), 0, 0);
        top.addView(h);
        TextView sub = UI.text(this, "Tap within a minute or the rings start again.", 16, UI.T2, UI.REG);
        sub.setLineSpacing(0, 1.2f);
        sub.setPadding(0, dp(8), 0, 0);
        top.addView(sub);
        root.addView(top);

        LinearLayout mid = column();
        mid.setPadding(dp(24), dp(56), dp(24), 0);
        count = UI.text(this, "1:00", 96, UI.T1, UI.LIGHT);
        count.setLetterSpacing(-0.03f);
        mid.addView(count);
        track = new FrameLayout(this);
        track.setBackground(UI.rr(this, UI.LINE, 1));
        fill = new View(this);
        fill.setBackground(UI.rr(this, UI.AMBER, 1));
        track.addView(fill, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2));
        tl.topMargin = dp(20);
        mid.addView(track, tl);
        root.addView(mid);

        root.addView(UI.spacer(this, 1f));

        LinearLayout bottom = column();
        bottom.setPadding(dp(24), 0, dp(24), dp(24));
        TextView up = UI.primary(this, "I'm up", 72);
        up.setTextSize(22);
        up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UI.haptic(v, UI.H_CONFIRM);
                StayAwake.cancel(StayAwakeActivity.this);
                finishToHome();
            }
        });
        bottom.addView(up, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView note = UI.text(this, "Confirming keeps today's alarms cancelled.", 13, UI.T3, UI.REG);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, dp(12), 0, 0);
        bottom.addView(note, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(bottom);

        show(root, UI.BG, false);

        chime = new SirenPlayer();
        chime.setSound("chime");
        chime.setGain(0.4f);
        chime.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(tick);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopChime();
        super.onDestroy();
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (finished) return;
            long left = MorningState.stayDeadline(StayAwakeActivity.this) - System.currentTimeMillis();
            if (!StayAwake.isPending(StayAwakeActivity.this)) {
                // Answered elsewhere, or already resumed by the deadline alarm.
                finishToHome();
                return;
            }
            if (left <= 0) {
                stopChime();
                StayAwake.resume(StayAwakeActivity.this);
                finished = true;
                startActivity(new Intent(StayAwakeActivity.this, RingActivity.class));
                finish();
                return;
            }
            long s = (left + 999) / 1000;
            count.setText((s / 60) + ":" + Fmt.two((int) (s % 60)));
            ViewGroup.LayoutParams lp = fill.getLayoutParams();
            lp.width = (int) (track.getWidth() * Math.min(1f, left / (float) StayAwake.WINDOW_MS));
            fill.setLayoutParams(lp);
            handler.postDelayed(this, 250);
        }
    };

    private void stopChime() {
        if (chime != null) {
            chime.stop();
            chime = null;
        }
    }

    private void finishToHome() {
        if (finished) return;
        finished = true;
        stopChime();
        startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    @Override
    public void onBackPressed() {
        // Answer the check, or let it run out.
    }
}
