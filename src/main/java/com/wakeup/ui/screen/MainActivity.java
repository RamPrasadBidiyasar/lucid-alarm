package com.wakeup.ui.screen;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.wakeup.alarm.Reliability;
import com.wakeup.alarm.RingService;
import com.wakeup.alarm.Scheduler;
import com.wakeup.data.MorningState;
import com.wakeup.data.PlanStore;
import com.wakeup.model.Plan;
import com.wakeup.ui.BaseActivity;
import com.wakeup.ui.Rows;
import com.wakeup.ui.UI;
import com.wakeup.ui.widget.IconView;
import com.wakeup.ui.widget.SwitchView;
import com.wakeup.util.Fmt;
import java.util.List;

/** Home: the next alarm is the hero, the routines sit quietly below. */
public class MainActivity extends BaseActivity {
    private FrameLayout frame;
    private String openId;
    private boolean firstRender = true;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        frame = new FrameLayout(this);
        show(frame, UI.BG, false);
        Scheduler.scheduleNext(this); // self-heal: make sure the next ring is armed
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (RingService.ringing) {
            startActivity(new Intent(this, RingActivity.class));
        }
        render();
    }

    private void render() {
        frame.removeAllViews();
        final List<Plan> plans = PlanStore.all(this);
        if (openId == null && !plans.isEmpty()) openId = plans.get(0).id;

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout col = column();
        scroll.addView(col, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        frame.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // App bar
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(24), 0, dp(12), 0);
        bar.addView(UI.text(this, "WakeUp", 15, UI.T1, UI.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        FrameLayout settings = new FrameLayout(this);
        settings.addView(new IconView(this, IconView.SLIDERS, UI.T1), new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        settings.setClickable(true);
        settings.setContentDescription("Settings and reliability");
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        bar.addView(settings, new LinearLayout.LayoutParams(dp(48), dp(48)));
        col.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        // Hero
        Scheduler.Next n = Scheduler.next(this, System.currentTimeMillis());
        LinearLayout hero = column();
        hero.setPadding(dp(24), dp(32), dp(24), 0);
        if (n == null) {
            hero.addView(UI.caption(this, "NO ALARM ARMED", UI.T2));
            TextView off = UI.text(this, "Off", 96, UI.T1, UI.LIGHT);
            off.setPadding(0, dp(8), 0, 0);
            hero.addView(off);
            TextView d = UI.text(this, "Turn on a routine below to arm your alarm.", 15, UI.T2, UI.REG);
            d.setPadding(0, dp(12), 0, 0);
            hero.addView(d);
        } else {
            hero.addView(UI.caption(this, Fmt.dayLabel(n.time), UI.T2));
            LinearLayout timeRow = new LinearLayout(this);
            timeRow.setOrientation(LinearLayout.HORIZONTAL);
            timeRow.setPadding(0, dp(8), 0, 0);
            int mod = Fmt.minuteOfDay(n.time);
            TextView time = UI.text(this, Fmt.hm(mod), 108, UI.T1, UI.LIGHT);
            time.setLetterSpacing(-0.03f);
            time.setIncludeFontPadding(false);
            timeRow.addView(time);
            TextView ap = UI.text(this, " " + Fmt.ampm(mod), 28, UI.T2, UI.REG);
            LinearLayout.LayoutParams ap2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ap2.gravity = Gravity.BOTTOM;
            ap2.bottomMargin = dp(18);
            timeRow.addView(ap, ap2);
            hero.addView(timeRow);
            TextView until = UI.text(this, Fmt.until(n.time), 20, UI.T1, UI.MED);
            until.setPadding(0, dp(12), 0, 0);
            hero.addView(until);
            TextView d = UI.text(this, n.plan.name + " · every " + n.plan.interval + " min until " + Fmt.time(n.plan.stopMin), 15, UI.T2, UI.REG);
            d.setPadding(0, dp(4), 0, 0);
            hero.addView(d);
            long later = Scheduler.firstOnLaterDay(this, n.time);
            if (later > 0) {
                TextView th = UI.text(this, "Then " + Fmt.dayShort(later) + " " + Fmt.time(later), 14, UI.T3, UI.REG);
                th.setPadding(0, dp(2), 0, 0);
                hero.addView(th);
            }
        }
        col.addView(hero);
        if (firstRender) UI.rise(hero, 0);
        firstRender = false;

        String result = MorningState.resultToday(this);
        if (result != null) {
            LinearLayout r = new LinearLayout(this);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(dp(24), dp(20), dp(24), 0);
            r.addView(new IconView(this, IconView.CHECK, UI.OK), new LinearLayout.LayoutParams(dp(18), dp(18)));
            TextView t = UI.text(this, result, 14, UI.T2, UI.REG);
            t.setPadding(dp(10), 0, 0, 0);
            r.addView(t);
            col.addView(r);
        }

        // Reliability row
        int issues = Reliability.issueCount(this);
        if (issues > 0) {
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rp.setMargins(dp(24), dp(28), dp(24), 0);
            LinearLayout wrap = column();
            wrap.addView(UI.hairline(this));
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp(52));
            View dot = new View(this);
            dot.setBackground(UI.rr(this, UI.AMBER, 4));
            row.addView(dot, new LinearLayout.LayoutParams(dp(8), dp(8)));
            if (UI.animationsOn()) {
                ObjectAnimator a = ObjectAnimator.ofFloat(dot, "alpha", 1f, 0.35f);
                a.setDuration(1200);
                a.setRepeatCount(ValueAnimator.INFINITE);
                a.setRepeatMode(ValueAnimator.REVERSE);
                a.start();
            }
            TextView t = UI.text(this, issues + (issues == 1 ? " setting needed." : " settings needed.") + " The alarm may not ring on time.", 14, UI.T1, UI.REG);
            t.setPadding(dp(12), 0, dp(8), 0);
            row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(Rows.chevron(this));
            wrap.addView(row);
            wrap.addView(UI.hairline(this));
            wrap.setClickable(true);
            wrap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                }
            });
            col.addView(wrap, rp);
        }

        // Routines
        LinearLayout list = column();
        list.setPadding(dp(24), dp(28), dp(24), dp(120));
        list.addView(UI.caption(this, "ROUTINES", UI.T3));
        View gap = new View(this);
        list.addView(gap, new LinearLayout.LayoutParams(1, dp(4)));
        for (Plan p : plans) list.addView(planRow(p));
        list.addView(UI.hairline(this));
        col.addView(list);

        // FAB
        FrameLayout fab = new FrameLayout(this);
        fab.setBackground(UI.press(UI.rr(this, UI.AMBER, 20), UI.rr(this, UI.AMBER_PRESSED, 20)));
        fab.addView(new IconView(this, IconView.PLUS, UI.ON_AMBER).stroke(2.2f), new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER));
        fab.setClickable(true);
        fab.setContentDescription("New routine");
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EditorActivity.class));
            }
        });
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(dp(60), dp(60), Gravity.BOTTOM | Gravity.END);
        fp.setMargins(0, 0, dp(24), dp(24));
        frame.addView(fab, fp);
    }

    private View planRow(final Plan p) {
        LinearLayout item = column();
        item.addView(UI.hairline(this));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout text = column();
        TextView name = UI.text(this, p.name, 17, UI.T1, UI.MED);
        name.setAlpha(p.enabled ? 1f : 0.5f);
        text.addView(name);
        TextView sub = UI.text(this, Fmt.time(p.startMin) + " · " + p.daysLabel(), 14, UI.T2, UI.REG);
        sub.setPadding(0, dp(2), 0, 0);
        text.addView(sub);
        text.setMinimumHeight(dp(72));
        text.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final IconView chev = new IconView(this, IconView.CHEV_D, UI.T3);
        head.addView(chev, new LinearLayout.LayoutParams(dp(20), dp(20)));
        View g = new View(this);
        head.addView(g, new LinearLayout.LayoutParams(dp(4), 1));
        head.addView(new SwitchView(this, p.enabled, new SwitchView.OnChange() {
            @Override
            public void onChange(boolean checked) {
                p.enabled = checked;
                PlanStore.upsert(MainActivity.this, p);
                render();
            }
        }));
        item.addView(head);

        // Expandable details
        final LinearLayout details = column();
        details.setPadding(0, 0, 0, dp(8));
        LinearLayout r1 = new LinearLayout(this);
        r1.addView(detail("REPEAT", "Every " + p.interval + " min"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r1.addView(detail("HARD STOP", Fmt.time(p.stopMin)), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        details.addView(r1);
        LinearLayout r2 = new LinearLayout(this);
        r2.setPadding(0, dp(12), 0, 0);
        r2.addView(detail("MISSION", p.missionLabel()), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r2.addView(detail("CONFIRM AWAKE", p.confirmLabel()), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        details.addView(r2);
        TextView edit = UI.text(this, "Edit routine", 15, UI.AMBER, UI.BOLD);
        edit.setGravity(Gravity.CENTER_VERTICAL);
        edit.setMinHeight(dp(48));
        edit.setClickable(true);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EditorActivity.class).putExtra("id", p.id));
            }
        });
        details.addView(edit);

        final FrameLayout clip = new FrameLayout(this);
        clip.addView(details);
        item.addView(clip, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
        final boolean open = p.id.equals(openId);
        details.measure(View.MeasureSpec.makeMeasureSpec(getResources().getDisplayMetrics().widthPixels - dp(48), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED);
        final int full = details.getMeasuredHeight();
        clip.getLayoutParams().height = open ? full : 0;
        chev.setRotation(open ? 180 : 0);

        head.setClickable(true);
        head.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean nowOpen = clip.getLayoutParams().height == 0;
                openId = nowOpen ? p.id : "";
                UI.haptic(v, UI.H_TICK);
                animateHeight(clip, nowOpen ? full : 0);
                chev.animate().rotation(nowOpen ? 180 : 0).setDuration(240).setInterpolator(new DecelerateInterpolator(2f)).start();
            }
        });
        return item;
    }

    private void animateHeight(final View v, int to) {
        final int from = v.getLayoutParams().height;
        if (!UI.animationsOn()) {
            v.getLayoutParams().height = to;
            v.requestLayout();
            return;
        }
        ValueAnimator a = ValueAnimator.ofInt(from, to);
        a.setDuration(240);
        a.setInterpolator(new DecelerateInterpolator(2f));
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator va) {
                v.getLayoutParams().height = (Integer) va.getAnimatedValue();
                v.requestLayout();
            }
        });
        a.start();
    }

    private View detail(String label, String value) {
        LinearLayout l = column();
        TextView a = UI.text(this, label, 12, UI.T3, UI.REG);
        a.setLetterSpacing(0.04f);
        l.addView(a);
        TextView b = UI.text(this, value, 15, UI.T1, UI.MED);
        b.setPadding(0, dp(2), 0, 0);
        l.addView(b);
        return l;
    }
}
