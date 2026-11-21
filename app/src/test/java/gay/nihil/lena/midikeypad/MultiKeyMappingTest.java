package gay.nihil.lena.midikeypad;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MultiKeyMappingTest {
    @Test
    public void previousMasterModeMigratesToPositionZero() {
        assertEquals(KeypadModes.MASTER,
                KeypadModes.fromPreviousModeOrder(KeypadModes.LEGACY_ADAPTIVE_TARGETS));
    }

    @Test
    public void previousLegacyModesMoveDownOnePosition() {
        for (int previousMode = 0; previousMode < 6; previousMode++) {
            assertEquals(previousMode + 1,
                    KeypadModes.fromPreviousModeOrder(previousMode));
        }
    }

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
        assertEquals(Integer.valueOf(61), mapping.noteForTap(121, 400));
    }

    @Test
    public void twoKeyAdaptiveDeadzoneMatchesAdaptiveButtonScreenWidth() {
        int deadzonePercent = 8;
        MultiKeyMapping multiKey = new MultiKeyMapping(new int[]{60, 61},
                deadzonePercent, 8, 8, MultiKeyMapping.ADAPTIVE_LANES);
        AdaptiveTargetsMapping adaptiveButtons = new AdaptiveTargetsMapping(deadzonePercent,
                40, 8, 8, AdaptiveSplitMapping.POSITION_HISTORY, 200);

        assertNull(multiKey.noteForTap(539, 1000));
        assertNull(adaptiveButtons.noteForTap(539, 1000, 100));
        assertEquals(Integer.valueOf(61), multiKey.noteForTap(541, 1000));
        assertEquals(Integer.valueOf(61), adaptiveButtons.noteForTap(541, 1000, 100));
    }

    @Test
    public void largeDeadzoneDoesNotConsumeEveryEightKeyLane() {
        MultiKeyMapping mapping = new MultiKeyMapping(
                new int[]{20, 21, 22, 23, 24, 25, 26, 27},
                30, 8, 8, MultiKeyMapping.STATIC_LANES);

        assertEquals(Integer.valueOf(20), mapping.noteForTap(62, 1000));
        assertTrue(mapping.getDeadzoneHalfWidthRatio(0) < 1f / MultiKeyMapping.MAX_KEYS / 2f);
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

        assertEquals(Integer.valueOf(60), mapping.noteForTap(390, 1000));
        assertNull(mapping.noteForTap(575, 1000));
    }

    @Test
    public void adaptiveDeadzoneTapsTeachTheNearestLaneWithoutSendingANote() {
        MultiKeyMapping mapping = new MultiKeyMapping(new int[]{60, 61},
                8, 1, 45, MultiKeyMapping.ADAPTIVE_LANES);

        assertNull(mapping.noteForTap(470, 1000));
        assertEquals(0.47f, mapping.getCenterRatio(0), 0.001f);
        assertEquals(Integer.valueOf(60), mapping.noteForTap(470, 1000));
    }

    @Test
    public void supportsEightKeysAndCustomMidiNotes() {
        int[] notes = new int[]{20, 21, 22, 23, 24, 25, 26, 27};
        MultiKeyMapping mapping = new MultiKeyMapping(notes, 0, 8, 8,
                MultiKeyMapping.STATIC_LANES);

        assertEquals(8, mapping.getKeyCount());
        assertEquals(Integer.valueOf(27), mapping.noteForTap(950, 1000));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMoreThanEightKeys() {
        new MultiKeyMapping(new int[]{20, 21, 22, 23, 24, 25, 26, 27, 28},
                0, 8, 8, MultiKeyMapping.STATIC_LANES);
    }

    @Test
    public void repeatedMidiNotesStayActiveUntilAllPointersRelease() {
        MidiNoteTracker tracker = new MidiNoteTracker();

        assertTrue(tracker.noteOn(60));
        tracker.noteOnSent(60);
        assertFalse(tracker.noteOn(60));
        assertFalse(tracker.noteOff(60));
        assertTrue(tracker.noteOff(60));
        tracker.noteOffSent(60);
        assertFalse(tracker.noteOff(60));
    }

    @Test
    public void failedNoteOnCanRetryWithoutCorruptingOverlappingPointers() {
        MidiNoteTracker tracker = new MidiNoteTracker();

        assertTrue(tracker.noteOn(60));
        assertTrue(tracker.noteOn(60));
        tracker.noteOnSent(60);
        assertFalse(tracker.noteOff(60));
        assertTrue(tracker.noteOff(60));
        tracker.noteOffSent(60);
        assertTrue(tracker.pendingNoteOffs().isEmpty());
    }

    @Test
    public void failedNoteOffRemainsPendingForRetry() {
        MidiNoteTracker tracker = new MidiNoteTracker();

        assertTrue(tracker.noteOn(60));
        tracker.noteOnSent(60);
        assertTrue(tracker.noteOff(60));
        assertEquals(1, tracker.pendingNoteOffs().size());

        tracker.noteOffSent(60);
        assertTrue(tracker.pendingNoteOffs().isEmpty());
    }
}
