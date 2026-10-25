package com.wakeup.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import com.wakeup.ui.UI;

/**
 * The WakeUp brand mark: three concentric rings (an amber core dot and two harmonic rings).
 * Supports a signature opening bloom animation upon app launch and interactive tap pulse.
 */
public class LogoView extends View {
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private ValueAnimator anim;
    private float progress = 1.0f;
    private boolean isOpening;
    private boolean isPulse;
    private boolean hapticFired;

    public LogoView(Context c) {
        super(c);
        init();
    }

    private void init() {
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(UI.AMBER);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(UI.AMBER);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);

        ripplePaint.setStyle(Paint.Style.STROKE);
        ripplePaint.setColor(UI.AMBER);
        ripplePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    /** Plays the signature opening bloom animation over 850 ms. */
    public void playOpening(final Runnable onEnd) {
        if (!UI.animationsOn()) {
            progress = 1.0f;
            isOpening = false;
            invalidate();
            if (onEnd != null) onEnd.run();
            return;
        }
        if (anim != null) anim.cancel();
        isOpening = true;
        isPulse = false;
        hapticFired = false;
        progress = 0f;
        invalidate();

        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(850);
        anim.setInterpolator(new DecelerateInterpolator(1.8f));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator va) {
                progress = (Float) va.getAnimatedValue();
                if (!hapticFired && progress >= 0.65f) {
                    hapticFired = true;
                    UI.haptic(LogoView.this, UI.H_TICK);
                }
                invalidate();
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isOpening = false;
                progress = 1.0f;
                invalidate();
                if (onEnd != null) onEnd.run();
            }
        });
        anim.start();
    }

    /** Interactive touch pulse when tapped. */
    public void pulse() {
        if (!UI.animationsOn()) return;
        if (anim != null && anim.isRunning()) return;
        UI.haptic(this, UI.H_TICK);
        isOpening = false;
        isPulse = true;
        progress = 0f;

        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(420);
        anim.setInterpolator(new DecelerateInterpolator(1.6f));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator va) {
                progress = (Float) va.getAnimatedValue();
                invalidate();
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isPulse = false;
                progress = 1.0f;
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
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cx = w / 2f;
        float cy = h / 2f;
        float sw = Math.max(UI.dp(getContext(), 1.5f), Math.min(w, h) * 0.045f);
        strokePaint.setStrokeWidth(sw);

        float maxR = Math.min(w, h) / 2f - sw * 1.5f;
        float rCore = maxR * 0.28f;
        float rMid = maxR * 0.62f;
        float rOuter = maxR * 0.96f;

        if (isOpening) {
            // Phase 1: Core dot scales and pops in
            float pCore = Math.min(1f, progress / 0.35f);
            float coreScale = pCore < 0.7f ? (pCore / 0.7f) * 1.15f : 1.15f - ((pCore - 0.7f) / 0.3f) * 0.15f;
            fillPaint.setAlpha((int) (255 * Math.min(1f, pCore * 1.4f)));
            c.drawCircle(cx, cy, rCore * coreScale, fillPaint);

            // Phase 2: Middle ring blooms out from core
            if (progress > 0.15f) {
                float pMid = Math.min(1f, (progress - 0.15f) / 0.50f);
                float midRadius = rCore + (rMid - rCore) * pMid;
                strokePaint.setAlpha((int) (255 * pMid));
                c.drawCircle(cx, cy, midRadius, strokePaint);
            }

            // Phase 3: Outer ring ripples out from middle ring
            if (progress > 0.30f) {
                float pOuter = Math.min(1f, (progress - 0.30f) / 0.55f);
                float outerRadius = rMid + (rOuter - rMid) * pOuter;
                strokePaint.setAlpha((int) (128 * pOuter));
                c.drawCircle(cx, cy, outerRadius, strokePaint);
            }

            // Phase 4: Acoustic sonar echo ripple
            if (progress > 0.60f) {
                float pRipple = (progress - 0.60f) / 0.40f;
                float rippleRadius = rOuter + (rOuter * 0.32f) * pRipple;
                ripplePaint.setStrokeWidth(sw * (1f - 0.4f * pRipple));
                ripplePaint.setAlpha((int) (150 * (1f - pRipple)));
                c.drawCircle(cx, cy, rippleRadius, ripplePaint);
            }
        } else if (isPulse) {
            // Interactive touch pulse
            float scale = 1f + 0.08f * (float) Math.sin(progress * Math.PI);
            fillPaint.setAlpha(255);
            c.drawCircle(cx, cy, rCore * scale, fillPaint);

            strokePaint.setAlpha(255);
            c.drawCircle(cx, cy, rMid * scale, strokePaint);

            strokePaint.setAlpha(128);
            c.drawCircle(cx, cy, rOuter * scale, strokePaint);

            // Expanding halo ripple
            float rippleR = rOuter + (rOuter * 0.28f) * progress;
            ripplePaint.setStrokeWidth(sw * (1f - 0.5f * progress));
            ripplePaint.setAlpha((int) (160 * (1f - progress)));
            c.drawCircle(cx, cy, rippleR, ripplePaint);
        } else {
            // Resting state: crisp signature 3 rings
            fillPaint.setAlpha(255);
            c.drawCircle(cx, cy, rCore, fillPaint);

            strokePaint.setAlpha(255);
            c.drawCircle(cx, cy, rMid, strokePaint);

            strokePaint.setAlpha(128);
            c.drawCircle(cx, cy, rOuter, strokePaint);
        }
    }
}
