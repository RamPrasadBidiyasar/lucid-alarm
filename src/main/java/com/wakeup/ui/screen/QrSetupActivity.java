package com.wakeup.ui.screen;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.wakeup.data.MorningState;
import com.wakeup.ui.BaseActivity;
import com.wakeup.ui.UI;
import com.wakeup.ui.widget.IconView;
import java.util.EnumMap;
import java.util.Map;

/** Shows your personal wake-up QR code so you can print it or save it, then stick it up far from your bed. */
public class QrSetupActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = column();
        root.addView(appBar("Wake-up code", IconView.CLOSE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        }));

        LinearLayout col = column();
        col.setPadding(dp(24), dp(8), dp(24), dp(16));
        TextView h = UI.text(this, "Stick this where you must walk to.", 32, UI.T1, UI.LIGHT);
        h.setLineSpacing(0, 1.1f);
        col.addView(h);
        TextView d = UI.text(this, "Print it or show it on another screen. Your bathroom mirror is ideal. Scanning it is the only way to cancel the rest of the morning.", 15, UI.T2, UI.REG);
        d.setLineSpacing(0, 1.2f);
        d.setPadding(0, dp(12), 0, dp(20));
        col.addView(d);

        ImageView img = new ImageView(this);
        img.setBackground(UI.rr(this, 0xFFFFFFFF, 20));
        img.setPadding(dp(16), dp(16), dp(16), dp(16));
        img.setImageBitmap(makeQr(MorningState.qrToken(this)));
        img.setContentDescription("Your wake-up QR code");
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(260), dp(260));
        ip.gravity = Gravity.CENTER_HORIZONTAL;
        col.addView(img, ip);
        root.addView(col, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bottom = column();
        bottom.setPadding(dp(24), dp(12), dp(24), dp(12));
        TextView done = UI.primary(this, "It's stuck up", 56);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MorningState.setQrReady(QrSetupActivity.this);
                finish();
            }
        });
        bottom.addView(done, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(bottom);

        show(root, UI.BG, false);
    }

    private Bitmap makeQr(String text) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix m = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 600, 600, hints);
            int w = m.getWidth(), h = m.getHeight();
            int[] px = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) px[y * w + x] = m.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
            }
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bmp.setPixels(px, 0, w, 0, 0, w, h);
            return bmp;
        } catch (WriterException e) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }
    }
}
