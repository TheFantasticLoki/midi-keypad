package gay.nihil.lena.midikeypad;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MultiKeyMappingTest {
    @Test
    public void staticLanesMapEachKeyToItsConfiguredMidiNote() {
        MultiKeyMapping mapping = new MultiKeyMapping(new int[]{36, 40, 43, 48},
                0, 8, 8, MultiKeyMapping.STATIC_LANES);

        assertEquals(Integer.valueOf(36), mapping.noteForTap(10, 400));
        assertEquals(Integer.valueOf(40), mapping.noteForTap(110, 400));
        assertEquals(Integer.valueOf(48), mapping.noteForTap(399, 400));
    }

    @Test
    public void staticLaneDeadzoneIgnoresTouchesAtLaneBoundaries() {
        MultiKeyMapping mapping = new MultiKeyMapping(new int[]{60, 61, 62, 63},
                10, 8, 8, MultiKeyMapping.STATIC_LANES);

        assertNull(mapping.noteForTap(100, 400));
        assertEquals(Integer.valueOf(61), mapping.noteForTap(120, 400));
    }

    @Test
    public void adaptiveCentersLearnPerKeyAndStayOrdered() {
        MultiKeyMapping mapping = new MultiKeyMapping(new int[]{10, 20, 30, 40},
                0, 1, 45, MultiKeyMapping.ADAPTIVE_LANES);

        assertEquals(Integer.valueOf(20), mapping.noteForTap(160, 400));
        assertEquals(0.4f, mapping.getCenterRatio(1), 0.001f);
        assertEquals(Integer.valueOf(10), mapping.noteForTap(0, 400));
        assertEquals(0f, mapping.getCenterRatio(0), 0.001f);
        for (int i = 0; i < mapping.getKeyCount() - 1; i++) {
            assertTrue(mapping.getCenterRatio(i) < mapping.getCenterRatio(i + 1));
        }
    }

    @Test
    public void adaptiveDeadzoneFollowsTheMovingLaneCenters() {
        MultiKeyMapping mapping = new MultiKeyMapping(new int[]{60, 61},
                20, 1, 45, MultiKeyMapping.ADAPTIVE_LANES);

        assertEquals(Integer.valueOf(60), mapping.noteForTap(400, 1000));
        assertNull(mapping.noteForTap(575, 1000));
    }

    @Test
    public void supportsTenKeysAndCustomMidiNotes() {
        int[] notes = new int[]{20, 21, 22, 23, 24, 25, 26, 27, 28, 29};
        MultiKeyMapping mapping = new MultiKeyMapping(notes, 0, 8, 8,
                MultiKeyMapping.STATIC_LANES);

        assertEquals(10, mapping.getKeyCount());
        assertEquals(Integer.valueOf(29), mapping.noteForTap(950, 1000));
    }

    @Test
    public void repeatedMidiNotesStayActiveUntilAllPointersRelease() {
        MidiNoteTracker tracker = new MidiNoteTracker();

        assertTrue(tracker.shouldSend(true, 60));
        assertFalse(tracker.shouldSend(true, 60));
        assertFalse(tracker.shouldSend(false, 60));
        assertTrue(tracker.shouldSend(false, 60));
        assertFalse(tracker.shouldSend(false, 60));
    }

    @Test
    public void failedNoteOnDoesNotLeaveNoteMarkedActive() {
        MidiNoteTracker tracker = new MidiNoteTracker();

        assertTrue(tracker.shouldSend(true, 60));
        tracker.noteOnSendFailed(60);

        assertTrue(tracker.shouldSend(true, 60));
        assertTrue(tracker.shouldSend(false, 60));
    }
}
