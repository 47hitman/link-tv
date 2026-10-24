package id.linktv.thumpremote;

import static id.linktv.thumpremote.KeyDef.STYLE_ACCENT;
import static id.linktv.thumpremote.KeyDef.STYLE_DANGER;
import static id.linktv.thumpremote.KeyDef.STYLE_NORMAL;

/**
 * Semua tombol remote beserta perintah NEC bawaan (preset "Generik MP5").
 *
 * Preset ini adalah kode remote 21 tombol yang umum dipakai head unit / MP3 mobil
 * murah (NEC, alamat 0x00). Tombol yang tidak ada di remote tersebut dibiarkan
 * tanpa kode (-1) dan bisa dicari lewat layar "Cari Kode".
 */
public final class Keys {
    private Keys() {
    }

    // Daya & sumber
    public static final KeyDef POWER = KeyDef.icon("POWER", "Power", R.drawable.ic_power, 0x45, false, STYLE_DANGER);
    public static final KeyDef MUTE = KeyDef.icon("MUTE", "Mute", R.drawable.ic_mute, 0x46, false, STYLE_NORMAL);
    public static final KeyDef SRC = KeyDef.text("SRC", "SRC / Mode", "SRC", 0x47);
    public static final KeyDef EJECT = KeyDef.icon("EJECT", "Eject", R.drawable.ic_eject, -1, false, STYLE_NORMAL);

    // Volume & trek
    public static final KeyDef VOL_DOWN = KeyDef.icon("VOL_DOWN", "Vol −", R.drawable.ic_vol_down, 0x07, true, STYLE_ACCENT);
    public static final KeyDef VOL_UP = KeyDef.icon("VOL_UP", "Vol +", R.drawable.ic_vol_up, 0x15, true, STYLE_ACCENT);
    public static final KeyDef PREV = KeyDef.icon("PREV", "Sebelumnya", R.drawable.ic_prev, 0x44, false, STYLE_NORMAL);
    public static final KeyDef PLAY_PAUSE = KeyDef.icon("PLAY_PAUSE", "Play / Pause", R.drawable.ic_play_pause, 0x43, false, STYLE_ACCENT);
    public static final KeyDef NEXT = KeyDef.icon("NEXT", "Berikutnya", R.drawable.ic_next, 0x40, false, STYLE_NORMAL);
    public static final KeyDef STOP = KeyDef.icon("STOP", "Stop", R.drawable.ic_stop, -1, false, STYLE_NORMAL);

    // Navigasi
    public static final KeyDef UP = KeyDef.icon("UP", "Atas", R.drawable.ic_up, -1, true, STYLE_NORMAL);
    public static final KeyDef DOWN = KeyDef.icon("DOWN", "Bawah", R.drawable.ic_down, -1, true, STYLE_NORMAL);
    public static final KeyDef LEFT = KeyDef.icon("LEFT", "Kiri", R.drawable.ic_left, -1, true, STYLE_NORMAL);
    public static final KeyDef RIGHT = KeyDef.icon("RIGHT", "Kanan", R.drawable.ic_right, -1, true, STYLE_NORMAL);
    public static final KeyDef OK = new KeyDef("OK", "OK / Enter", "OK", 0, -1, false, STYLE_ACCENT);
    public static final KeyDef MENU = KeyDef.icon("MENU", "Menu", R.drawable.ic_menu, -1, false, STYLE_NORMAL);
    public static final KeyDef SETUP = KeyDef.text("SETUP", "Setup", -1);
    public static final KeyDef BACK = KeyDef.icon("BACK", "Kembali", R.drawable.ic_back, -1, false, STYLE_NORMAL);
    public static final KeyDef DISP = KeyDef.text("DISP", "Disp / OSD", "DISP", -1);

    // Audio & mode
    public static final KeyDef BAND = KeyDef.text("BAND", "Band", -1);
    public static final KeyDef EQ = KeyDef.text("EQ", "EQ", 0x09);
    public static final KeyDef LOUD = KeyDef.text("LOUD", "Loud", -1);
    public static final KeyDef SEL = KeyDef.text("SEL", "Sel", -1);
    public static final KeyDef RPT = KeyDef.icon("RPT", "Repeat", R.drawable.ic_repeat, -1, false, STYLE_NORMAL);
    public static final KeyDef RDM = KeyDef.icon("RDM", "Random", R.drawable.ic_shuffle, -1, false, STYLE_NORMAL);
    public static final KeyDef AMS = KeyDef.text("AMS", "AMS / APS", "AMS", -1);
    public static final KeyDef GOTO = KeyDef.text("GOTO", "Goto", -1);

    // Video
    public static final KeyDef ZOOM = KeyDef.text("ZOOM", "Zoom", -1);
    public static final KeyDef AUDIO = KeyDef.text("AUDIO", "Audio", -1);
    public static final KeyDef SUBTITLE = KeyDef.text("SUBTITLE", "Sub-T", -1);
    public static final KeyDef ANGLE = KeyDef.text("ANGLE", "Angle", -1);
    public static final KeyDef TITLE = KeyDef.text("TITLE", "Title", -1);
    public static final KeyDef PBC = KeyDef.text("PBC", "PBC", -1);
    public static final KeyDef SLOW = KeyDef.text("SLOW", "Slow", -1);
    public static final KeyDef CLOCK = KeyDef.text("CLOCK", "Clock", -1);

    // Angka
    public static final KeyDef NUM_1 = KeyDef.text("NUM_1", "1", 0x0C);
    public static final KeyDef NUM_2 = KeyDef.text("NUM_2", "2", 0x18);
    public static final KeyDef NUM_3 = KeyDef.text("NUM_3", "3", 0x5E);
    public static final KeyDef NUM_4 = KeyDef.text("NUM_4", "4", 0x08);
    public static final KeyDef NUM_5 = KeyDef.text("NUM_5", "5", 0x1C);
    public static final KeyDef NUM_6 = KeyDef.text("NUM_6", "6", 0x5A);
    public static final KeyDef NUM_7 = KeyDef.text("NUM_7", "7", 0x42);
    public static final KeyDef NUM_8 = KeyDef.text("NUM_8", "8", 0x52);
    public static final KeyDef NUM_9 = KeyDef.text("NUM_9", "9", 0x4A);
    public static final KeyDef NUM_0 = KeyDef.text("NUM_0", "0", 0x16);
    public static final KeyDef NUM_10PLUS = KeyDef.text("NUM_10PLUS", "10+", 0x19);
    public static final KeyDef NUM_100PLUS = KeyDef.text("NUM_100PLUS", "100+", 0x0D);

    public static final KeyDef[] ALL = {
            POWER, MUTE, SRC, EJECT,
            VOL_DOWN, VOL_UP, PREV, PLAY_PAUSE, NEXT, STOP,
            UP, DOWN, LEFT, RIGHT, OK, MENU, SETUP, BACK, DISP,
            BAND, EQ, LOUD, SEL, RPT, RDM, AMS, GOTO,
            ZOOM, AUDIO, SUBTITLE, ANGLE, TITLE, PBC, SLOW, CLOCK,
            NUM_1, NUM_2, NUM_3, NUM_4, NUM_5, NUM_6, NUM_7, NUM_8, NUM_9, NUM_0, NUM_10PLUS, NUM_100PLUS,
    };

    public static KeyDef byId(String id) {
        if (id == null) return null;
        for (KeyDef k : ALL) {
            if (k.id.equalsIgnoreCase(id)) return k;
        }
        return null;
    }

    public static String[] names() {
        String[] n = new String[ALL.length];
        for (int i = 0; i < ALL.length; i++) n[i] = ALL[i].name;
        return n;
    }
}
