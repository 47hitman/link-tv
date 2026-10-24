package id.linktv.thumpremote;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Layar remote utama. Tata letak dibangun lewat kode supaya mudah diubah. */
public class MainActivity extends Activity {

    private static final long REPEAT_INTERVAL_MS = 140;

    private IrSender ir;
    private CodeStore store;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<View> keyViews = new ArrayList<>();
    private TextView statusView;
    private Runnable repeatTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ir = new IrSender(this);
        store = new CodeStore(this);
        setContentView(buildUi());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshKeys();
        refreshStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRepeat();
    }

    // ---------------------------------------------------------------- UI

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(20), dp(16), dp(24));
        scroll.addView(col, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        col.addView(buildHeader());

        statusView = new TextView(this);
        statusView.setTextSize(13);
        statusView.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams slp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(12);
        col.addView(statusView, slp);

        section(col, "Daya & sumber");
        row(col, 60, Keys.POWER, Keys.MUTE, Keys.SRC, Keys.EJECT);

        section(col, "Volume & trek");
        row(col, 68, Keys.VOL_DOWN, Keys.VOL_UP);
        row(col, 60, Keys.PREV, Keys.PLAY_PAUSE, Keys.NEXT, Keys.STOP);

        section(col, "Navigasi");
        row(col, 58, Keys.MENU, Keys.UP, Keys.SETUP);
        row(col, 58, Keys.LEFT, Keys.OK, Keys.RIGHT);
        row(col, 58, Keys.BACK, Keys.DOWN, Keys.DISP);

        section(col, "Audio & mode");
        row(col, 54, Keys.BAND, Keys.EQ, Keys.LOUD, Keys.SEL);
        row(col, 54, Keys.RPT, Keys.RDM, Keys.AMS, Keys.GOTO);

        section(col, "Video");
        row(col, 54, Keys.ZOOM, Keys.AUDIO, Keys.SUBTITLE, Keys.ANGLE);
        row(col, 54, Keys.TITLE, Keys.PBC, Keys.SLOW, Keys.CLOCK);

        section(col, "Angka");
        row(col, 54, Keys.NUM_1, Keys.NUM_2, Keys.NUM_3);
        row(col, 54, Keys.NUM_4, Keys.NUM_5, Keys.NUM_6);
        row(col, 54, Keys.NUM_7, Keys.NUM_8, Keys.NUM_9);
        row(col, 54, Keys.NUM_10PLUS, Keys.NUM_0, Keys.NUM_100PLUS);

        TextView hint = new TextView(this);
        hint.setText(R.string.hint_default_codes);
        hint.setTextSize(12);
        hint.setTextColor(color(R.color.text_muted));
        hint.setLineSpacing(0, 1.15f);
        LinearLayout.LayoutParams hlp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = dp(20);
        col.addView(hint, hlp);

        return scroll;
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(color(R.color.text));
        TextView sub = new TextView(this);
        sub.setText(R.string.device_name);
        sub.setTextSize(13);
        sub.setTextColor(color(R.color.accent));
        titles.addView(title);
        titles.addView(sub);
        LinearLayout.LayoutParams tlp = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.weight = 1;
        header.addView(titles, tlp);

        header.addView(navButton(R.string.btn_scan, ScanActivity.class), lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)));
        LinearLayout.LayoutParams blp = lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
        blp.leftMargin = dp(8);
        header.addView(navButton(R.string.btn_settings, SettingsActivity.class), blp);
        return header;
    }

    private Button navButton(int textRes, final Class<?> target) {
        Button b = new Button(this);
        b.setText(textRes);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTextColor(color(R.color.text));
        b.setBackgroundResource(R.drawable.btn_key);
        b.setStateListAnimator(null);
        b.setPadding(dp(12), 0, dp(12), 0);
        b.setMinHeight(0);
        b.setMinimumHeight(dp(40));
        b.setOnClickListener(v -> startActivity(new Intent(this, target)));
        return b;
    }

    private void section(LinearLayout col, String label) {
        TextView t = new TextView(this);
        t.setText(label.toUpperCase());
        t.setTextSize(11);
        t.setLetterSpacing(0.1f);
        t.setTextColor(color(R.color.text_muted));
        LinearLayout.LayoutParams p = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(18);
        p.bottomMargin = dp(4);
        col.addView(t, p);
    }

    private void row(LinearLayout col, int heightDp, KeyDef... keys) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        for (KeyDef k : keys) {
            View v = makeKey(k);
            LinearLayout.LayoutParams p = lp(0, dp(heightDp));
            p.weight = 1;
            p.setMargins(dp(3), dp(3), dp(3), dp(3));
            r.addView(v, p);
        }
        col.addView(r, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private View makeKey(final KeyDef k) {
        View v;
        if (k.icon != 0) {
            ImageButton ib = new ImageButton(this);
            ib.setImageResource(k.icon);
            ib.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            ib.setColorFilter(k.style == KeyDef.STYLE_NORMAL ? color(R.color.text) : Color.WHITE);
            ib.setPadding(dp(14), dp(10), dp(14), dp(10));
            v = ib;
        } else {
            Button b = new Button(this);
            b.setText(k.text);
            b.setAllCaps(false);
            b.setSingleLine(true);
            b.setTextSize(k.text.length() > 4 ? 13 : 16);
            b.setTypeface(Typeface.DEFAULT_BOLD);
            b.setTextColor(k.style == KeyDef.STYLE_NORMAL ? color(R.color.text) : Color.WHITE);
            b.setPadding(dp(4), 0, dp(4), 0);
            b.setMinHeight(0);
            b.setMinWidth(0);
            b.setMinimumWidth(0);
            v = b;
        }
        int bg = k.style == KeyDef.STYLE_DANGER ? R.drawable.btn_danger
                : k.style == KeyDef.STYLE_ACCENT ? R.drawable.btn_accent
                : R.drawable.btn_key;
        v.setBackgroundResource(bg);
        v.setStateListAnimator(null);
        v.setContentDescription(k.name);
        v.setTag(k);

        v.setOnClickListener(view -> send(k, view));
        if (k.repeat) {
            v.setOnLongClickListener(view -> {
                startRepeat(k, view);
                return true;
            });
            v.setOnTouchListener((view, ev) -> {
                int a = ev.getActionMasked();
                if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) stopRepeat();
                return false;
            });
        }
        keyViews.add(v);
        return v;
    }

    private void refreshKeys() {
        for (View v : keyViews) {
            KeyDef k = (KeyDef) v.getTag();
            v.setAlpha(store.get(k).isSet() ? 1f : 0.38f);
        }
    }

    private void refreshStatus() {
        if (ir.isAvailable()) {
            statusView.setText(R.string.ir_ready);
            statusView.setTextColor(color(R.color.accent));
            statusView.setBackgroundResource(R.drawable.bg_banner_ok);
        } else {
            statusView.setText(R.string.ir_missing);
            statusView.setTextColor(color(R.color.text));
            statusView.setBackgroundResource(R.drawable.bg_banner_warn);
        }
    }

    // ---------------------------------------------------------------- IR

    private boolean send(KeyDef k, View source) {
        Code c = store.get(k);
        if (!c.isSet()) {
            toast(getString(R.string.code_not_set, k.name));
            return false;
        }
        if (!ir.isAvailable()) {
            toast(getString(R.string.ir_missing));
            return false;
        }
        try {
            ir.send(c);
            if (source != null) source.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            return true;
        } catch (RuntimeException e) {
            toast(getString(R.string.ir_send_failed) + ": " + e.getMessage());
            return false;
        }
    }

    private void startRepeat(final KeyDef k, final View source) {
        stopRepeat();
        if (!send(k, source)) return;
        repeatTask = new Runnable() {
            @Override
            public void run() {
                if (repeatTask != this) return;
                if (send(k, null)) handler.postDelayed(this, REPEAT_INTERVAL_MS);
            }
        };
        handler.postDelayed(repeatTask, REPEAT_INTERVAL_MS);
    }

    private void stopRepeat() {
        if (repeatTask != null) {
            handler.removeCallbacks(repeatTask);
            repeatTask = null;
        }
    }

    // ---------------------------------------------------------------- util

    private Toast lastToast;

    private void toast(String msg) {
        if (lastToast != null) lastToast.cancel();
        lastToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        lastToast.show();
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @SuppressWarnings("deprecation")
    private int color(int res) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getColor(res) : getResources().getColor(res);
    }

    private static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }
}
