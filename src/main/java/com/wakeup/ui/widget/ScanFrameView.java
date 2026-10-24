package com.wakeup.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import com.wakeup.ui.UI;

/** Amber corner brackets with a sweeping line, drawn over the camera preview. */
public class ScanFrameView extends View {
    private final Paint corner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private ValueAnimator anim;
    private float sweep = 0.5f;

    public ScanFrameView(Context c) {
        super(c);
        float d = c.getResources().getDisplayMetrics().density;
        corner.setStyle(Paint.Style.STROKE);
        corner.setStrokeCap(Paint.Cap.ROUND);
        corner.setStrokeWidth(4 * d);
        corner.setColor(UI.AMBER);
        line.setColor(UI.AMBER);
        line.setStrokeWidth(3 * d);
        line.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!UI.animationsOn()) return;
        anim = ValueAnimator.ofFloat(0.08f, 0.92f);
        anim.setDuration(2000);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                sweep = (Float) a.getAnimatedValue();
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
        float d = getResources().getDisplayMetrics().density;
        float size = Math.min(getWidth(), getHeight()) * 0.66f;
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        box.set(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2);
        float len = 40 * d;
        // four corners
        c.drawLine(box.left, box.top + len, box.left, box.top, corner);
        c.drawLine(box.left, box.top, box.left + len, box.top, corner);
        c.drawLine(box.right - len, box.top, box.right, box.top, corner);
        c.drawLine(box.right, box.top, box.right, box.top + len, corner);
        c.drawLine(box.left, box.bottom - len, box.left, box.bottom, corner);
        c.drawLine(box.left, box.bottom, box.left + len, box.bottom, corner);
        c.drawLine(box.right - len, box.bottom, box.right, box.bottom, corner);
        c.drawLine(box.right, box.bottom, box.right, box.bottom - len, corner);
        float y = box.top + box.height() * sweep;
        c.drawLine(box.left + 10 * d, y, box.right - 10 * d, y, line);
    }
}
