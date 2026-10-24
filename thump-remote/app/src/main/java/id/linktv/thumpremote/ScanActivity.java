package id.linktv.thumpremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Pencari kode: mengirim semua perintah NEC 00..FF pada sebuah alamat satu per satu.
 * Saat head unit bereaksi, pengguna menekan "ITU DIA!" dan beberapa kode terakhir
 * ditampilkan sebagai kandidat untuk diuji dan disimpan ke tombol.
 */
public class ScanActivity extends Activity {

    /** Alamat NEC yang paling sering dipakai remote head unit/MP5 generik. */
    private static final int[] COMMON_ADDRESSES = {
            0x00, 0x80, 0xFF, 0x40, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20,
            0x7F, 0xBF, 0xEF, 0xFE, 0x86, 0x38, 0x50, 0x60, 0x6B86,
    };
    private static final int CANDIDATE_COUNT = 4;
    private static final int MIN_INTERVAL = 100;      // ms
    private static final int NEC_FRAME_MS = 70;

    private IrSender ir;
    private CodeStore store;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private Spinner addressSpinner;
    private Spinner targetSpinner;
    private EditText addressCustom;
    private EditText startCmd;
    private TextView intervalLabel;
    private SeekBar intervalSeek;
    private Button startStop;
    private Button hit;
    private ProgressBar progress;
    private TextView progressText;
    private LinearLayout candidates;

    private volatile boolean running;
    private Thread worker;
    private final Deque<Code> history = new ArrayDeque<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        ir = new IrSender(this);
        store = new CodeStore(this);

        addressSpinner = findViewById(R.id.address_spinner);
        targetSpinner = findViewById(R.id.target_spinner);
        addressCustom = findViewById(R.id.address_custom);
        startCmd = findViewById(R.id.start_cmd);
        intervalLabel = findViewById(R.id.interval_label);
        intervalSeek = findViewById(R.id.interval_seek);
        startStop = findViewById(R.id.start_stop);
        hit = findViewById(R.id.hit);
        progress = findViewById(R.id.progress);
        progressText = findViewById(R.id.progress_text);
        candidates = findViewById(R.id.candidates);

        setupAddressSpinner();
        setupTargetSpinner();

        intervalSeek.setProgress(20); // 300 ms
        updateIntervalLabel();
        intervalSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
                updateIntervalLabel();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        startStop.setOnClickListener(v -> {
            if (running) stopScan(false);
            else startScan();
        });
        hit.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            stopScan(true);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
    }

    // ---------------------------------------------------------------- setup

    private void setupAddressSpinner() {
        List<String> items = new ArrayList<>();
        for (int a : COMMON_ADDRESSES) {
            String label = "0x" + Code.hex(a, a > 0xFF ? 4 : 2);
            if (a == 0x00) label += "  (paling umum)";
            if (a > 0xFF) label += "  (NEC extended)";
            items.add(label);
        }
        items.add("Kustom…");
        int fullMinutes = (int) Math.ceil(65536L * (300 + NEC_FRAME_MS) / 60000.0);
        items.add(getString(R.string.scan_all_addresses, fullMinutes));
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        addressSpinner.setAdapter(ad);
        addressSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                addressCustom.setVisibility(pos == COMMON_ADDRESSES.length ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupTargetSpinner() {
        List<String> items = new ArrayList<>();
        items.add(getString(R.string.scan_target_none));
        for (KeyDef k : Keys.ALL) items.add(k.name);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        targetSpinner.setAdapter(ad);
    }

    private int intervalMs() {
        return MIN_INTERVAL + intervalSeek.getProgress() * 10;
    }

    private void updateIntervalLabel() {
        intervalLabel.setText(getString(R.string.scan_interval, intervalMs()));
    }

    /** Daftar alamat yang akan dipindai sesuai pilihan, atau null jika input tidak valid. */
    private int[] selectedAddresses() {
        int pos = addressSpinner.getSelectedItemPosition();
        if (pos < COMMON_ADDRESSES.length) return new int[]{COMMON_ADDRESSES[pos]};
        if (pos == COMMON_ADDRESSES.length) {
            try {
                return new int[]{Code.parseHex(addressCustom.getText().toString())};
            } catch (NumberFormatException e) {
                return null;
            }
        }
        int[] all = new int[256];
        for (int i = 0; i < 256; i++) all[i] = i;
        return all;
    }

    private KeyDef selectedTarget() {
        int pos = targetSpinner.getSelectedItemPosition();
        return pos <= 0 ? null : Keys.ALL[pos - 1];
    }

    // ---------------------------------------------------------------- scan

    private void startScan() {
        if (!ir.isAvailable()) {
            toast(getString(R.string.ir_missing));
            return;
        }
        final int[] addresses = selectedAddresses();
        if (addresses == null) {
            toast(getString(R.string.invalid_hex));
            return;
        }
        int start;
        try {
            start = Code.parseHex(startCmd.getText().toString()) & 0xFF;
        } catch (NumberFormatException e) {
            start = 0;
        }
        final int startFrom = start;
        final int interval = intervalMs();

        synchronized (history) {
            history.clear();
        }
        candidates.removeAllViews();
        running = true;
        setScanningUi(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        worker = new Thread(() -> {
            final int total = addresses.length * 256 - startFrom;
            int done = 0;
            boolean first = true;
            outer:
            for (final int addr : addresses) {
                for (int c = first ? startFrom : 0; c < 256; c++) {
                    if (!running) break outer;
                    final Code code = Code.nec(addr, c);
                    final int n = ++done;
                    ui.post(() -> {
                        progress.setMax(total);
                        progress.setProgress(n);
                        progressText.setText(getString(R.string.scan_sending,
                                Code.hex(addr, addr > 0xFF ? 4 : 2), Code.hex(code.command, 2), n, total));
                    });
                    try {
                        ir.send(code);
                    } catch (RuntimeException e) {
                        final String msg = e.getMessage();
                        ui.post(() -> toast(getString(R.string.ir_send_failed) + ": " + msg));
                        running = false;
                        break outer;
                    }
                    synchronized (history) {
                        history.addFirst(code);
                        while (history.size() > CANDIDATE_COUNT) history.removeLast();
                    }
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        break outer;
                    }
                }
                first = false;
            }
            final boolean finished = running;
            running = false;
            ui.post(() -> {
                setScanningUi(false);
                if (finished) progressText.setText(R.string.scan_done);
            });
        }, "ir-scan");
        worker.start();
    }

    private void stopScan(boolean showCandidates) {
        running = false;
        if (worker != null) worker.interrupt();
        setScanningUi(false);
        if (showCandidates) showCandidates();
    }

    private void setScanningUi(boolean scanning) {
        startStop.setText(scanning ? R.string.scan_stop : R.string.scan_start);
        hit.setEnabled(scanning);
        addressSpinner.setEnabled(!scanning);
        addressCustom.setEnabled(!scanning);
        startCmd.setEnabled(!scanning);
        intervalSeek.setEnabled(!scanning);
        if (!scanning) getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // ---------------------------------------------------------------- kandidat

    private void showCandidates() {
        List<Code> list;
        synchronized (history) {
            list = new ArrayList<>(history);
        }
        candidates.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);
        for (final Code code : list) {
            View item = inf.inflate(R.layout.item_candidate, candidates, false);
            ((TextView) item.findViewById(R.id.code)).setText(code.display());
            item.findViewById(R.id.send).setOnClickListener(v -> {
                try {
                    ir.send(code);
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                } catch (RuntimeException e) {
                    toast(getString(R.string.ir_send_failed));
                }
            });
            item.findViewById(R.id.use).setOnClickListener(v -> assign(code));
            candidates.addView(item);
        }
        if (!list.isEmpty()) {
            // Sarankan kode berikutnya sebagai titik lanjut scan.
            Code newest = list.get(0);
            startCmd.setText(Code.hex(Math.min(newest.command + 1, 0xFF), 2));
        }
    }

    private void assign(final Code code) {
        KeyDef target = selectedTarget();
        if (target != null) {
            save(target, code);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_key)
                .setItems(Keys.names(), (d, which) -> save(Keys.ALL[which], code))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void save(KeyDef k, Code code) {
        store.set(k, code);
        toast(getString(R.string.saved_to, k.name));
    }

    private Toast lastToast;

    private void toast(String msg) {
        if (lastToast != null) lastToast.cancel();
        lastToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        lastToast.show();
    }
}
