package com.wakeup.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.LinearInterpolator;
import com.wakeup.ui.UI;

/** Three rings that expand and fade from the middle while an alarm rings. Still rings if animations are off. */
public class PulseView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator anim;
    private float phase;

    public PulseView(Context c, int color) {
        super(c);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(UI.dp(c, 2));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!UI.animationsOn()) return;
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(2400);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                phase = (Float) a.getAnimatedValue();
                invalidate();
            }
        });
        anim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (anim != null) anim.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas c) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float base = Math.min(getWidth(), getHeight()) / 2f * 0.8f;
        for (int i = 0; i < 3; i++) {
            float p;
            if (anim == null) {
                p = (i + 1) / 4f;
                paint.setAlpha((int) (255 * 0.18f * (1 - p)));
            } else {
                p = (phase + i / 3f) % 1f;
                float eased = 1f - (1f - p) * (1f - p);
                paint.setAlpha((int) (255 * 0.5f * (1f - eased)));
                p = eased;
            }
            float radius = base * (0.45f + 1.65f * p);
            c.drawCircle(cx, cy, radius, paint);
        }
    }
}
