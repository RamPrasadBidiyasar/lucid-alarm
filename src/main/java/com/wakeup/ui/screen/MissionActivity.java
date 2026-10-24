package com.wakeup.ui.screen;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.wakeup.alarm.RingService;
import com.wakeup.data.PlanStore;
import com.wakeup.model.Plan;
import com.wakeup.ui.BaseActivity;
import com.wakeup.ui.Paste;
import com.wakeup.ui.UI;
import com.wakeup.ui.widget.IconView;
import com.wakeup.util.Fmt;
import java.util.Random;

/**
 * The mission that stops one ring: math, typing or shake. While you work the siren is quiet for
 * 60 s, restarted by each correct step. Run out of time and the sound comes back.
 */
public class MissionActivity extends BaseActivity implements SensorEventListener {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Plan plan;
    private int total = 1;
    private int solved;
    private boolean completing;

    private View[] segs;
    private TextView label;
    private LinearLayout chip;
    private TextView chipText;
    private IconView chipIcon;
    private View quietFill;
    private FrameLayout quietTrack;

    // math
    private String[] questions;
    private int[] answers;
    private int index;
    private StringBuilder input = new StringBuilder();
    private boolean busy;
    private TextView question, answer, message;
    private FrameLayout answerBox;

    // shake
    private int shakes, shakeTarget;
    private long lastShake;
    private TextView shakeCount;
    private SensorManager sensors;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        plan = PlanStore.get(this, RingService.planId);
        if (plan == null) plan = new Plan();
        if (Plan.MISSION_MATH.equals(plan.mission)) total = 3;

        LinearLayout root = column();
        root.addView(header());
        if (Plan.MISSION_TYPING.equals(plan.mission)) typingBody(root);
        else if (Plan.MISSION_SHAKE.equals(plan.mission)) shakeBody(root);
        else mathBody(root);
        show(root, UI.BG, false);
        if (Plan.MISSION_TYPING.equals(plan.mission)) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        if (plan.quiet) RingService.startQuiet(this);
    }

    // ---- shared header: progress, caption, quiet chip, countdown line ----------------------

    private View header() {
        LinearLayout h = column();
        h.setPadding(dp(24), dp(20), dp(24), 0);

        LinearLayout bar = new LinearLayout(this);
        segs = new View[total];
        for (int i = 0; i < total; i++) {
            View s = new View(this);
            s.setBackground(UI.rr(this, UI.LINE, 2));
            segs[i] = s;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(4), 1f);
            if (i > 0) lp.leftMargin = dp(6);
            bar.addView(s, lp);
        }
        h.addView(bar);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(16), 0, 0);
        label = UI.text(this, "", 13, UI.T2, UI.MED);
        row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        chip = new LinearLayout(this);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(14), 0, dp(14), 0);
        chipIcon = new IconView(this, IconView.SPEAKER_MUTE, UI.T1).stroke(1.9f);
        chip.addView(chipIcon, new LinearLayout.LayoutParams(dp(18), dp(18)));
        chipText = UI.text(this, "", 13, UI.T1, UI.BOLD);
        chipText.setPadding(dp(8), 0, 0, 0);
        chip.addView(chipText);
        row.addView(chip, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36)));
        h.addView(row);

        quietTrack = new FrameLayout(this);
        quietTrack.setBackground(UI.rr(this, UI.LINE, 1));
        quietFill = new View(this);
        quietFill.setBackground(UI.rr(this, UI.AMBER, 1));
        quietTrack.addView(quietFill, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));
        LinearLayout.LayoutParams ql = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2));
        ql.topMargin = dp(14);
        h.addView(quietTrack, ql);
        if (!plan.quiet) {
            chip.setVisibility(View.GONE);
            quietTrack.setVisibility(View.INVISIBLE);
        }
        updateLabel();
        return h;
    }

    private void updateLabel() {
        if (solved >= total) label.setText("All done");
        else if (total > 1) label.setText("Problem " + (solved + 1) + " of " + total);
        else label.setText(Plan.MISSION_SHAKE.equals(plan.mission) ? "Shake the phone" : "Type the sentence");
    }

    private void paintChip() {
        if (!plan.quiet) return;
        long left = RingService.quietUntil - System.currentTimeMillis();
        boolean quiet = left > 0;
        chip.setBackground(UI.rr(this, quiet ? UI.S2 : UI.AMBER, 18));
        chipText.setTextColor(quiet ? UI.T1 : UI.ON_AMBER);
        chipIcon.set(quiet ? IconView.SPEAKER_MUTE : IconView.SPEAKER, quiet ? UI.T1 : UI.ON_AMBER);
        long s = (left + 999) / 1000;
        chipText.setText(quiet ? "Quiet 0:" + Fmt.two((int) s) : "Sound on");
        ViewGroup.LayoutParams lp = quietFill.getLayoutParams();
        lp.width = quietTrack.getWidth() <= 0 ? 0 : (int) (quietTrack.getWidth() * Math.max(0, Math.min(1, left / (float) RingService.QUIET_MS)));
        quietFill.setLayoutParams(lp);
    }

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            if (!RingService.ringing && !completing) {
                finish();
                return;
            }
            paintChip();
            handler.postDelayed(this, 250);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(poll);
        if (Plan.MISSION_SHAKE.equals(plan.mission)) {
            sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            Sensor a = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (a != null) sensors.registerListener(this, a, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(poll);
        if (sensors != null) sensors.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        // The mission is the only way through.
    }

    /** One step solved. Returns true when the whole mission is done. */
    private boolean stepSolved() {
        View seg = segs[Math.min(solved, total - 1)];
        solved++;
        UI.haptic(seg, UI.H_CONFIRM);
        if (UI.animationsOn()) {
            final GradientDrawableHolder g = new GradientDrawableHolder(this, seg);
            g.animateToAmber();
        } else {
            seg.setBackground(UI.rr(this, UI.AMBER, 2));
        }
        updateLabel();
        if (plan.quiet) RingService.startQuiet(this);
        if (solved >= total) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    complete();
                }
            }, 520);
            return true;
        }
        return false;
    }

    /** Animates a segment from line grey to amber over 240 ms. */
    private static class GradientDrawableHolder {
        private final Context c;
        private final View v;

        GradientDrawableHolder(Context c, View v) {
            this.c = c;
            this.v = v;
        }

        void animateToAmber() {
            ValueAnimator a = ValueAnimator.ofArgb(UI.LINE, UI.AMBER);
            a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator va) {
                    v.setBackground(UI.rr(c, (Integer) va.getAnimatedValue(), 2));
                }
            });
            a.setDuration(240);
            a.setInterpolator(new DecelerateInterpolator(2f));
            a.start();
        }
    }

    private void complete() {
        if (completing) return;
        completing = true;
        RingService.send(this, RingService.ACTION_STOP_RING);
        startActivity(new Intent(this, DismissedActivity.class));
        finish();
    }

    // ---- math -----------------------------------------------------------------------------

    private void mathBody(LinearLayout root) {
        genMath();
        LinearLayout center = column();
        center.setGravity(Gravity.CENTER);
        center.setPadding(dp(24), 0, dp(24), 0);
        question = UI.text(this, "", 56, UI.T1, UI.LIGHT);
        question.setLetterSpacing(-0.02f);
        question.setGravity(Gravity.CENTER);
        center.addView(question, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        answerBox = new FrameLayout(this);
        answer = UI.text(this, "", 52, UI.T1, UI.REG);
        answer.setGravity(Gravity.CENTER);
        answerBox.addView(answer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        answerBox.setPadding(dp(28), 0, dp(28), 0);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(88));
        ap.topMargin = dp(28);
        answerBox.setMinimumWidth(dp(200));
        center.addView(answerBox, ap);

        message = UI.text(this, "Sound is quiet while you solve", 14, UI.T3, UI.MED);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, dp(16), 0, 0);
        center.addView(message);
        root.addView(center, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(keypad());
        showQuestion();
    }

    private void genMath() {
        Random r = new Random();
        questions = new String[3];
        answers = new int[3];
        for (int i = 0; i < 3; i++) {
            int a, b, c, d;
            switch (plan.difficulty) {
                case 0:
                    a = 10 + r.nextInt(50);
                    b = 10 + r.nextInt(50);
                    questions[i] = a + " + " + b;
                    answers[i] = a + b;
                    break;
                case 2:
                    a = 40 + r.nextInt(60);
                    b = 11 + r.nextInt(29);
                    c = 3 + r.nextInt(7);
                    d = 5 + r.nextInt(26);
                    questions[i] = "(" + a + " − " + b + ") × " + c + " + " + d;
                    answers[i] = (a - b) * c + d;
                    break;
                default:
                    a = 11 + r.nextInt(49);
                    b = 3 + r.nextInt(7);
                    c = 5 + r.nextInt(36);
                    questions[i] = a + " × " + b + " − " + c;
                    answers[i] = a * b - c;
                    break;
            }
        }
    }

    private void showQuestion() {
        question.setText(questions[Math.min(index, 2)]);
        input.setLength(0);
        answer.setText("");
        paintBox(UI.LINE2, UI.T1);
        if (UI.animationsOn()) UI.rise(question, 0);
    }

    private void paintBox(int border, int text) {
        answerBox.setBackground(UI.outline(this, 0, border, 2f, 20));
        answer.setTextColor(text);
    }

    private View keypad() {
        LinearLayout pad = column();
        pad.setPadding(dp(20), 0, dp(20), dp(16));
        String[][] rows = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"del", "0", "ok"}};
        for (String[] r : rows) {
            LinearLayout row = new LinearLayout(this);
            for (final String k : r) {
                View key;
                if (k.equals("ok")) {
                    FrameLayout f = new FrameLayout(this);
                    f.setBackground(UI.press(UI.rr(this, UI.AMBER, 14), UI.rr(this, UI.AMBER_PRESSED, 14)));
                    f.addView(new IconView(this, IconView.CHECK, UI.ON_AMBER).stroke(2.4f), new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER));
                    f.setContentDescription("Check answer");
                    key = f;
                } else if (k.equals("del")) {
                    TextView t = UI.text(this, "⌫", 24, UI.T1, UI.REG);
                    t.setGravity(Gravity.CENTER);
                    t.setBackground(UI.press(UI.rr(this, UI.S1, 14), UI.rr(this, UI.S2, 14)));
                    t.setContentDescription("Delete");
                    key = t;
                } else {
                    TextView t = UI.text(this, k, 28, UI.T1, UI.REG);
                    t.setGravity(Gravity.CENTER);
                    t.setBackground(UI.press(UI.rr(this, UI.S1, 14), UI.rr(this, UI.S2, 14)));
                    key = t;
                }
                key.setClickable(true);
                key.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        UI.haptic(v, UI.H_TICK);
                        key(k);
                    }
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(64), 1f);
                lp.setMargins(dp(4), dp(4), dp(4), dp(4));
                row.addView(key, lp);
            }
            pad.addView(row);
        }
        return pad;
    }

    private void key(String k) {
        if (busy || solved >= total) return;
        if (k.equals("del")) {
            if (input.length() > 0) input.setLength(input.length() - 1);
        } else if (k.equals("ok")) {
            check();
            return;
        } else if (input.length() < 5) {
            input.append(k);
        }
        answer.setText(input.toString());
        paintBox(UI.LINE2, UI.T1);
        message.setText("Sound is quiet while you solve");
        message.setTextColor(UI.T3);
    }

    private void check() {
        if (input.length() == 0) return;
        if (input.toString().equals(String.valueOf(answers[index]))) {
            busy = true;
            paintBox(UI.OK, UI.OK);
            message.setText("Correct");
            message.setTextColor(UI.OK);
            if (UI.animationsOn()) {
                answerBox.animate().scaleX(1.05f).scaleY(1.05f).setDuration(120).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        answerBox.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    }
                }).start();
            }
            boolean done = stepSolved();
            if (!done) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        index++;
                        busy = false;
                        showQuestion();
                        message.setText("Sound is quiet while you solve");
                        message.setTextColor(UI.T3);
                    }
                }, 520);
            }
        } else {
            busy = true;
            UI.haptic(answerBox, UI.H_REJECT);
            paintBox(UI.ERR, UI.ERR);
            message.setText("Not quite. Try again");
            message.setTextColor(UI.ERR);
            if (UI.animationsOn()) {
                float d = dp(10);
                ObjectAnimator a = ObjectAnimator.ofFloat(answerBox, "translationX", 0, -d, d * 0.8f, -d * 0.6f, d * 0.3f, 0);
                a.setDuration(360);
                a.setInterpolator(new DecelerateInterpolator(2f));
                a.start();
            }
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    input.setLength(0);
                    answer.setText("");
                    paintBox(UI.LINE2, UI.T1);
                    busy = false;
                }
            }, 460);
        }
    }

    // ---- typing ---------------------------------------------------------------------------

    private void typingBody(LinearLayout root) {
        final String[] phrases = {"Up and at it", "Today I get up on time", "I choose to leave this bed right now"};
        final String phrase = phrases[Math.max(0, Math.min(2, plan.difficulty))];
        LinearLayout center = column();
        center.setGravity(Gravity.CENTER);
        center.setPadding(dp(24), 0, dp(24), dp(24));
        TextView p = UI.text(this, phrase, 30, UI.T1, UI.LIGHT);
        p.setGravity(Gravity.CENTER);
        p.setLineSpacing(0, 1.15f);
        center.addView(p, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final EditText in = new EditText(this);
        in.setTextColor(UI.T1);
        in.setTextSize(22);
        in.setSingleLine(true);
        in.setGravity(Gravity.CENTER);
        in.setHint("Type it here");
        in.setHintTextColor(UI.T3);
        in.setBackground(UI.outline(this, 0, UI.LINE2, 2f, 20));
        in.setPadding(dp(16), dp(16), dp(16), dp(16));
        Paste.block(in);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(72));
        lp.topMargin = dp(28);
        center.addView(in, lp);

        message = UI.text(this, "Sound is quiet while you type", 14, UI.T3, UI.MED);
        message.setPadding(0, dp(16), 0, 0);
        center.addView(message);
        root.addView(center, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        in.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

            public void onTextChanged(CharSequence s, int a, int b, int c) {}

            public void afterTextChanged(Editable e) {
                String t = e.toString();
                boolean prefix = phrase.toLowerCase().startsWith(t.toLowerCase());
                in.setBackground(UI.outline(MissionActivity.this, 0, prefix ? UI.LINE2 : UI.ERR, 2f, 20));
                if (t.trim().equalsIgnoreCase(phrase) && !busy) {
                    busy = true;
                    in.setBackground(UI.outline(MissionActivity.this, 0, UI.OK, 2f, 20));
                    message.setText("Correct");
                    message.setTextColor(UI.OK);
                    stepSolved();
                }
            }
        });
    }

    // ---- shake ----------------------------------------------------------------------------

    private void shakeBody(LinearLayout root) {
        int[] targets = {30, 50, 100};
        shakeTarget = targets[Math.max(0, Math.min(2, plan.difficulty))];
        LinearLayout center = column();
        center.setGravity(Gravity.CENTER);
        center.setPadding(dp(24), 0, dp(24), dp(24));
        shakeCount = UI.text(this, "0", 120, UI.T1, UI.LIGHT);
        shakeCount.setGravity(Gravity.CENTER);
        center.addView(shakeCount, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView of = UI.text(this, "of " + shakeTarget + " shakes", 20, UI.T2, UI.REG);
        of.setGravity(Gravity.CENTER);
        of.setPadding(0, dp(8), 0, 0);
        center.addView(of, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        message = UI.text(this, "Shake hard. Sound is quiet while you do", 14, UI.T3, UI.MED);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, dp(24), 0, 0);
        center.addView(message, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(center, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (shakeCount == null || busy) return;
        double g = Math.sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]);
        long now = System.currentTimeMillis();
        if (g > 22 && now - lastShake > 300) {
            lastShake = now;
            shakes++;
            shakeCount.setText(String.valueOf(Math.min(shakes, shakeTarget)));
            UI.haptic(shakeCount, UI.H_TICK);
            if (plan.quiet) RingService.startQuiet(this);
            if (shakes >= shakeTarget) {
                busy = true;
                shakeCount.setTextColor(UI.OK);
                stepSolved();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor s, int accuracy) {
    }
}
