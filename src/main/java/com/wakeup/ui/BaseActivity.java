package com.wakeup.ui;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.wakeup.ui.widget.IconView;
import com.wakeup.ui.widget.LogoView;

/** Edge-to-edge screen host: draws under the system bars and pads the content by the insets. */
public abstract class BaseActivity extends Activity {

    /** Configures an activity to reliably wake up and show over the lock screen on modern Android. */
    protected void setupLockscreen() {
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * @param content   root of the screen. Its padding is set to the system bar insets.
     * @param bg        screen background colour
     * @param darkIcons true for a light background (dark status and navigation icons)
     */
    protected void show(final View content, int bg, boolean darkIcons) {
        FrameLayout host = new FrameLayout(this);
        host.setBackgroundColor(bg);
        host.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        host.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                content.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
                return insets;
            }
        });
        setContentView(host);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        if (darkIcons) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        host.setSystemUiVisibility(flags);
    }

    protected LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    protected int dp(float v) {
        return UI.dp(this, v);
    }

    /** Adds the small brand mark at the end of an app bar. Tap it for a little pulse. */
    protected LinearLayout withLogo(LinearLayout bar) {
        final LogoView logo = new LogoView(this);
        logo.setContentDescription("WakeUp logo");
        logo.setClickable(true);
        logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logo.pulse();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(22), dp(22));
        lp.leftMargin = dp(12);
        bar.addView(logo, lp);
        return bar;
    }

    /** App bar with a back or close icon and a title. */
    protected LinearLayout appBar(String title, int iconType, View.OnClickListener onIcon) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), 0, dp(24), 0);
        FrameLayout btn = new FrameLayout(this);
        IconView icon = new IconView(this, iconType, UI.T1);
        btn.addView(icon, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        btn.setClickable(true);
        btn.setContentDescription(iconType == IconView.CLOSE ? "Close" : "Back");
        btn.setOnClickListener(onIcon);
        bar.addView(btn, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView t = UI.text(this, title, 20, UI.T1, UI.REG);
        bar.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        return bar;
    }
}
