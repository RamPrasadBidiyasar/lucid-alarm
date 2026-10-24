package com.wakeup.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Design tokens and small view factories. The values match the design system boards. */
public final class UI {
    private UI() {}

    // Colour
    public static final int BG = 0xFF0B0B0C;
    public static final int S1 = 0xFF141415;
    public static final int S2 = 0xFF1D1D1F;
    public static final int LINE = 0xFF2A2A2D;
    public static final int LINE2 = 0xFF3C3C40;
    public static final int T1 = 0xFFF5F3EE;
    public static final int T2 = 0xFFA8A59E;
    public static final int T3 = 0xFF8A8780;
    public static final int AMBER = 0xFFFFB400;
    public static final int AMBER_PRESSED = 0xFFE09F00;
    public static final int WASH = 0xFF241B06;
    public static final int ON_AMBER = 0xFF1A1100;
    public static final int OK = 0xFF8FD3B0;
    public static final int ERR = 0xFFFF8A78;

    // Type
    public static final Typeface LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);
    public static final Typeface REG = Typeface.create("sans-serif", Typeface.NORMAL);
    public static final Typeface MED = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    public static final Typeface BOLD = Typeface.create("sans-serif", Typeface.BOLD);

    // Haptic constants (HapticFeedbackConstants)
    public static final int H_TICK = 3;     // KEYBOARD_TAP
    public static final int H_CONFIRM = 16; // CONFIRM
    public static final int H_REJECT = 17;  // REJECT

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    public static boolean animationsOn() {
        return ValueAnimator.areAnimatorsEnabled();
    }

    public static void haptic(View v, int type) {
        v.performHapticFeedback(type);
    }

    public static TextView text(Context c, CharSequence s, float sp, int color, Typeface tf) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(tf);
        t.setIncludeFontPadding(false);
        return t;
    }

    /** Small caps label: 12sp, semi-bold, wide tracking. */
    public static TextView caption(Context c, String s, int color) {
        TextView t = text(c, s, 12, color, BOLD);
        t.setLetterSpacing(0.14f);
        return t;
    }

    public static GradientDrawable rr(Context c, int fill, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static GradientDrawable outline(Context c, int fill, int stroke, float strokeDp, float radiusDp) {
        GradientDrawable g = rr(c, fill, radiusDp);
        g.setStroke(Math.max(1, dp(c, strokeDp)), stroke);
        return g;
    }

    public static Drawable press(Drawable normal, Drawable pressed) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, pressed);
        s.addState(new int[]{}, normal);
        return s;
    }

    public static TextView pill(Context c, String label, int fill, int pressedFill, int textColor,
                                int strokeColor, float heightDp, float sp) {
        TextView t = text(c, label, sp, textColor, BOLD);
        t.setGravity(Gravity.CENTER);
        t.setMinHeight(dp(c, heightDp));
        t.setClickable(true);
        Drawable n = strokeColor != 0 ? outline(c, fill, strokeColor, 1.5f, 500) : rr(c, fill, 500);
        Drawable p = strokeColor != 0 ? outline(c, pressedFill, strokeColor, 1.5f, 500) : rr(c, pressedFill, 500);
        t.setBackground(press(n, p));
        return t;
    }

    public static TextView primary(Context c, String label, float heightDp) {
        return pill(c, label, AMBER, AMBER_PRESSED, ON_AMBER, 0, heightDp, 17);
    }

    public static TextView secondary(Context c, String label, float heightDp) {
        return pill(c, label, 0x00000000, S2, T1, LINE2, heightDp, 16);
    }

    public static View hairline(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return v;
    }

    public static View hairline(Context c, int color) {
        View v = hairline(c);
        v.setBackgroundColor(color);
        return v;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams lpW(int h, float weight) {
        return new LinearLayout.LayoutParams(0, h, weight);
    }

    public static LinearLayout.LayoutParams lpMatch(int heightPx) {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
    }

    public static View spacer(Context c, float weight) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 0, weight));
        return v;
    }

    public static View vspace(Context c, float dp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, dp)));
        return v;
    }

    /** Fade and lift in. Skipped when the system asks for no animations. */
    public static void rise(View v, long delay) {
        if (!animationsOn()) return;
        v.setAlpha(0f);
        v.setTranslationY(dp(v.getContext(), 12));
        v.animate().alpha(1f).translationY(0).setStartDelay(delay).setDuration(400)
                .setInterpolator(new DecelerateInterpolator(2f)).start();
    }
}
