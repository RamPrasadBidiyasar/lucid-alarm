package com.wakeup.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.DecelerateInterpolator;
import com.wakeup.ui.UI;

/** 52 by 32 track in a 56 by 48 touch target. Flat, amber when on. */
public class SwitchView extends View {
    public interface OnChange {
        void onChange(boolean checked);
    }

    private boolean checked;
    private float t; // 0 off, 1 on
    private OnChange listener;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private ValueAnimator anim;

    public SwitchView(Context c, boolean checked, OnChange l) {
        super(c);
        this.checked = checked;
        this.t = checked ? 1f : 0f;
        this.listener = l;
        setClickable(true);
        setMinimumWidth(UI.dp(c, 56));
        setMinimumHeight(UI.dp(c, 48));
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setChecked(!SwitchView.this.checked, true);
                UI.haptic(v, UI.H_TICK);
            }
        });
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean on, boolean notify) {
        if (on == checked) return;
        checked = on;
        if (anim != null) anim.cancel();
        if (UI.animationsOn()) {
            anim = ValueAnimator.ofFloat(t, on ? 1f : 0f);
            anim.setDuration(160);
            anim.setInterpolator(new DecelerateInterpolator(2f));
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator a) {
                    t = (Float) a.getAnimatedValue();
                    invalidate();
                }
            });
            anim.start();
        } else {
            t = on ? 1f : 0f;
            invalidate();
        }
        if (notify && listener != null) listener.onChange(on);
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(UI.dp(getContext(), 56), UI.dp(getContext(), 48));
    }

    @Override
    protected void onDraw(Canvas c) {
        float d = getResources().getDisplayMetrics().density;
        float tw = 52 * d, th = 32 * d;
        float left = getWidth() - tw, top = (getHeight() - th) / 2f;
        rect.set(left, top, left + tw, top + th);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(blend(UI.S2, UI.AMBER, t));
        c.drawRoundRect(rect, th / 2, th / 2, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2 * d);
        paint.setColor(blend(UI.LINE2, UI.AMBER, t));
        rect.inset(d, d);
        c.drawRoundRect(rect, th / 2, th / 2, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(blend(UI.T3, UI.ON_AMBER, t));
        float rad = (8 + 2 * t) * d;
        float cx = left + (14 + 24 * t) * d;
        c.drawCircle(cx, top + th / 2f, rad, paint);
    }

    private static int blend(int a, int b, float f) {
        int r = (int) (((a >> 16) & 255) * (1 - f) + ((b >> 16) & 255) * f);
        int g = (int) (((a >> 8) & 255) * (1 - f) + ((b >> 8) & 255) * f);
        int bl = (int) ((a & 255) * (1 - f) + (b & 255) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.Switch");
        info.setCheckable(true);
        info.setChecked(checked);
    }
}
