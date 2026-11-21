package gay.nihil.lena.midikeypad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class MidiNoteTracker {
    private static final class NoteState {
        int activePointers;
        boolean sounding;
    }

    private final Map<Integer, NoteState> notes = new HashMap<>();

    boolean noteOn(int note) {
        NoteState state = notes.get(note);
        if (state == null) {
            state = new NoteState();
            notes.put(note, state);
        }
        boolean wasInactive = state.activePointers == 0;
        state.activePointers++;
        return wasInactive || !state.sounding;
    }

    boolean noteOff(int note) {
        NoteState state = notes.get(note);
        if (state == null || state.activePointers == 0) {
            return false;
        }

        state.activePointers--;
        if (state.activePointers != 0) {
            return false;
        }
        if (state.sounding) {
            return true;
        }

        notes.remove(note);
        return false;
    }

    void noteOnSent(int note) {
        NoteState state = notes.get(note);
        if (state != null) {
            state.sounding = true;
        }
    }

    void noteOffSent(int note) {
        NoteState state = notes.get(note);
        if (state != null) {
            state.sounding = false;
            if (state.activePointers == 0) {
                notes.remove(note);
            }
        }
    }

    List<Integer> pendingNoteOffs() {
        List<Integer> pending = new ArrayList<>();
        for (Map.Entry<Integer, NoteState> entry : notes.entrySet()) {
            NoteState state = entry.getValue();
            if (state.activePointers == 0 && state.sounding) {
                pending.add(entry.getKey());
            }
        }
        return pending;
    }
}
