package com.wakeup.ui.sheet;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import com.wakeup.ui.UI;
import com.wakeup.util.Fmt;
import java.lang.reflect.Field;

/** Bottom sheet with three wheels: hour, minute (5 min steps), AM or PM. */
public class TimeSheet extends Sheet {
    public interface OnTime {
        void onTime(int minuteOfDay);
    }

    public TimeSheet(final Context c, String title, String subtitle, int minuteOfDay, final OnTime cb) {
        super(c);
        body.addView(UI.text(c, title, 24, UI.T1, UI.REG));
        TextView sub = UI.text(c, subtitle, 14, UI.T2, UI.REG);
        sub.setPadding(0, UI.dp(c, 4), 0, UI.dp(c, 8));
        body.addView(sub);

        int h24 = minuteOfDay / 60;
        int h12 = h24 % 12 == 0 ? 12 : h24 % 12;
        int mIdx = Math.min(11, Math.round((minuteOfDay % 60) / 5f));

        final NumberPicker hp = picker(c, 1, 12, null);
        hp.setValue(h12);
        String[] mins = new String[12];
        for (int i = 0; i < 12; i++) mins[i] = Fmt.two(i * 5);
        final NumberPicker mp = picker(c, 0, 11, mins);
        mp.setValue(mIdx);
        final NumberPicker ap = picker(c, 0, 1, new String[]{"AM", "PM"});
        ap.setValue(h24 >= 12 ? 1 : 0);
        ap.setWrapSelectorWheel(false);

        LinearLayout wheels = new LinearLayout(c);
        wheels.setGravity(Gravity.CENTER);
        wheels.addView(hp, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        wheels.addView(mp, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        wheels.addView(ap, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        body.addView(wheels, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(c, 180)));

        LinearLayout buttons = new LinearLayout(c);
        buttons.setPadding(0, UI.dp(c, 20), 0, 0);
        TextView cancel = UI.secondary(c, "Cancel", 56);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        TextView set = UI.primary(c, "Set time", 56);
        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hour = (hp.getValue() % 12) + (ap.getValue() == 1 ? 12 : 0);
                cb.onTime(hour * 60 + mp.getValue() * 5);
                dismiss();
            }
        });
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        l1.rightMargin = UI.dp(c, 12);
        buttons.addView(cancel, l1);
        buttons.addView(set, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f));
        body.addView(buttons);
    }

    private NumberPicker picker(Context c, int min, int max, String[] labels) {
        NumberPicker p = new NumberPicker(c);
        p.setMinValue(min);
        p.setMaxValue(max);
        if (labels != null) p.setDisplayedValues(labels);
        p.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        p.setWrapSelectorWheel(true);
        try {
            Field f = NumberPicker.class.getDeclaredField("mSelectionDivider");
            f.setAccessible(true);
            f.set(p, new ColorDrawable(UI.AMBER));
        } catch (Exception ignored) {
            // hidden API: the default divider is fine
        }
        return p;
    }
}
