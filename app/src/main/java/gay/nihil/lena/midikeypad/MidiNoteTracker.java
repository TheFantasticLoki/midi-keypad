package gay.nihil.lena.midikeypad;

import java.util.HashMap;
import java.util.Map;

final class MidiNoteTracker {
    private final Map<Integer, Integer> activeNotes = new HashMap<>();

    boolean shouldSend(boolean on, int note) {
        int count = activeNotes.containsKey(note) ? activeNotes.get(note) : 0;
        if (on) {
            activeNotes.put(note, count + 1);
            return count == 0;
        }
        if (count == 0) {
            return false;
        }
        if (count == 1) {
            activeNotes.remove(note);
            return true;
        }
        activeNotes.put(note, count - 1);
        return false;
    }

    void noteOnSendFailed(int note) {
        int count = activeNotes.containsKey(note) ? activeNotes.get(note) : 0;
        if (count <= 1) {
            activeNotes.remove(note);
        } else {
            activeNotes.put(note, count - 1);
        }
    }
}
