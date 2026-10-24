package com.wakeup.ui.screen;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.wakeup.alarm.RingService;
import com.wakeup.alarm.Scheduler;
import com.wakeup.data.PlanStore;
import com.wakeup.model.Plan;
import com.wakeup.ui.BaseActivity;
import com.wakeup.ui.UI;
import com.wakeup.ui.widget.IconView;
import com.wakeup.util.Fmt;

/** Ring stopped. Makes it plain that you are not marked awake until you scan. */
public class DismissedActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setupLockscreen();
        String pid = getIntent().getStringExtra("planId");
        if (pid == null) pid = RingService.planId;
        final Plan plan = pid != null ? PlanStore.get(this, pid) : null;
        final String targetPlanId = pid;
        final int ringNo = RingService.ringIndex;
        long now = System.currentTimeMillis();
        Scheduler.Next next = Scheduler.next(this, now);
        int remaining = Scheduler.remainingToday(this);

        LinearLayout root = column();
        root.setPadding(0, 0, 0, 0);

        LinearLayout top = column();
        top.setPadding(dp(24), dp(44), dp(24), 0);
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout ok = new FrameLayout(this);
        ok.setBackground(UI.outline(this, 0, UI.OK, 2f, 16));
        ok.addView(new IconView(this, IconView.CHECK, UI.OK).stroke(3f), new FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER));
        head.addView(ok, new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView t = UI.text(this, "Ring " + ringNo + " stopped", 16, UI.T1, UI.MED);
        t.setPadding(dp(12), 0, 0, 0);
        head.addView(t);
        top.addView(head);

        if (next != null) {
            TextView cap = UI.caption(this, "NEXT RING", UI.T2);
            cap.setPadding(0, dp(44), 0, dp(8));
            top.addView(cap);
            LinearLayout timeRow = new LinearLayout(this);
            int mod = Fmt.minuteOfDay(next.time);
            TextView time = UI.text(this, Fmt.hm(mod), 108, UI.T1, UI.LIGHT);
            time.setLetterSpacing(-0.03f);
            timeRow.addView(time);
            TextView ap = UI.text(this, " " + Fmt.ampm(mod), 28, UI.T2, UI.REG);
            LinearLayout.LayoutParams apl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            apl.gravity = Gravity.BOTTOM;
            apl.bottomMargin = dp(18);
            timeRow.addView(ap, apl);
            top.addView(timeRow);
            TextView until = UI.text(this, Fmt.until(next.time), 20, UI.T1, UI.MED);
            until.setPadding(0, dp(12), 0, 0);
            top.addView(until);
        }
        root.addView(top);

        LinearLayout mid = column();
        mid.setPadding(dp(24), dp(44), dp(24), 0);
        mid.addView(UI.hairline(this));
        TextView h = UI.text(this, "You're not marked awake yet", 24, UI.T1, UI.REG);
        h.setPadding(0, dp(24), 0, dp(8));
        mid.addView(h);

        String method = plan != null ? plan.confirm : Plan.CONFIRM_PHRASE;
        String desc;
        String btnLabel;
        int btnIcon = IconView.CHECK;
        String remainingStr = remaining == 0
                ? "All rings complete for today. "
                : (remaining == 1 ? "1 more ring scheduled today. " : remaining + " more rings scheduled today. ");
        if (Plan.CONFIRM_PHRASE.equals(method)) {
            desc = remainingStr + (remaining == 0 ? "Type phrase to confirm you are awake." : "Type your wake-up phrase to cancel all of them.");
            btnLabel = "Type phrase to wake up";
            btnIcon = IconView.KEYBOARD;
        } else if (Plan.CONFIRM_HOLD.equals(method)) {
            desc = remainingStr + "Hold for 5 seconds to confirm you are awake.";
            btnLabel = "Hold 5 sec to wake up";
            btnIcon = IconView.CHECK;
        } else if (Plan.CONFIRM_NFC.equals(method)) {
            desc = remainingStr + (remaining == 0 ? "Tap NFC sticker to confirm you are awake." : "Tap your NFC sticker to cancel all of them.");
            btnLabel = "Tap NFC tag to wake up";
            btnIcon = IconView.QR;
        } else {
            desc = remainingStr + (remaining == 0 ? "Scan code or any bathroom item to confirm you are awake." : "Scan code or any bathroom item to cancel all of them.");
            btnLabel = "Scan code to wake up";
            btnIcon = IconView.QR;
        }

        TextView d = UI.text(this, desc, 16, UI.T2, UI.REG);
        d.setLineSpacing(0, 1.2f);
        mid.addView(d);
        root.addView(mid);

        root.addView(UI.spacer(this, 1f));

        LinearLayout bottom = column();
        bottom.setPadding(dp(24), 0, dp(24), dp(24));
        TextView scope = UI.caption(this, "CANCELS ALL REMAINING TODAY", UI.T3);
        scope.setGravity(Gravity.CENTER);
        scope.setPadding(0, 0, 0, dp(8));
        bottom.addView(scope, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout scan = new LinearLayout(this);
        scan.setGravity(Gravity.CENTER);
        scan.setMinimumHeight(dp(64));
        scan.setBackground(UI.press(UI.rr(this, UI.AMBER, 500), UI.rr(this, UI.AMBER_PRESSED, 500)));
        scan.addView(new IconView(this, btnIcon, UI.ON_AMBER).stroke(1.9f), new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView st = UI.text(this, btnLabel, 18, UI.ON_AMBER, UI.BOLD);
        st.setPadding(dp(12), 0, 0, 0);
        scan.addView(st);
        scan.setClickable(true);
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(DismissedActivity.this, ScanActivity.class);
                if (targetPlanId != null) i.putExtra("planId", targetPlanId);
                startActivity(i);
            }
        });
        bottom.addView(scan, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView close = UI.text(this, "Close. The next ring will still come", 15, UI.T2, UI.MED);
        close.setGravity(Gravity.CENTER);
        close.setMinHeight(dp(48));
        close.setClickable(true);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goHome();
            }
        });
        bottom.addView(close, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(bottom);

        show(root, UI.BG, false);
        UI.rise(top, 0);
        UI.rise(mid, 120);
        UI.rise(bottom, 240);
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
