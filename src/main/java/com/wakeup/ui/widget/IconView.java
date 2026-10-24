package com.wakeup.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Outline icons on a 24 unit grid, 1.75 stroke, round caps. Drawn in code so no assets are needed. */
public class IconView extends View {
    public static final int CHEV_R = 0, CHEV_L = 1, CHEV_D = 2, CLOSE = 3, PLUS = 4, CHECK = 5,
            SLIDERS = 6, QR = 7, CALC = 8, KEYBOARD = 9, SHAKE = 10, SPEAKER = 11, SPEAKER_MUTE = 12,
            CLOCK = 13, TORCH = 14, WARN = 15, EDIT = 16;

    private int type;
    private int color;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF r = new RectF();
    private float stroke = 1.75f;

    public IconView(Context c, int type, int color) {
        super(c);
        this.type = type;
        this.color = color;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    public void set(int type, int color) {
        this.type = type;
        this.color = color;
        invalidate();
    }

    public IconView stroke(float s) {
        stroke = s;
        return this;
    }

    private void l(Canvas c, float s, float x1, float y1, float x2, float y2) {
        c.drawLine(x1 * s, y1 * s, x2 * s, y2 * s, p);
    }

    private void poly(Canvas c, float s, float... pts) {
        path.reset();
        path.moveTo(pts[0] * s, pts[1] * s);
        for (int i = 2; i < pts.length; i += 2) path.lineTo(pts[i] * s, pts[i + 1] * s);
        c.drawPath(path, p);
    }

    private void box(Canvas c, float s, float l, float t, float rt, float b, float rad) {
        r.set(l * s, t * s, rt * s, b * s);
        c.drawRoundRect(r, rad * s, rad * s, p);
    }

    @Override
    protected void onDraw(Canvas c) {
        float s = Math.min(getWidth(), getHeight()) / 24f;
        p.setColor(color);
        p.setStrokeWidth(stroke * s);
        switch (type) {
            case CHEV_R: poly(c, s, 9, 6, 15, 12, 9, 18); break;
            case CHEV_L: poly(c, s, 15, 6, 9, 12, 15, 18); break;
            case CHEV_D: poly(c, s, 6, 9, 12, 15, 18, 9); break;
            case CLOSE: l(c, s, 6, 6, 18, 18); l(c, s, 18, 6, 6, 18); break;
            case PLUS: l(c, s, 12, 5, 12, 19); l(c, s, 5, 12, 19, 12); break;
            case CHECK: poly(c, s, 5, 12, 10, 17, 20, 7); break;
            case SLIDERS:
                l(c, s, 4, 7, 13, 7); l(c, s, 17, 7, 20, 7); c.drawCircle(15 * s, 7 * s, 2 * s, p);
                l(c, s, 4, 17, 7, 17); l(c, s, 11, 17, 20, 17); c.drawCircle(9 * s, 17 * s, 2 * s, p);
                break;
            case QR:
                box(c, s, 4, 4, 10, 10, 0.5f); box(c, s, 14, 4, 20, 10, 0.5f); box(c, s, 4, 14, 10, 20, 0.5f);
                poly(c, s, 14, 14, 17, 14, 17, 17); l(c, s, 21, 14, 21, 14.01f); l(c, s, 14, 21, 14, 21.01f);
                poly(c, s, 17.5f, 21, 21, 21, 21, 17);
                break;
            case CALC:
                box(c, s, 5, 3, 19, 21, 2); l(c, s, 8, 7, 16, 7);
                l(c, s, 8, 12, 10, 12); l(c, s, 14, 12, 16, 12); l(c, s, 8, 16, 10, 16); l(c, s, 14, 16, 16, 16);
                break;
            case KEYBOARD:
                box(c, s, 3, 6, 21, 18, 2);
                l(c, s, 7, 10, 7, 10.01f); l(c, s, 11, 10, 11, 10.01f); l(c, s, 15, 10, 15, 10.01f);
                l(c, s, 7, 14, 17, 14);
                break;
            case SHAKE:
                box(c, s, 9, 4, 15, 20, 1.5f);
                l(c, s, 4, 9, 4, 15); l(c, s, 20, 9, 20, 15); l(c, s, 2, 11, 2, 13); l(c, s, 22, 11, 22, 13);
                break;
            case SPEAKER:
            case SPEAKER_MUTE:
                path.reset();
                path.moveTo(4 * s, 10 * s); path.lineTo(4 * s, 14 * s); path.lineTo(8 * s, 14 * s);
                path.lineTo(13 * s, 18 * s); path.lineTo(13 * s, 6 * s); path.lineTo(8 * s, 10 * s); path.close();
                c.drawPath(path, p);
                if (type == SPEAKER) {
                    r.set(9 * s, 8 * s, 17 * s, 16 * s);
                    c.drawArc(r, -50, 100, false, p);
                    r.set(7 * s, 5 * s, 21 * s, 19 * s);
                    c.drawArc(r, -50, 100, false, p);
                } else {
                    l(c, s, 16, 9, 21, 15); l(c, s, 21, 9, 16, 15);
                }
                break;
            case CLOCK:
                c.drawCircle(12 * s, 12 * s, 9 * s, p);
                poly(c, s, 12, 7, 12, 12, 15, 14);
                break;
            case TORCH:
                poly(c, s, 9, 3, 15, 3, 14, 9, 10, 9);
                box(c, s, 10, 9, 14, 21, 0.5f);
                break;
            case WARN:
                poly(c, s, 12, 3, 22, 20, 2, 20, 12, 3);
                l(c, s, 12, 10, 12, 15); l(c, s, 12, 17.5f, 12, 17.51f);
                break;
            case EDIT:
                poly(c, s, 4, 20, 5, 15, 16, 4, 20, 8, 9, 19, 4, 20);
                l(c, s, 14, 6, 18, 10);
                break;
            default:
                break;
        }
    }
}
