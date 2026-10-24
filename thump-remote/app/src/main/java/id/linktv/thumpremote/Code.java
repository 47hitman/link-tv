package id.linktv.thumpremote;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Satu kode IR untuk sebuah tombol: NEC (alamat + perintah), pola RAW, atau belum diatur.
 *
 * NEC standar: alamat 8-bit dikirim lalu kebalikannya. Jika alamat > 0xFF dianggap
 * NEC extended: byte rendah dikirim lebih dulu, lalu byte tinggi (tanpa inversi).
 */
public final class Code {
    public static final int NONE = 0;
    public static final int NEC = 1;
    public static final int RAW = 2;

    public static final int NEC_FREQ = 38000;

    public final int type;
    public final int address;   // 0..0xFFFF
    public final int command;   // 0..0xFF
    public final int freq;      // Hz (RAW)
    public final int[] pattern; // mikrodetik, nyala/mati bergantian (RAW)

    private Code(int type, int address, int command, int freq, int[] pattern) {
        this.type = type;
        this.address = address;
        this.command = command;
        this.freq = freq;
        this.pattern = pattern;
    }

    public static Code none() {
        return new Code(NONE, 0, 0, 0, null);
    }

    public static Code nec(int address, int command) {
        return new Code(NEC, address & 0xFFFF, command & 0xFF, NEC_FREQ, null);
    }

    public static Code raw(int freq, int[] pattern) {
        return new Code(RAW, 0, 0, freq, pattern);
    }

    public boolean isSet() {
        return type != NONE;
    }

    public int frequency() {
        return type == RAW ? freq : NEC_FREQ;
    }

    /** Pola nyala/mati dalam mikrodetik, siap dikirim ke ConsumerIrManager. */
    public int[] toPattern() {
        switch (type) {
            case NEC:
                return necPattern(address, command);
            case RAW:
                return pattern;
            default:
                return null;
        }
    }

    public static int[] necPattern(int address, int command) {
        int lo = address & 0xFF;
        int hi = address > 0xFF ? (address >> 8) & 0xFF : (~address) & 0xFF;
        int[] p = new int[2 + 32 * 2 + 1];
        int i = 0;
        p[i++] = 9000;
        p[i++] = 4500;
        i = putByte(p, i, lo);
        i = putByte(p, i, hi);
        i = putByte(p, i, command & 0xFF);
        i = putByte(p, i, (~command) & 0xFF);
        p[i] = 562;
        return p;
    }

    private static int putByte(int[] p, int i, int b) {
        for (int bit = 0; bit < 8; bit++) {
            p[i++] = 562;
            p[i++] = ((b >> bit) & 1) == 1 ? 1687 : 562;
        }
        return i;
    }

    public String serialize() {
        switch (type) {
            case NEC:
                return String.format(Locale.US, "nec:%04X:%02X", address, command);
            case RAW: {
                StringBuilder sb = new StringBuilder("raw:").append(freq).append(':');
                for (int k = 0; k < pattern.length; k++) {
                    if (k > 0) sb.append(',');
                    sb.append(pattern[k]);
                }
                return sb.toString();
            }
            default:
                return "none";
        }
    }

    /** Bentuk yang ditampilkan di daftar, mis. "NEC 00 · 45" atau "RAW 38 kHz (67)". */
    public String display() {
        switch (type) {
            case NEC:
                return "NEC " + hex(address, address > 0xFF ? 4 : 2) + " · " + hex(command, 2);
            case RAW:
                return "RAW " + (freq / 1000) + " kHz (" + pattern.length + ")";
            default:
                return "";
        }
    }

    /** Parse bentuk tersimpan ("nec:AAAA:CC", "raw:F:a,b,c", "none"). null jika tidak dikenali. */
    public static Code parse(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.equalsIgnoreCase("none")) return none();
        String[] parts = s.split(":", 3);
        try {
            if (parts.length == 3 && parts[0].equalsIgnoreCase("nec")) {
                return nec(parseHex(parts[1]), parseHex(parts[2]));
            }
            if (parts.length == 3 && parts[0].equalsIgnoreCase("raw")) {
                int f = Integer.parseInt(parts[1].trim());
                int[] p = parsePattern(parts[2]);
                if (p == null) return null;
                return raw(f, p);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    /**
     * Parse longgar untuk impor: menerima bentuk tersimpan, "AAAA:CC", atau "CC" saja
     * (memakai alamat bawaan), serta "raw:" dengan/ tanpa frekuensi.
     */
    public static Code parseLenient(String s, int defaultAddress) {
        Code c = parse(s);
        if (c != null) return c;
        if (s == null) return null;
        s = s.trim();
        try {
            if (s.toLowerCase(Locale.US).startsWith("raw:")) {
                int[] p = parsePattern(s.substring(4));
                return p == null ? null : raw(NEC_FREQ, p);
            }
            String[] parts = s.split(":");
            if (parts.length == 2) return nec(parseHex(parts[0]), parseHex(parts[1]));
            if (parts.length == 1) return nec(defaultAddress, parseHex(parts[0]));
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    public static int[] parsePattern(String s) {
        if (s == null) return null;
        String[] toks = s.trim().split("[,\\s]+");
        List<Integer> vals = new ArrayList<>();
        for (String t : toks) {
            if (t.isEmpty()) continue;
            int v = Integer.parseInt(t);
            if (v <= 0) return null;
            vals.add(v);
        }
        if (vals.size() < 2) return null;
        int[] p = new int[vals.size()];
        for (int k = 0; k < p.length; k++) p[k] = vals.get(k);
        return p;
    }

    public static String hex(int v, int digits) {
        return String.format(Locale.US, "%0" + digits + "X", v);
    }

    /** Menerima "45", "0x45", "0X45", dengan spasi di sekitar. */
    public static int parseHex(String s) throws NumberFormatException {
        if (s == null) throw new NumberFormatException("null");
        s = s.trim().replace(" ", "");
        if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
        if (s.isEmpty() || s.length() > 4) throw new NumberFormatException(s);
        return Integer.parseInt(s, 16);
    }
}
