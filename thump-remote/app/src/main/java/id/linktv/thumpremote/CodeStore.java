package id.linktv.thumpremote;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

/** Penyimpanan kode per tombol (SharedPreferences) + ekspor/impor teks. */
public final class CodeStore {
    private static final String PREFS = "codes";
    private static final String K_ADDRESS = "default_address";
    private static final String K_PRESET = "preset_enabled";
    private static final String K_PREFIX = "key_";

    private final SharedPreferences sp;

    public CodeStore(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public int defaultAddress() {
        return sp.getInt(K_ADDRESS, 0x00);
    }

    public void setDefaultAddress(int address) {
        sp.edit().putInt(K_ADDRESS, address & 0xFFFF).apply();
    }

    /** true = tombol tanpa kode tersimpan memakai perintah bawaan preset. */
    public boolean presetEnabled() {
        return sp.getBoolean(K_PRESET, true);
    }

    public void setPresetEnabled(boolean enabled) {
        sp.edit().putBoolean(K_PRESET, enabled).apply();
    }

    public boolean isCustom(KeyDef k) {
        return sp.contains(K_PREFIX + k.id);
    }

    public Code get(KeyDef k) {
        String s = sp.getString(K_PREFIX + k.id, null);
        if (s != null) {
            Code c = Code.parse(s);
            if (c != null) return c;
        }
        if (presetEnabled() && k.defaultCmd >= 0) return Code.nec(defaultAddress(), k.defaultCmd);
        return Code.none();
    }

    public void set(KeyDef k, Code c) {
        sp.edit().putString(K_PREFIX + k.id, c.serialize()).apply();
    }

    public void clear(KeyDef k) {
        sp.edit().remove(K_PREFIX + k.id).apply();
    }

    public void resetAll() {
        sp.edit().clear().apply();
    }

    public String exportText() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Thump Remote - kode IR (NEC alamat:perintah, hex)\n");
        sb.append("address=").append(Code.hex(defaultAddress(), 2)).append('\n');
        sb.append("preset=").append(presetEnabled() ? 1 : 0).append('\n');
        for (KeyDef k : Keys.ALL) {
            Code c = get(k);
            if (!c.isSet()) continue;
            sb.append(k.id).append('=').append(c.serialize()).append('\n');
        }
        return sb.toString();
    }

    /** Mengimpor baris "KEY=nilai". Mengembalikan jumlah tombol yang berhasil dibaca. */
    public int importText(String text) {
        if (text == null) return 0;
        int count = 0;
        SharedPreferences.Editor ed = sp.edit();
        int addr = defaultAddress();
        for (String rawLine : text.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String key = line.substring(0, eq).trim();
            String val = line.substring(eq + 1).trim();
            if (key.equalsIgnoreCase("address")) {
                try {
                    addr = Code.parseHex(val);
                    ed.putInt(K_ADDRESS, addr);
                } catch (NumberFormatException ignored) {
                }
                continue;
            }
            if (key.equalsIgnoreCase("preset")) {
                ed.putBoolean(K_PRESET, !(val.equals("0") || val.equalsIgnoreCase("false")));
                continue;
            }
            KeyDef k = Keys.byId(key.toUpperCase(Locale.US));
            if (k == null) continue;
            Code c = Code.parseLenient(val, addr);
            if (c == null) continue;
            ed.putString(K_PREFIX + k.id, c.serialize());
            count++;
        }
        ed.apply();
        return count;
    }
}
