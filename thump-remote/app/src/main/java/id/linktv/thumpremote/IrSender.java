package id.linktv.thumpremote;

import android.content.Context;
import android.hardware.ConsumerIrManager;

/** Pembungkus tipis ConsumerIrManager (IR blaster bawaan HP). */
public final class IrSender {
    private final ConsumerIrManager mgr;

    public IrSender(Context ctx) {
        Object s = ctx.getSystemService(Context.CONSUMER_IR_SERVICE);
        mgr = s instanceof ConsumerIrManager ? (ConsumerIrManager) s : null;
    }

    public boolean isAvailable() {
        try {
            return mgr != null && mgr.hasIrEmitter();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Mengirim satu frame. Melempar exception jika HP menolak pola/frekuensi. */
    public boolean send(Code code) {
        if (code == null) return false;
        int[] p = code.toPattern();
        if (p == null || mgr == null) return false;
        mgr.transmit(code.frequency(), p);
        return true;
    }
}
