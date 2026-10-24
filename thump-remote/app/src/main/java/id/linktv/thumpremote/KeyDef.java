package id.linktv.thumpremote;

/** Definisi satu tombol remote. */
public final class KeyDef {
    public static final int STYLE_NORMAL = 0;
    public static final int STYLE_ACCENT = 1;
    public static final int STYLE_DANGER = 2;

    /** Id stabil yang dipakai untuk menyimpan kode (jangan diubah). */
    public final String id;
    /** Nama untuk daftar/dialog. */
    public final String name;
    /** Teks di tombol; null jika memakai ikon. */
    public final String text;
    /** Resource ikon; 0 jika memakai teks. */
    public final int icon;
    /** Perintah NEC bawaan dari preset, -1 jika tidak ada. */
    public final int defaultCmd;
    /** Tahan untuk mengulang (volume). */
    public final boolean repeat;
    public final int style;

    KeyDef(String id, String name, String text, int icon, int defaultCmd, boolean repeat, int style) {
        this.id = id;
        this.name = name;
        this.text = text;
        this.icon = icon;
        this.defaultCmd = defaultCmd;
        this.repeat = repeat;
        this.style = style;
    }

    static KeyDef text(String id, String name, int defaultCmd) {
        return new KeyDef(id, name, name, 0, defaultCmd, false, STYLE_NORMAL);
    }

    /** Tombol teks yang nama panjangnya berbeda dari tulisan singkat di tombol. */
    static KeyDef text(String id, String name, String text, int defaultCmd) {
        return new KeyDef(id, name, text, 0, defaultCmd, false, STYLE_NORMAL);
    }

    static KeyDef icon(String id, String name, int icon, int defaultCmd, boolean repeat, int style) {
        return new KeyDef(id, name, null, icon, defaultCmd, repeat, style);
    }

    @Override
    public String toString() {
        return name;
    }
}
