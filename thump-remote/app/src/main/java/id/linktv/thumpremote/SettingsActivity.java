package id.linktv.thumpremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/** Daftar semua tombol dengan kodenya; ketuk untuk mengubah, menguji, atau menghapus. */
public class SettingsActivity extends Activity {

    private IrSender ir;
    private CodeStore store;
    private KeyAdapter adapter;
    private EditText defaultAddress;
    private Spinner presetSpinner;
    private boolean suppressEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ir = new IrSender(this);
        store = new CodeStore(this);

        defaultAddress = findViewById(R.id.default_address);
        presetSpinner = findViewById(R.id.preset_spinner);
        ListView list = findViewById(R.id.list);
        adapter = new KeyAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, pos, id) -> edit(Keys.ALL[pos]));

        ArrayAdapter<String> presets = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{getString(R.string.preset_carmp3), getString(R.string.preset_empty)});
        presets.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presetSpinner.setAdapter(presets);
        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (suppressEvents) return;
                store.setPresetEnabled(pos == 0);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        defaultAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (suppressEvents) return;
                try {
                    store.setDefaultAddress(Code.parseHex(s.toString()));
                    adapter.notifyDataSetChanged();
                } catch (NumberFormatException ignored) {
                }
            }
        });

        findViewById(R.id.export).setOnClickListener(v -> exportCodes());
        findViewById(R.id.import_btn).setOnClickListener(v -> importCodes());
        findViewById(R.id.reset).setOnClickListener(v -> confirmReset());

        loadHeader();
    }

    private void loadHeader() {
        suppressEvents = true;
        defaultAddress.setText(Code.hex(store.defaultAddress(), store.defaultAddress() > 0xFF ? 4 : 2));
        presetSpinner.setSelection(store.presetEnabled() ? 0 : 1);
        suppressEvents = false;
    }

    // ---------------------------------------------------------------- aksi

    private void exportCodes() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("thump-remote-codes", store.exportText()));
        toast(getString(R.string.copied));
    }

    private void importCodes() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = cm.getPrimaryClip();
        String text = null;
        if (clip != null && clip.getItemCount() > 0) {
            CharSequence cs = clip.getItemAt(0).coerceToText(this);
            if (cs != null) text = cs.toString();
        }
        int n = store.importText(text);
        if (n == 0) {
            toast(getString(R.string.import_failed));
        } else {
            toast(getString(R.string.imported, n));
            loadHeader();
            adapter.notifyDataSetChanged();
        }
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.reset_confirm)
                .setPositiveButton(R.string.yes, (d, w) -> {
                    store.resetAll();
                    loadHeader();
                    adapter.notifyDataSetChanged();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void edit(final KeyDef k) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_edit, null);
        final EditText address = v.findViewById(R.id.address);
        final EditText command = v.findViewById(R.id.command);
        final EditText raw = v.findViewById(R.id.raw);
        Button delete = v.findViewById(R.id.delete);

        Code current = store.get(k);
        if (current.type == Code.NEC) {
            address.setText(Code.hex(current.address, current.address > 0xFF ? 4 : 2));
            command.setText(Code.hex(current.command, 2));
        } else {
            address.setText(Code.hex(store.defaultAddress(), store.defaultAddress() > 0xFF ? 4 : 2));
            if (current.type == Code.RAW) raw.setText(current.serialize().substring("raw:".length()).replaceFirst("^\\d+:", ""));
        }
        delete.setVisibility(store.isCustom(k) ? View.VISIBLE : View.GONE);

        final AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(k.name)
                .setView(v)
                .setPositiveButton(R.string.save, null)
                .setNeutralButton(R.string.test, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dlg.setOnShowListener(d -> {
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
                Code c = readCode(address, command, raw);
                if (c == null) return;
                store.set(k, c);
                adapter.notifyDataSetChanged();
                dlg.dismiss();
            });
            dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(b -> {
                Code c = readCode(address, command, raw);
                if (c == null) return;
                if (!ir.isAvailable()) {
                    toast(getString(R.string.ir_missing));
                    return;
                }
                try {
                    ir.send(c);
                    b.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                } catch (RuntimeException e) {
                    toast(getString(R.string.ir_send_failed) + ": " + e.getMessage());
                }
            });
        });
        delete.setOnClickListener(b -> {
            store.clear(k);
            adapter.notifyDataSetChanged();
            dlg.dismiss();
        });
        dlg.show();
    }

    /** Membaca isian dialog; RAW diutamakan jika diisi. null + toast jika tidak valid. */
    private Code readCode(EditText address, EditText command, EditText raw) {
        String rawText = raw.getText().toString().trim();
        if (!rawText.isEmpty()) {
            try {
                int[] p = Code.parsePattern(rawText);
                if (p != null) return Code.raw(Code.NEC_FREQ, p);
            } catch (NumberFormatException ignored) {
            }
            toast(getString(R.string.invalid_hex));
            return null;
        }
        try {
            int a = Code.parseHex(address.getText().toString());
            int c = Code.parseHex(command.getText().toString());
            if (c > 0xFF) throw new NumberFormatException();
            return Code.nec(a, c);
        } catch (NumberFormatException e) {
            toast(getString(R.string.invalid_hex));
            return null;
        }
    }

    private Toast lastToast;

    private void toast(String msg) {
        if (lastToast != null) lastToast.cancel();
        lastToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        lastToast.show();
    }

    @SuppressWarnings("deprecation")
    private int color(int res) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getColor(res) : getResources().getColor(res);
    }

    // ---------------------------------------------------------------- adapter

    private final class KeyAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return Keys.ALL.length;
        }

        @Override
        public Object getItem(int position) {
            return Keys.ALL[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView != null ? convertView
                    : LayoutInflater.from(SettingsActivity.this).inflate(R.layout.item_key, parent, false);
            KeyDef k = Keys.ALL[position];
            Code c = store.get(k);
            ((TextView) v.findViewById(R.id.name)).setText(k.name);
            TextView code = v.findViewById(R.id.code);
            code.setText(c.isSet() ? c.display() : getString(R.string.not_set));
            code.setTextColor(color(c.isSet() ? R.color.text : R.color.text_muted));
            v.findViewById(R.id.custom).setVisibility(store.isCustom(k) ? View.VISIBLE : View.INVISIBLE);
            return v;
        }
    }
}
