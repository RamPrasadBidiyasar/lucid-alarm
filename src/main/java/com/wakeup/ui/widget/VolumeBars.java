package com.wakeup.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Ten bars that fill as the siren gets louder. */
public class VolumeBars extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private float level = 0.08f;

    public VolumeBars(Context c, int color) {
        super(c);
        paint.setColor(color);
    }

    public void setLevel(float l) {
        if (Math.abs(l - level) < 0.005f) return;
        level = l;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float d = getResources().getDisplayMetrics().density;
        float barW = 6 * d, gap = 5 * d;
        int n = 10;
        float total = n * barW + (n - 1) * gap;
        float x = (getWidth() - total) / 2f;
        float h = getHeight();
        float lit = level * n;
        for (int i = 0; i < n; i++) {
            float bh = h * (0.22f + 0.78f * (i + 1) / n);
            paint.setAlpha(i + 1 <= lit + 0.5f ? 255 : 56);
            r.set(x, h - bh, x + barW, h);
            c.drawRoundRect(r, barW / 2, barW / 2, paint);
            x += barW + gap;
        }
    }
}
