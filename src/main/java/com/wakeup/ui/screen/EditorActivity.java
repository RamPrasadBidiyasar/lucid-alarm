package com.wakeup.ui.screen;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.wakeup.alarm.RingService;
import com.wakeup.alarm.Scheduler;
import com.wakeup.alarm.SirenPlayer;
import com.wakeup.data.PlanStore;
import com.wakeup.model.Plan;
import com.wakeup.ui.BaseActivity;
import com.wakeup.ui.Rows;
import com.wakeup.ui.Segmented;
import com.wakeup.ui.UI;
import com.wakeup.ui.sheet.Sheet;
import com.wakeup.ui.sheet.TimeSheet;
import com.wakeup.ui.widget.IconView;
import com.wakeup.ui.widget.SwitchView;
import com.wakeup.util.Fmt;

/** Create or edit one wake plan. */
public class EditorActivity extends BaseActivity {
    private Plan plan;
    private boolean existing;
    private boolean draftSaved;
    private TextView title, vStart, vStop, vMission, vConfirm, summary, vSound;
    private final FrameLayout[] chips = new FrameLayout[7];

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String id = getIntent().getStringExtra("id");
        Plan found = id == null ? null : PlanStore.get(this, id);
        existing = found != null;
        plan = existing ? found : new Plan();
        if (!existing) plan.name = "New routine";
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (draftSaved) {
            Plan s = PlanStore.get(this, plan.id);
            if (s != null) {
                plan.mission = s.mission;
                plan.difficulty = s.difficulty;
                plan.confirm = s.confirm;
                vMission.setText(plan.missionLabel());
                vConfirm.setText(plan.confirmLabel());
            }
        }
    }

    private void build() {
        LinearLayout root = column();
        LinearLayout bar = appBar(existing ? plan.name : "New routine", IconView.CLOSE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        title = (TextView) bar.getChildAt(1);
        View.OnClickListener rename = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                renameSheet();
            }
        };
        title.setOnClickListener(rename);
        title.setContentDescription("Routine name, tap to rename");
        IconView pencil = new IconView(this, IconView.EDIT, UI.T3);
        pencil.setOnClickListener(rename);
        bar.addView(pencil, new LinearLayout.LayoutParams(dp(20), dp(20)));
        withLogo(bar);
        root.addView(bar);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout col = column();
        col.setPadding(dp(24), 0, dp(24), dp(16));
        scroll.addView(col, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        col.addView(Rows.section(this, "TIMING", true));

        vStart = valueView(Fmt.time(plan.startMin));
        Rows.add(col, "First ring", null, wrapValue(vStart), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimeSheet(EditorActivity.this, "First ring", "Then every " + plan.interval + " min until " + Fmt.time(plan.stopMin),
                        plan.startMin, new TimeSheet.OnTime() {
                    @Override
                    public void onTime(int m) {
                        plan.startMin = m;
                        vStart.setText(Fmt.time(m));
                        updateSummary();
                    }
                }).show();
            }
        });
        vStop = valueView(Fmt.time(plan.stopMin));
        Rows.add(col, "Hard stop", null, wrapValue(vStop), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimeSheet(EditorActivity.this, "Hard stop", "Rings end here and the morning counts as missed",
                        plan.stopMin, new TimeSheet.OnTime() {
                    @Override
                    public void onTime(int m) {
                        plan.stopMin = m;
                        vStop.setText(Fmt.time(m));
                        updateSummary();
                    }
                }).show();
            }
        });

        // Repeat interval
        col.addView(UI.hairline(this));
        LinearLayout rep = column();
        rep.setPadding(0, dp(16), 0, dp(12));
        rep.addView(UI.text(this, "Repeat every", 16, UI.T1, UI.REG));
        final int[] opts = {1, 3, 5, 10, 15};
        int sel = 2;
        for (int i = 0; i < opts.length; i++) if (opts[i] == plan.interval) sel = i;
        Segmented seg = new Segmented(this, new String[]{"1", "3", "5", "10", "15"}, sel, new Segmented.OnSelect() {
            @Override
            public void onSelect(int i) {
                plan.interval = opts[i];
                updateSummary();
            }
        });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = dp(8);
        rep.addView(seg, sp);
        summary = UI.text(this, "", 13, UI.T3, UI.REG);
        summary.setPadding(0, dp(8), 0, 0);
        rep.addView(summary);
        col.addView(rep);
        updateSummary();

        // Days
        col.addView(UI.hairline(this));
        LinearLayout days = new LinearLayout(this);
        days.setPadding(0, dp(4), 0, dp(4));
        String[] letters = {"M", "T", "W", "T", "F", "S", "S"};
        String[] full = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (int i = 0; i < 7; i++) {
            final int d = i;
            FrameLayout cell = new FrameLayout(this);
            TextView t = UI.text(this, letters[i], 14, UI.T2, UI.MED);
            t.setGravity(Gravity.CENTER);
            cell.addView(t, new FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER));
            cell.setClickable(true);
            cell.setContentDescription(full[i]);
            cell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    plan.days ^= (1 << d);
                    paintChip(d);
                    UI.haptic(v, UI.H_TICK);
                    updateSummary();
                }
            });
            chips[i] = cell;
            days.addView(cell, new LinearLayout.LayoutParams(0, dp(48), 1f));
            paintChip(i);
        }
        col.addView(days);

        // Wake up
        col.addView(Rows.section(this, "WAKE UP", false));
        vMission = valueView(plan.missionLabel());
        Rows.add(col, "Stop each ring", null, wrapValue(vMission), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMissions(false);
            }
        });
        vConfirm = valueView(plan.confirmLabel());
        Rows.add(col, "Confirm awake", null, wrapValue(vConfirm), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMissions(true);
            }
        });

        // Sound
        col.addView(Rows.section(this, "SOUND", false));
        vSound = valueView(plan.soundLabel());
        Rows.add(col, "Sound and vibration", "Starts quiet, full volume in about 90 s", wrapValue(vSound),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        soundSheet();
                    }
                });
        col.addView(UI.hairline(this));

        TextView preview = UI.text(this, "Preview this routine", 15, UI.AMBER, UI.BOLD);
        preview.setGravity(Gravity.CENTER_VERTICAL);
        preview.setMinHeight(dp(56));
        preview.setClickable(true);
        preview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previewRing();
            }
        });
        col.addView(preview);

        if (existing && PlanStore.all(this).size() > 1) {
            TextView del = UI.text(this, "Delete routine", 15, UI.T2, UI.MED);
            del.setGravity(Gravity.CENTER_VERTICAL);
            del.setMinHeight(dp(56));
            del.setClickable(true);
            del.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDelete();
                }
            });
            col.addView(del);
        }

        // Save bar
        LinearLayout bottom = column();
        bottom.addView(UI.hairline(this));
        bottom.setPadding(dp(24), dp(12), dp(24), dp(12));
        TextView save = UI.primary(this, "Save routine", 56);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
        bottom.addView(save, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(bottom, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        show(root, UI.BG, false);
    }

    private TextView valueView(String s) {
        return UI.text(this, s, 20, UI.T1, UI.LIGHT);
    }

    private View wrapValue(TextView value) {
        LinearLayout l = new LinearLayout(this);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.addView(value);
        View gap = new View(this);
        l.addView(gap, new LinearLayout.LayoutParams(dp(8), 1));
        l.addView(Rows.chevron(this));
        return l;
    }

    private void paintChip(int i) {
        boolean on = ((plan.days >> i) & 1) == 1;
        TextView t = (TextView) chips[i].getChildAt(0);
        t.setBackground(on ? UI.rr(this, UI.AMBER, 20) : UI.outline(this, 0, UI.LINE2, 1f, 20));
        t.setTextColor(on ? UI.ON_AMBER : UI.T2);
        t.setTypeface(on ? UI.BOLD : UI.MED);
    }

    private void updateSummary() {
        if (summary == null) return;
        if (plan.stopMin < plan.startMin) {
            summary.setText("minutes · hard stop is before the first ring");
        } else {
            summary.setText("minutes · " + plan.totalRings() + " rings, " + Fmt.time(plan.startMin) + " to " + Fmt.time(plan.stopMin));
        }
    }

    private void openMissions(boolean confirmTab) {
        if (plan.name.length() == 0) plan.name = "Routine";
        PlanStore.upsert(this, plan);
        draftSaved = true;
        existing = true;
        startActivity(new Intent(this, MissionsActivity.class).putExtra("id", plan.id).putExtra("confirm", confirmTab));
    }

    private void save() {
        if (plan.name.length() == 0) plan.name = "Routine";
        if (plan.stopMin <= plan.startMin) {
            Toast.makeText(this, "The hard stop must be after the first ring", Toast.LENGTH_SHORT).show();
            return;
        }
        if (plan.days == 0) {
            Toast.makeText(this, "Pick at least one day", Toast.LENGTH_SHORT).show();
            return;
        }
        PlanStore.upsert(this, plan);
        finish();
    }

    private void soundSheet() {
        final Sheet s = new Sheet(this);
        s.body.addView(UI.text(this, "Sound and vibration", 24, UI.T1, UI.REG));
        TextView d = UI.text(this, "Starts quiet, then rises to full volume in about 90 seconds.", 15, UI.T2, UI.REG);
        d.setPadding(0, dp(4), 0, dp(12));
        s.body.addView(d);

        final String[] ids = {Plan.SOUND_SIREN, Plan.SOUND_BEACON, Plan.SOUND_CHIME};
        final String[] titles = {"Rising siren", "Pulse beacon", "Harmonic chime"};
        final String[] descs = {"Two-tone sweep", "Sonar ping", "Bell chord"};
        final FrameLayout[] radios = new FrameLayout[ids.length];

        for (int i = 0; i < ids.length; i++) {
            final int index = i;
            s.body.addView(UI.hairline(this));
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp(60));
            LinearLayout t = column();
            t.addView(UI.text(this, titles[i], 16, UI.T1, UI.MED));
            TextView sub = UI.text(this, descs[i], 13, UI.T3, UI.REG);
            sub.setPadding(0, dp(2), 0, 0);
            t.addView(sub);
            row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            radios[i] = new FrameLayout(this);
            row.addView(radios[i], new LinearLayout.LayoutParams(dp(24), dp(24)));
            paintRadio(radios[i], ids[i].equals(plan.sound));
            row.setClickable(true);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    plan.sound = ids[index];
                    vSound.setText(plan.soundLabel());
                    for (int k = 0; k < radios.length; k++) paintRadio(radios[k], k == index);
                    UI.haptic(v, UI.H_TICK);
                    playPreview(ids[index]);
                }
            });
            s.body.addView(row);
        }
        Rows.add(s.body, "Quiet while solving", "Mutes for 60 s, restarts on each answer",
                new SwitchView(this, plan.quiet, new SwitchView.OnChange() {
                    @Override
                    public void onChange(boolean c) {
                        plan.quiet = c;
                    }
                }), null);
        Rows.add(s.body, "Vibration", null, new SwitchView(this, plan.vibration, new SwitchView.OnChange() {
            @Override
            public void onChange(boolean c) {
                plan.vibration = c;
            }
        }), null);
        s.body.addView(UI.hairline(this));
        TextView done = UI.primary(this, "Done", 56);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s.dismiss();
            }
        });
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dp.topMargin = dp(16);
        s.body.addView(done, dp);
        s.show();
    }

    private void paintRadio(FrameLayout radio, boolean on) {
        radio.removeAllViews();
        radio.setBackground(on ? UI.rr(this, UI.AMBER, 12) : UI.outline(this, 0, UI.LINE2, 2f, 12));
        if (on) radio.addView(new IconView(this, IconView.CHECK, UI.ON_AMBER).stroke(3.2f),
                new FrameLayout.LayoutParams(dp(14), dp(14), Gravity.CENTER));
    }

    /** One second of the sound at half gain so you can hear what you picked. */
    private void playPreview(String sound) {
        final SirenPlayer preview = new SirenPlayer();
        preview.setSound(sound);
        preview.setGain(0.5f);
        preview.start();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                preview.stop();
            }
        }, 1000);
    }

    private void renameSheet() {
        final Sheet s = new Sheet(this);
        s.body.addView(UI.text(this, "Routine name", 24, UI.T1, UI.REG));
        final EditText in = new EditText(this);
        in.setText(plan.name);
        in.setSelection(in.getText().length());
        in.setSingleLine(true);
        in.setTextColor(UI.T1);
        in.setHint("Name");
        in.setHintTextColor(UI.T3);
        in.setBackground(UI.outline(this, 0, UI.LINE2, 1f, 14));
        in.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        ip.topMargin = dp(16);
        s.body.addView(in, ip);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, dp(16), 0, 0);
        TextView cancel = UI.secondary(this, "Cancel", 56);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s.dismiss();
            }
        });
        TextView save = UI.primary(this, "Save", 56);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String n = in.getText().toString().trim();
                if (n.length() > 0) {
                    plan.name = n;
                    title.setText(n);
                }
                s.dismiss();
            }
        });
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        l1.rightMargin = dp(12);
        row.addView(cancel, l1);
        row.addView(save, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f));
        s.body.addView(row);
        s.show();
        in.requestFocus();
    }

    /** Rings once with this routine's sound and mission. Nothing is cancelled or logged. */
    private void previewRing() {
        if (plan.name.length() == 0) plan.name = "Routine";
        PlanStore.upsert(this, plan);
        existing = true;
        draftSaved = true;
        Intent svc = new Intent(this, RingService.class).setAction(RingService.ACTION_START);
        svc.putExtra(Scheduler.EXTRA_PLAN, plan.id);
        svc.putExtra(Scheduler.EXTRA_TIME, System.currentTimeMillis());
        svc.putExtra(Scheduler.EXTRA_TEST, true);
        startForegroundService(svc);
        startActivity(new Intent(this, RingActivity.class));
    }

    private void confirmDelete() {
        final Sheet s = new Sheet(this);
        s.body.addView(UI.text(this, "Delete this routine?", 24, UI.T1, UI.REG));
        TextView d = UI.text(this, "Its alarms will stop being scheduled.", 15, UI.T2, UI.REG);
        d.setPadding(0, dp(8), 0, dp(20));
        s.body.addView(d);
        LinearLayout row = new LinearLayout(this);
        TextView no = UI.secondary(this, "Keep it", 56);
        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s.dismiss();
            }
        });
        TextView yes = UI.primary(this, "Delete", 56);
        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlanStore.delete(EditorActivity.this, plan.id);
                s.dismiss();
                finish();
            }
        });
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        a.rightMargin = dp(12);
        row.addView(no, a);
        row.addView(yes, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        s.body.addView(row);
        s.show();
    }
}
