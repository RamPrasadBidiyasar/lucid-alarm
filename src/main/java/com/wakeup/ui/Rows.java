package com.wakeup.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.wakeup.ui.widget.IconView;

/** List rows with a hairline above and no box. */
public final class Rows {
    private Rows() {}

    public static TextView section(Context c, String label, boolean first) {
        TextView t = UI.caption(c, label, UI.T3);
        t.setPadding(0, UI.dp(c, first ? 12 : 24), 0, UI.dp(c, 8));
        return t;
    }

    /**
     * Adds a hairline and a row to the parent.
     *
     * @param sub      optional second line, 13sp
     * @param trailing optional view at the end
     * @param click    optional click listener for the whole row
     */
    public static LinearLayout add(LinearLayout parent, String title, String sub, View trailing,
                                   View.OnClickListener click) {
        Context c = parent.getContext();
        parent.addView(UI.hairline(c));
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(UI.dp(c, sub == null ? 56 : 62));
        LinearLayout text = new LinearLayout(c);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(UI.text(c, title, 16, UI.T1, UI.REG));
        if (sub != null) {
            TextView s = UI.text(c, sub, 13, UI.T3, UI.REG);
            s.setPadding(0, UI.dp(c, 2), 0, 0);
            text.addView(s);
        }
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (trailing != null) row.addView(trailing);
        if (click != null) {
            row.setClickable(true);
            row.setOnClickListener(click);
        }
        parent.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    public static IconView chevron(Context c) {
        IconView v = new IconView(c, IconView.CHEV_R, UI.T3);
        v.setLayoutParams(new LinearLayout.LayoutParams(UI.dp(c, 20), UI.dp(c, 20)));
        return v;
    }

    /** Value text with a trailing chevron, for navigation rows. */
    public static LinearLayout valueChevron(Context c, String value, boolean light) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        TextView v = light ? UI.text(c, value, 20, UI.T1, UI.LIGHT) : UI.text(c, value, 15, UI.T2, UI.REG);
        l.addView(v);
        View gap = new View(c);
        l.addView(gap, new LinearLayout.LayoutParams(UI.dp(c, 8), 1));
        l.addView(chevron(c));
        return l;
    }
}
