package com.wakeup.ui.screen;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.wakeup.data.MorningState;
import com.wakeup.data.PlanStore;
import com.wakeup.model.Plan;
import com.wakeup.ui.BaseActivity;
import com.wakeup.ui.Rows;
import com.wakeup.ui.Segmented;
import com.wakeup.ui.UI;
import com.wakeup.ui.widget.IconView;
import java.util.ArrayList;
import java.util.List;

/** Choose the mission that stops a ring, and the one that confirms you are awake. */
public class MissionsActivity extends BaseActivity {
    private Plan plan;
    private boolean confirmTab;
    private LinearLayout tabs;
    private TextView blurb;
    private LinearLayout list;
    private TextView cta;

    private static class Item {
        final String id, name, desc;
        final String[] opts;
        final String[] examples;
        final int icon;

        Item(String id, String name, String desc, int icon, String[] opts, String[] examples) {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.icon = icon;
            this.opts = opts;
            this.examples = examples;
        }
    }

    private final Item[] stopItems = {
            new Item(Plan.MISSION_MATH, "Math", "Three problems", IconView.CALC,
                    new String[]{"Easy", "Medium", "Hard"}, new String[]{"7 + 5", "24 + 19", "24 × 5 − 18"}),
            new Item(Plan.MISSION_TYPING, "Typing", "Retype a sentence, paste blocked", IconView.KEYBOARD,
                    new String[]{"Short", "Medium", "Long"}, new String[]{"Up and at it", "Today I get up on time", "I choose to leave this bed right now"}),
            new Item(Plan.MISSION_SHAKE, "Shake", "Shake the phone", IconView.SHAKE,
                    new String[]{"30", "50", "100"}, new String[]{"Shake 30 times", "Shake 50 times", "Shake 100 times"})
    };
    private final Item[] confirmItems = {
            new Item(Plan.CONFIRM_PHRASE, "Type a phrase", "Type a short affirmation to cancel alarms", IconView.KEYBOARD,
                    new String[0], new String[]{"Type: I am awake"}),
            new Item(Plan.CONFIRM_HOLD, "Hold 5 seconds", "Touch and hold button to prove you are up", IconView.CHECK,
                    new String[0], new String[]{"Firm 5-second press with haptics"}),
            new Item(Plan.CONFIRM_QR, "Scan QR or Barcode", "Code or any bathroom item (toothpaste, soap)", IconView.QR,
                    new String[0], new String[]{"Walk there, then scan code or package"}),
            new Item(Plan.CONFIRM_NFC, "Tap NFC tag", "A sticker in the bathroom or kitchen", IconView.QR,
                    new String[0], new String[]{"Touch phone to the sticker"})
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        plan = PlanStore.get(this, getIntent().getStringExtra("id"));
        if (plan == null) {
            finish();
            return;
        }
        confirmTab = getIntent().getBooleanExtra("confirm", false);

        LinearLayout root = column();
        root.addView(appBar("Missions", IconView.CHEV_L, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        }));

        tabs = new LinearLayout(this);
        tabs.setPadding(dp(24), dp(8), dp(24), 0);
        root.addView(tabs);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout col = column();
        col.setPadding(dp(24), 0, dp(24), dp(16));
        blurb = UI.text(this, "", 14, UI.T2, UI.REG);
        blurb.setLineSpacing(0, 1.25f);
        blurb.setPadding(0, dp(16), 0, dp(8));
        col.addView(blurb);
        list = column();
        col.addView(list);
        scroll.addView(col, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bottom = column();
        bottom.addView(UI.hairline(this));
        bottom.setPadding(dp(24), dp(12), dp(24), dp(12));
        cta = UI.primary(this, "", 56);
        cta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        bottom.addView(cta, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(bottom);

        show(root, UI.BG, false);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (plan != null && list != null) render();
    }

    private void render() {
        // Tabs
        tabs.removeAllViews();
        tabs.addView(tab("Stop a ring", !confirmTab, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tabs.addView(tab("Confirm awake", confirmTab, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        blurb.setText(confirmTab
                ? "Completing this proves you are out of bed and cancels every remaining ring today."
                : "Solving it silences only the current ring. The next one still comes.");

        list.removeAllViews();
        Item[] items = confirmTab ? visibleConfirmItems() : stopItems;
        String selected = confirmTab ? plan.confirm : plan.mission;
        Item chosen = items[0];
        for (final Item it : items) {
            boolean sel = it.id.equals(selected);
            if (sel) chosen = it;
            list.addView(UI.hairline(this));
            list.addView(itemRow(it, sel));
        }
        list.addView(UI.hairline(this));
        list.addView(Rows.section(this, "LATER", false));
        String[] later = confirmTab ? new String[]{"Photo of a place", "Steps"} : new String[]{"Memory", "Steps"};
        for (String s : later) {
            LinearLayout r = new LinearLayout(this);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setMinimumHeight(dp(52));
            r.setAlpha(0.7f);
            TextView t = UI.text(this, s, 16, UI.T1, UI.REG);
            r.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView soon = UI.text(this, "SOON", 11, UI.T3, UI.BOLD);
            soon.setLetterSpacing(0.08f);
            soon.setPadding(dp(8), dp(2), dp(8), dp(2));
            soon.setBackground(UI.outline(this, 0, UI.LINE2, 1f, 12));
            r.addView(soon);
            list.addView(UI.hairline(this));
            list.addView(r);
        }
        cta.setText("Use " + chosen.name);
    }

    /** NFC is only offered on phones that have it, or when this routine already uses it. */
    private Item[] visibleConfirmItems() {
        boolean hasNfc = NfcAdapter.getDefaultAdapter(this) != null || Plan.CONFIRM_NFC.equals(plan.confirm);
        if (hasNfc) return confirmItems;
        List<Item> out = new ArrayList<Item>();
        for (Item it : confirmItems) if (!Plan.CONFIRM_NFC.equals(it.id)) out.add(it);
        return out.toArray(new Item[0]);
    }

    private View tab(String label, boolean on, final boolean toConfirm) {
        LinearLayout t = column();
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView tv = UI.text(this, label, 15, on ? UI.T1 : UI.T2, on ? UI.BOLD : UI.MED);
        tv.setGravity(Gravity.CENTER);
        t.addView(tv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
        View u = new View(this);
        u.setBackgroundColor(on ? UI.AMBER : UI.LINE);
        t.addView(u, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, on ? dp(2) : 1));
        t.setClickable(true);
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (confirmTab == toConfirm) return;
                confirmTab = toConfirm;
                UI.haptic(v, UI.H_TICK);
                render();
            }
        });
        return t;
    }

    private View itemRow(final Item it, boolean sel) {
        LinearLayout wrap = column();
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setMinimumHeight(dp(72));
        head.addView(new IconView(this, it.icon, sel ? UI.AMBER : UI.T2), new LinearLayout.LayoutParams(dp(24), dp(24)));
        LinearLayout text = column();
        text.setPadding(dp(16), 0, dp(8), 0);
        text.addView(UI.text(this, it.name, 17, UI.T1, UI.MED));
        TextView d = UI.text(this, it.desc, 14, UI.T2, UI.REG);
        d.setPadding(0, dp(2), 0, 0);
        text.addView(d);
        head.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        FrameLayout radio = new FrameLayout(this);
        radio.setBackground(sel ? UI.rr(this, UI.AMBER, 12) : UI.outline(this, 0, UI.LINE2, 2f, 12));
        if (sel) radio.addView(new IconView(this, IconView.CHECK, UI.ON_AMBER).stroke(3.2f),
                new FrameLayout.LayoutParams(dp(14), dp(14), Gravity.CENTER));
        head.addView(radio, new LinearLayout.LayoutParams(dp(24), dp(24)));
        wrap.addView(head);

        head.setClickable(true);
        head.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (confirmTab) plan.confirm = it.id;
                else plan.mission = it.id;
                PlanStore.upsert(MissionsActivity.this, plan);
                UI.haptic(v, UI.H_TICK);
                render();
            }
        });

        if (sel) {
            LinearLayout more = column();
            more.setPadding(dp(40), 0, 0, dp(20));
            int di = Math.max(0, Math.min(it.examples.length - 1, plan.difficulty));
            final TextView ex = UI.text(this, it.examples[it.opts.length > 0 ? di : 0], 22, UI.T1, UI.LIGHT);
            if (it.opts.length > 0) {
                int idx = Math.max(0, Math.min(it.opts.length - 1, plan.difficulty));
                Segmented seg = new Segmented(this, it.opts, idx, new Segmented.OnSelect() {
                    @Override
                    public void onSelect(int i) {
                        plan.difficulty = i;
                        PlanStore.upsert(MissionsActivity.this, plan);
                        ex.setText(it.examples[Math.min(i, it.examples.length - 1)]);
                    }
                });
                LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                sp.bottomMargin = dp(12);
                more.addView(seg, sp);
            }
            more.addView(ex);
            if (Plan.CONFIRM_QR.equals(it.id)) {
                TextView code = UI.text(this, MorningState.qrReady(this) ? "Show my code" : "Create and show my code", 15, UI.AMBER, UI.BOLD);
                code.setGravity(Gravity.CENTER_VERTICAL);
                code.setMinHeight(dp(48));
                code.setClickable(true);
                code.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(MissionsActivity.this, QrSetupActivity.class));
                    }
                });
                more.addView(code);
            }
            wrap.addView(more);
        }
        return wrap;
    }
}
