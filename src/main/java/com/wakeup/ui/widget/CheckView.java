package com.wakeup.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.LinearInterpolator;
import com.wakeup.ui.UI;

/** The completion mark: a ring draws over 700 ms, then the tick over 360 ms. Fully drawn if animations are off. */
public class CheckView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path tick = new Path();
    private final Path partial = new Path();
    private final RectF oval = new RectF();
    private float ms = 1010f; // timeline position, 0 to 1010

    public CheckView(Context c, int color) {
        super(c);
        float d = c.getResources().getDisplayMetrics().density;
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(3 * d);
        track.setColor(UI.LINE);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeCap(Paint.Cap.ROUND);
        ring.setStrokeJoin(Paint.Join.ROUND);
        ring.setColor(color);
    }

    public void play() {
        if (!UI.animationsOn()) {
            ms = 1010f;
            invalidate();
            return;
        }
        ms = 0f;
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1010f);
        a.setDuration(1010);
        a.setInterpolator(new LinearInterpolator());
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator v) {
                ms = (Float) v.getAnimatedValue();
                invalidate();
            }
        });
        a.start();
    }

    private static float ease(float x) {
        x = Math.max(0f, Math.min(1f, x));
        return 1f - (1f - x) * (1f - x) * (1f - x);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float d = getResources().getDisplayMetrics().density;
        float pad = 3 * d;
        oval.set(pad, pad, w - pad, h - pad);
        c.drawOval(oval, track);

        float ringP = ease((ms - 100f) / 700f);
        ring.setStrokeWidth(3 * d);
        c.drawArc(oval, -90, 360f * ringP, false, ring);

        float s = Math.min(w, h) / 120f;
        tick.reset();
        tick.moveTo(38 * s, 62 * s);
        tick.lineTo(54 * s, 78 * s);
        tick.lineTo(82 * s, 46 * s);
        float tickP = ease((ms - 650f) / 360f);
        if (tickP > 0) {
            PathMeasure pm = new PathMeasure(tick, false);
            partial.reset();
            pm.getSegment(0, pm.getLength() * tickP, partial, true);
            ring.setStrokeWidth(5 * d);
            c.drawPath(partial, ring);
        }
    }
}
