package gay.nihil.lena.midikeypad;

public class KeypadData {
    boolean handled;
    boolean sendMidi;
    boolean on;
    int code;

    public KeypadData(boolean handled, boolean on, int code) {
        this(handled, handled, on, code);
    }

    private KeypadData(boolean handled, boolean sendMidi, boolean on, int code) {
        this.handled = handled;
        this.sendMidi = sendMidi;
        this.on = on;
        this.code = code;
    }

    public static KeypadData consumedOnly() {
        return new KeypadData(true, false, false, 0);
    }
}
