package com.wakeup.ui.sheet;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import com.wakeup.R;
import com.wakeup.ui.UI;

/** Bottom sheet: radius 28 on top, a handle, 24dp side padding. */
public class Sheet extends Dialog {
    public final LinearLayout body;

    public Sheet(Context c) {
        super(c, R.style.SheetDialog);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(UI.S1);
        float r = UI.dp(c, 28);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        g.setStroke(1, UI.LINE2);
        body.setBackground(g);
        body.setPadding(UI.dp(c, 24), UI.dp(c, 12), UI.dp(c, 24), UI.dp(c, 40));

        View handle = new View(c);
        handle.setBackground(UI.rr(c, UI.LINE2, 2));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(UI.dp(c, 32), UI.dp(c, 4));
        hp.gravity = Gravity.CENTER_HORIZONTAL;
        hp.bottomMargin = UI.dp(c, 20);
        body.addView(handle, hp);
        setContentView(body);
    }

    @Override
    public void show() {
        super.show();
        Window w = getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
        }
    }
}
