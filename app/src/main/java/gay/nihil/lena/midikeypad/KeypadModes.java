package gay.nihil.lena.midikeypad;

final class KeypadModes {
    static final int MASTER = 0;
    static final int LEGACY_OSU_STATIC = 1;
    static final int LEGACY_OSU_DYNAMIC = 2;
    static final int LEGACY_MANIA = 3;
    static final int LEGACY_DEADZONE = 4;
    static final int LEGACY_ADAPTIVE_SPLIT = 5;
    static final int LEGACY_ADAPTIVE_TARGETS = 6;

    private KeypadModes() {
    }

    static int fromPreviousModeOrder(int previousMode) {
        if (previousMode == LEGACY_ADAPTIVE_TARGETS) {
            return MASTER;
        }
        if (previousMode >= 0 && previousMode < LEGACY_ADAPTIVE_TARGETS) {
            return previousMode + 1;
        }
        return MASTER;
    }
}
