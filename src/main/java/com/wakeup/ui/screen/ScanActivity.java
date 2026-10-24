package com.wakeup.ui.screen;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import com.wakeup.ui.Segmented;
import com.wakeup.ui.UI;
import com.wakeup.ui.widget.IconView;
import com.wakeup.ui.widget.ScanFrameView;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Final wake-up confirmation:
 * Offers Phrase, 5-Second Hold, Code/Barcode Scan, or NFC Tap without trapping the user.
 */
public class ScanActivity extends BaseActivity implements SurfaceHolder.Callback, Camera.PreviewCallback, SensorEventListener {
    public static final int MODE_PHRASE = 0;
    public static final int MODE_HOLD = 1;
    public static final int MODE_SCAN = 2;

    private static final String AFFIRMATION = "I am awake";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final MultiFormatReader reader = new MultiFormatReader();

    private Camera camera;
    private SurfaceHolder holder;
    private FrameLayout panel;
    private TextView h1, sub, message;
    private FrameLayout flashBtn;
    private IconView torchIcon;
    private boolean torch;
    private boolean manualTorch;
    private SensorManager sensors;
    private Sensor lightSensor;
    private NfcAdapter nfc;
    private volatile boolean busy;
    private volatile boolean done;
    private long lastDecode;
    private int pw, ph;
    private int activeMode = MODE_PHRASE;
    private int holdSeconds = 5;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Support QR and standard retail packaging barcodes (toothpaste, shampoo, soap)
        Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
        List<BarcodeFormat> formats = new ArrayList<BarcodeFormat>();
        formats.add(BarcodeFormat.QR_CODE);
        formats.add(BarcodeFormat.EAN_13);
        formats.add(BarcodeFormat.EAN_8);
        formats.add(BarcodeFormat.UPC_A);
        formats.add(BarcodeFormat.CODE_128);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        reader.setHints(hints);

        String pid = getIntent().getStringExtra("planId");
        if (pid == null) pid = RingService.planId;
        Plan plan = pid != null ? PlanStore.get(this, pid) : null;

        if (plan != null) {
            if (Plan.CONFIRM_HOLD.equals(plan.confirm)) activeMode = MODE_HOLD;
            else if (Plan.CONFIRM_QR.equals(plan.confirm) && MorningState.qrReady(this)) activeMode = MODE_SCAN;
            else activeMode = MODE_PHRASE;
        } else {
            activeMode = MODE_PHRASE;
        }

        nfc = NfcAdapter.getDefaultAdapter(this);
        sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensors != null) {
            lightSensor = sensors.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        LinearLayout root = column();

        // App Bar
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
        bar.addView(UI.caption(this, "CONFIRM AWAKE", UI.T2));
        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        // Header
        LinearLayout head = column();
        head.setPadding(dp(24), dp(8), dp(24), 0);
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

        TextView step = UI.text(this, "Ring stopped · now confirm you're awake", 13, UI.T2, UI.REG);
        step.setPadding(0, dp(12), 0, 0);
        head.addView(step);

        h1 = UI.text(this, "", 32, UI.T1, UI.LIGHT);
        h1.setLineSpacing(0, 1.05f);
        h1.setPadding(0, dp(12), 0, 0);
        head.addView(h1);

        sub = UI.text(this, "", 15, UI.T2, UI.REG);
        sub.setPadding(0, dp(6), 0, 0);
        head.addView(sub);

        // Mode switch tabs
        Segmented tabs = new Segmented(this, new String[]{"Phrase", "Hold 5s", "Scan"}, activeMode, new Segmented.OnSelect() {
            @Override
            public void onSelect(int index) {
                switchMode(index);
            }
        });
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = dp(16);
        head.addView(tabs, tp);
        root.addView(head);

        panel = new FrameLayout(this);
        panel.setBackgroundColor(0xFF050506);
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(310));
        pl.setMargins(dp(24), dp(16), dp(24), 0);
        root.addView(panel, pl);

        root.addView(UI.spacer(this, 1f));

        LinearLayout info = column();
        info.setPadding(dp(24), dp(12), dp(24), 0);
        info.addView(UI.hairline(this));
        int remaining = Scheduler.remainingToday(this);
        TextView t = UI.text(this, "This cancels all " + remaining + " remaining rings today", 16, UI.T1, UI.BOLD);
        t.setPadding(0, dp(12), 0, 0);
        info.addView(t);
        TextView d = UI.text(this, "Tomorrow's routine arms automatically.", 14, UI.T2, UI.REG);
        d.setPadding(0, dp(4), 0, 0);
        info.addView(d);
        message = UI.text(this, "", 14, UI.ERR, UI.MED);
        message.setPadding(0, dp(4), 0, 0);
        info.addView(message);
        root.addView(info);

        LinearLayout foot = new LinearLayout(this);
        foot.setGravity(Gravity.CENTER_VERTICAL);
        foot.setPadding(dp(24), dp(4), dp(24), dp(12));
        TextView nfcHint = UI.text(this, "NFC tag tap is always accepted.", 13, UI.T3, UI.REG);
        foot.addView(nfcHint, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        flashBtn = new FrameLayout(this);
        flashBtn.setBackground(UI.outline(this, 0, UI.LINE2, 1f, 24));
        torchIcon = new IconView(this, IconView.TORCH, UI.T1);
        flashBtn.addView(torchIcon, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        flashBtn.setClickable(true);
        flashBtn.setContentDescription("Toggle flashlight");
        flashBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manualTorch = true;
                setTorch(!torch);
            }
        });
        foot.addView(flashBtn, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(foot);

        show(root, UI.BG, false);
        switchMode(activeMode);
    }

    private void switchMode(int mode) {
        activeMode = mode;
        message.setText("");
        panel.removeAllViews();
        flashBtn.setVisibility(mode == MODE_SCAN ? View.VISIBLE : View.GONE);

        if (mode == MODE_SCAN) {
            h1.setText("Scan your code");
            sub.setText("Scan QR or any bathroom item (toothpaste, shampoo).");
            buildCameraPanel();
            startCamera();
        } else {
            stopCamera();
            if (mode == MODE_HOLD) {
                h1.setText("Hold 5 seconds");
                sub.setText("Touch and hold the button below to prove you are up.");
                buildHoldPanel();
            } else {
                h1.setText("Type wake-up phrase");
                sub.setText("Type 'I am awake' below to confirm you are out of bed.");
                buildPhrasePanel();
            }
        }
    }

    // ---- 1. Phrase panel -------------------------------------------------------------------

    private void buildPhrasePanel() {
        panel.setBackgroundColor(0);
        LinearLayout c = column();
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(12), 0, dp(12), 0);

        TextView p = UI.text(this, "\"" + AFFIRMATION + "\"", 28, UI.AMBER, UI.LIGHT);
        p.setGravity(Gravity.CENTER);
        c.addView(p, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final EditText in = new EditText(this);
        in.setTextColor(UI.T1);
        in.setTextSize(20);
        in.setGravity(Gravity.CENTER);
        in.setSingleLine(true);
        in.setImeOptions(EditorInfo.IME_ACTION_DONE);
        in.setHint("Type: " + AFFIRMATION);
        in.setHintTextColor(UI.T3);
        in.setBackground(UI.outline(this, 0, UI.LINE2, 2f, 20));
        in.setPadding(dp(16), dp(14), dp(16), dp(14));
        Paste.block(in);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64));
        lp.topMargin = dp(20);
        c.addView(in, lp);

        final TextView confirmBtn = UI.primary(this, "I'm awake", 52);
        confirmBtn.setAlpha(0.4f);
        confirmBtn.setClickable(false);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.topMargin = dp(16);
        c.addView(confirmBtn, bp);

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                success();
            }
        });

        in.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
            public void onTextChanged(CharSequence s, int a, int b, int d) {}
            public void afterTextChanged(Editable e) {
                String t = e.toString().trim();
                boolean matches = isPhraseMatch(t);
                confirmBtn.setAlpha(matches ? 1f : 0.4f);
                confirmBtn.setClickable(matches);
                in.setBackground(UI.outline(ScanActivity.this, 0, matches ? UI.OK : UI.LINE2, 2f, 20));
                if (matches) {
                    UI.haptic(confirmBtn, UI.H_TICK);
                }
            }
        });

        in.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    if (isPhraseMatch(in.getText().toString().trim())) {
                        success();
                        return true;
                    }
                }
                return false;
            }
        });

        panel.addView(c, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Auto-focus soft keyboard
        in.requestFocus();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(in, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 150);
    }

    private static boolean isPhraseMatch(String t) {
        if (t == null) return false;
        String clean = t.replaceAll("[^a-zA-Z0-9 ]", "").toLowerCase().trim();
        return clean.equals("i am awake")
                || clean.equals("i am awake and out of bed")
                || clean.equals("i am awake and i am out of bed")
                || clean.equals(MorningState.PHRASE.replaceAll("[^a-zA-Z0-9 ]", "").toLowerCase().trim());
    }

    // ---- 2. 5-Second Hold panel ------------------------------------------------------------

    private void buildHoldPanel() {
        panel.setBackgroundColor(0);
        LinearLayout c = column();
        c.setGravity(Gravity.CENTER);

        final FrameLayout btn = new FrameLayout(this);
        btn.setBackground(UI.outline(this, UI.S1, UI.AMBER, 2.5f, 100));
        final TextView btnText = UI.text(this, "Hold 5s", 20, UI.T1, UI.BOLD);
        btn.addView(btnText, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(110), dp(110));
        c.addView(btn, bp);

        final TextView hint = UI.text(this, "Press and hold firmly", 14, UI.T2, UI.REG);
        hint.setPadding(0, dp(20), 0, 0);
        c.addView(hint);

        final Runnable holdCountdown = new Runnable() {
            @Override
            public void run() {
                holdSeconds--;
                UI.haptic(btn, UI.H_TICK);
                if (holdSeconds <= 0) {
                    btnText.setText("Done");
                    btn.setBackground(UI.rr(ScanActivity.this, UI.OK, 100));
                    success();
                } else {
                    btnText.setText(holdSeconds + "s");
                    handler.postDelayed(this, 1000);
                }
            }
        };

        btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    holdSeconds = 5;
                    btnText.setText("5s");
                    btn.setBackground(UI.outline(ScanActivity.this, UI.S2, UI.AMBER, 3.5f, 100));
                    hint.setText("Keep holding…");
                    UI.haptic(btn, UI.H_TICK);
                    handler.postDelayed(holdCountdown, 1000);
                    return true;
                }
                if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                    handler.removeCallbacks(holdCountdown);
                    btnText.setText("Hold 5s");
                    btn.setBackground(UI.outline(ScanActivity.this, UI.S1, UI.AMBER, 2.5f, 100));
                    hint.setText("Press and hold firmly");
                    return true;
                }
                return true;
            }
        });

        panel.addView(c, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    // ---- 3. Camera / Barcode / QR panel -----------------------------------------------------

    private void buildCameraPanel() {
        panel.setBackgroundColor(0xFF050506);
        SurfaceView sv = new SurfaceView(this);
        sv.getHolder().addCallback(this);
        panel.addView(sv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        panel.addView(new ScanFrameView(this), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 11);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            message.setTextColor(UI.ERR);
            message.setText("Camera permission denied. Switch to Phrase or Hold above.");
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder h) {
        holder = h;
        if (activeMode == MODE_SCAN) startCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder h) {
        holder = null;
        stopCamera();
    }

    private void startCamera() {
        if (camera != null || holder == null || activeMode != MODE_SCAN) return;
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
            message.setText("Camera could not open. Switch to Phrase or Hold above.");
        }
    }

    private void stopCamera() {
        if (camera != null) {
            try {
                camera.setPreviewCallback(null);
                camera.stopPreview();
            } catch (Exception ignored) {}
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
        } catch (Exception ignored) {}
    }

    @Override
    public void onPreviewFrame(final byte[] data, Camera cam) {
        if (!manualTorch && !torch && pw > 0 && ph > 0 && activeMode == MODE_SCAN) {
            long sum = 0;
            int step = Math.max(1, (pw * ph) / 64);
            int samples = 0;
            for (int i = 0; i < pw * ph; i += step) {
                sum += (data[i] & 0xFF);
                samples++;
            }
            if (samples > 0 && (sum / samples) < 35) {
                setTorch(true);
            }
        }
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
                } finally {
                    reader.reset();
                }
                final String found = text;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        busy = false;
                        if (found == null || done) return;

                        // If user has paired a specific QR token, verify it.
                        // If no specific token was created, ANY barcode in bathroom/kitchen is valid!
                        if (MorningState.qrReady(ScanActivity.this)) {
                            if (found.equals(MorningState.qrToken(ScanActivity.this))) {
                                success();
                            } else {
                                UI.haptic(message, UI.H_REJECT);
                                message.setTextColor(UI.ERR);
                                message.setText("That is not your wake-up code.");
                            }
                        } else {
                            // Any scanned barcode/code confirms awake
                            success();
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
        String how = activeMode == MODE_SCAN ? "scan" : activeMode == MODE_HOLD ? "hold" : "phrase";
        Awake.confirm(this, how);
        startActivity(new Intent(this, ClearedActivity.class));
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            String act = intent.getAction();
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(act)
                    || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(act)
                    || NfcAdapter.ACTION_TECH_DISCOVERED.equals(act)) {
                success();
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (e.sensor.getType() == Sensor.TYPE_LIGHT && !manualTorch && !torch && activeMode == MODE_SCAN) {
            if (e.values[0] < 10) {
                setTorch(true);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onPause() {
        if (sensors != null) sensors.unregisterListener(this);
        if (nfc != null) {
            try { nfc.disableForegroundDispatch(this); } catch (Exception ignored) {}
        }
        stopCamera();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lightSensor != null && sensors != null) {
            sensors.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (nfc != null && nfc.isEnabled()) {
            try {
                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (android.os.Build.VERSION.SDK_INT >= 31) flags |= 0x02000000;
                PendingIntent pi = PendingIntent.getActivity(this, 0,
                        new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                        flags);
                nfc.enableForegroundDispatch(this, pi, null, null);
            } catch (Exception ignored) {}
        }
        if (activeMode == MODE_SCAN) startCamera();
    }

    @Override
    protected void onDestroy() {
        exec.shutdownNow();
        super.onDestroy();
    }
}
