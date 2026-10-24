package com.wakeup.ui.screen;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.wakeup.alarm.Awake;
import com.wakeup.alarm.RingService;
import com.wakeup.alarm.Scheduler;
import com.wakeup.data.MorningState;
import com.wakeup.data.PlanStore;
import com.wakeup.model.Plan;
import com.wakeup.ui.BaseActivity;
import com.wakeup.ui.Paste;
import com.wakeup.ui.UI;
import com.wakeup.ui.widget.IconView;
import com.wakeup.ui.widget.ScanFrameView;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Final wake-up confirmation: scan the QR code you stuck up far from your bed, or type the phrase. */
public class ScanActivity extends BaseActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final MultiFormatReader reader = new MultiFormatReader();

    private Camera camera;
    private SurfaceHolder holder;
    private FrameLayout panel;
    private TextView message;
    private IconView torchIcon;
    private boolean torch;
    private volatile boolean busy;
    private volatile boolean done;
    private long lastDecode;
    private int pw, ph;
    private boolean phraseMode;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        reader.setHints(hints);

        Plan plan = PlanStore.get(this, RingService.planId);
        phraseMode = plan != null && Plan.CONFIRM_PHRASE.equals(plan.confirm);

        LinearLayout root = column();

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), 0, dp(24), 0);
        FrameLayout back = new FrameLayout(this);
        back.addView(new IconView(this, IconView.CHEV_L, UI.T1), new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        back.setClickable(true);
        back.setContentDescription("Back");
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        bar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        bar.addView(UI.caption(this, "FINAL STEP", UI.T2));
        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        LinearLayout head = column();
        head.setPadding(dp(24), dp(12), dp(24), 0);
        LinearLayout segs = new LinearLayout(this);
        View s1 = new View(this);
        s1.setBackground(UI.rr(this, UI.OK, 2));
        View s2 = new View(this);
        s2.setBackground(UI.rr(this, UI.AMBER, 2));
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, dp(4), 1f);
        a.rightMargin = dp(6);
        segs.addView(s1, a);
        segs.addView(s2, new LinearLayout.LayoutParams(0, dp(4), 1f));
        head.addView(segs);
        TextView step = UI.text(this, MorningState.stopped(this) > 0 ? "Ring stopped · now confirm you're awake" : "Confirm you're awake", 13, UI.T2, UI.REG);
        step.setPadding(0, dp(12), 0, 0);
        head.addView(step);
        TextView h1 = UI.text(this, phraseMode ? "Type your wake-up phrase" : "Scan your wake-up code", 34, UI.T1, UI.LIGHT);
        h1.setLineSpacing(0, 1.05f);
        h1.setPadding(0, dp(16), 0, 0);
        head.addView(h1);
        TextView sub = UI.text(this, phraseMode ? "Type it slowly. Paste is blocked." : "It's in your bathroom. Walk there, then scan.", 16, UI.T2, UI.REG);
        sub.setPadding(0, dp(8), 0, 0);
        head.addView(sub);
        root.addView(head);

        panel = new FrameLayout(this);
        panel.setBackgroundColor(0xFF050506);
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(340));
        pl.setMargins(dp(24), dp(24), dp(24), 0);
        root.addView(panel, pl);

        root.addView(UI.spacer(this, 1f));

        LinearLayout info = column();
        info.setPadding(dp(24), dp(16), dp(24), 0);
        info.addView(UI.hairline(this));
        int remaining = Scheduler.remainingToday(this);
        TextView t = UI.text(this, "This cancels all " + remaining + " remaining rings today", 16, UI.T1, UI.BOLD);
        t.setPadding(0, dp(16), 0, 0);
        info.addView(t);
        TextView d = UI.text(this, "Tomorrow's routine arms automatically.", 14, UI.T2, UI.REG);
        d.setPadding(0, dp(4), 0, 0);
        info.addView(d);
        message = UI.text(this, "", 14, UI.ERR, UI.MED);
        message.setPadding(0, dp(8), 0, 0);
        info.addView(message);
        root.addView(info);

        LinearLayout foot = new LinearLayout(this);
        foot.setGravity(Gravity.CENTER_VERTICAL);
        foot.setPadding(dp(24), dp(4), dp(24), dp(12));
        TextView alt = UI.text(this, phraseMode ? "Use the QR code instead" : "Can't scan? Type the phrase", 15, UI.T2, UI.MED);
        alt.setMinHeight(dp(48));
        alt.setGravity(Gravity.CENTER_VERTICAL);
        alt.setPaintFlags(alt.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        alt.setClickable(true);
        alt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                phraseMode = !phraseMode;
                recreate();
            }
        });
        foot.addView(alt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (!phraseMode) {
            FrameLayout flash = new FrameLayout(this);
            flash.setBackground(UI.outline(this, 0, UI.LINE2, 1f, 24));
            torchIcon = new IconView(this, IconView.TORCH, UI.T1);
            flash.addView(torchIcon, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
            flash.setClickable(true);
            flash.setContentDescription("Toggle flashlight");
            flash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setTorch(!torch);
                }
            });
            foot.addView(flash, new LinearLayout.LayoutParams(dp(48), dp(48)));
        }
        root.addView(foot);

        show(root, UI.BG, false);

        if (phraseMode) buildPhrasePanel();
        else buildCameraPanel();
    }

    // ---- phrase fallback ------------------------------------------------------------------

    private void buildPhrasePanel() {
        panel.setBackgroundColor(0);
        LinearLayout c = column();
        c.setGravity(Gravity.CENTER);
        TextView p = UI.text(this, MorningState.PHRASE, 26, UI.AMBER, UI.LIGHT);
        p.setGravity(Gravity.CENTER);
        p.setLineSpacing(0, 1.15f);
        c.addView(p, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final EditText in = new EditText(this);
        in.setTextColor(UI.T1);
        in.setTextSize(20);
        in.setGravity(Gravity.CENTER);
        in.setSingleLine(true);
        in.setHint("Type it here");
        in.setHintTextColor(UI.T3);
        in.setBackground(UI.outline(this, 0, UI.LINE2, 2f, 20));
        in.setPadding(dp(16), dp(16), dp(16), dp(16));
        Paste.block(in);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(68));
        lp.topMargin = dp(24);
        c.addView(in, lp);
        in.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int d) {}

            public void onTextChanged(CharSequence s, int a, int b, int d) {}

            public void afterTextChanged(Editable e) {
                String t = e.toString();
                boolean prefix = MorningState.PHRASE.toLowerCase().startsWith(t.toLowerCase());
                in.setBackground(UI.outline(ScanActivity.this, 0, prefix ? UI.LINE2 : UI.ERR, 2f, 20));
                if (t.trim().equalsIgnoreCase(MorningState.PHRASE)) success();
            }
        });
        panel.addView(c, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    // ---- camera ---------------------------------------------------------------------------

    private void buildCameraPanel() {
        SurfaceView sv = new SurfaceView(this);
        sv.getHolder().addCallback(this);
        panel.addView(sv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        panel.addView(new ScanFrameView(this), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 11);
        }
        if (!MorningState.qrReady(this)) {
            message.setTextColor(UI.T2);
            message.setText("No code yet? Create it in Settings, or type the phrase.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            message.setTextColor(UI.ERR);
            message.setText("Camera access is off. Type the phrase instead.");
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder h) {
        holder = h;
        startCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder h) {
        holder = null;
        stopCamera();
    }

    private void startCamera() {
        if (camera != null || holder == null || phraseMode) return;
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;
        try {
            camera = Camera.open();
            Camera.Parameters p = camera.getParameters();
            Camera.Size best = null;
            List<Camera.Size> sizes = p.getSupportedPreviewSizes();
            for (Camera.Size s : sizes) {
                if (s.width <= 1280 && (best == null || s.width * s.height > best.width * best.height)) best = s;
            }
            if (best != null) {
                p.setPreviewSize(best.width, best.height);
                pw = best.width;
                ph = best.height;
            } else {
                pw = p.getPreviewSize().width;
                ph = p.getPreviewSize().height;
            }
            if (p.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            camera.setParameters(p);
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(holder);
            camera.setPreviewCallback(this);
            camera.startPreview();
        } catch (Exception e) {
            stopCamera();
            message.setTextColor(UI.ERR);
            message.setText("The camera could not start. Type the phrase instead.");
        }
    }

    private void stopCamera() {
        if (camera != null) {
            try {
                camera.setPreviewCallback(null);
                camera.stopPreview();
            } catch (Exception ignored) {
            }
            camera.release();
            camera = null;
        }
    }

    private void setTorch(boolean on) {
        if (camera == null) return;
        try {
            Camera.Parameters p = camera.getParameters();
            p.setFlashMode(on ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(p);
            torch = on;
            torchIcon.set(IconView.TORCH, on ? UI.AMBER : UI.T1);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onPreviewFrame(final byte[] data, Camera cam) {
        long now = System.currentTimeMillis();
        if (busy || done || now - lastDecode < 250) return;
        lastDecode = now;
        busy = true;
        final byte[] copy = data.clone();
        final int w = pw, h = ph;
        exec.execute(new Runnable() {
            @Override
            public void run() {
                String text = null;
                try {
                    PlanarYUVLuminanceSource src = new PlanarYUVLuminanceSource(copy, w, h, 0, 0, w, h, false);
                    Result r = reader.decodeWithState(new BinaryBitmap(new HybridBinarizer(src)));
                    text = r.getText();
                } catch (Exception ignored) {
                    // nothing readable in this frame
                } finally {
                    reader.reset();
                }
                final String found = text;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        busy = false;
                        if (found == null || done) return;
                        if (found.equals(MorningState.qrToken(ScanActivity.this))) {
                            success();
                        } else {
                            UI.haptic(message, UI.H_REJECT);
                            message.setTextColor(UI.ERR);
                            message.setText("That is not your wake-up code.");
                        }
                    }
                });
            }
        });
    }

    private void success() {
        if (done) return;
        done = true;
        UI.haptic(message, UI.H_CONFIRM);
        stopCamera();
        Awake.confirm(this, "qr");
        startActivity(new Intent(this, ClearedActivity.class));
        finish();
    }

    @Override
    protected void onPause() {
        stopCamera();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();
    }

    @Override
    protected void onDestroy() {
        exec.shutdownNow();
        super.onDestroy();
    }
}
