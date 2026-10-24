package com.wakeup.ui;

import android.content.Context;
import android.graphics.Outline;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Single-choice segmented control. Selected segment is amber. */
public class Segmented extends LinearLayout {
    public interface OnSelect {
        void onSelect(int index);
    }

    private final TextView[] items;
    private int selected;
    private final OnSelect listener;

    public Segmented(Context c, String[] labels, int selected, OnSelect l) {
        super(c);
        this.selected = selected;
        this.listener = l;
        setOrientation(HORIZONTAL);
        final int radius = UI.dp(c, 14);
        setBackground(UI.outline(c, 0, UI.LINE2, 1f, 14));
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            }
        });
        items = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            if (i > 0) {
                View sep = new View(c);
                sep.setBackgroundColor(UI.LINE);
                addView(sep, new LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            final int idx = i;
            TextView t = UI.text(c, labels[i], 15, UI.T1, UI.MED);
            t.setGravity(Gravity.CENTER);
            t.setMinHeight(UI.dp(c, 48));
            t.setClickable(true);
            t.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (idx == Segmented.this.selected) return;
                    select(idx);
                    UI.haptic(v, UI.H_TICK);
                    if (listener != null) listener.onSelect(idx);
                }
            });
            items[i] = t;
            addView(t, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        paint();
    }

    public void select(int i) {
        selected = i;
        paint();
    }

    private void paint() {
        for (int i = 0; i < items.length; i++) {
            boolean on = i == selected;
            items[i].setBackgroundColor(on ? UI.AMBER : 0);
            items[i].setTextColor(on ? UI.ON_AMBER : UI.T1);
            items[i].setTypeface(on ? UI.BOLD : UI.MED);
            items[i].setSelected(on);
        }
    }
}
