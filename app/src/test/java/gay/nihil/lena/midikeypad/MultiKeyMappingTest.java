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
    public void tapHistoryMovesAdaptiveCentersAsTheHandShifts() {
        MultiKeyMapping mapping = new MultiKeyMapping(new int[]{60, 61},
                0, 1, 45, MultiKeyMapping.ADAPTIVE_LANES, true);

        assertEquals(Integer.valueOf(60),
                mapping.noteForTap(350, 1000, 100));
        assertEquals(Integer.valueOf(61),
                mapping.noteForTap(850, 1000, 200));

        assertEquals(0.35f, mapping.getCenterRatio(0), 0.001f);
        assertEquals(0.85f, mapping.getCenterRatio(1), 0.001f);
        assertEquals(0.6f, mapping.getDeadzoneCenterRatio(0), 0.001f);
    }

    @Test
    public void screenContextCanBeDisabled() {
        MultiKeyMapping mapping = new MultiKeyMapping(new int[]{60, 61},
                0, 1, 45, MultiKeyMapping.ADAPTIVE_LANES, false);

        assertEquals(Integer.valueOf(60), mapping.noteForTap(300, 1000, 100));
        assertEquals(Integer.valueOf(61), mapping.noteForTap(600, 1000, 200));

        assertEquals(0.3f, mapping.getCenterRatio(0), 0.001f);
        assertEquals(0.6f, mapping.getCenterRatio(1), 0.001f);
    }

    @Test
    public void screenClustersFromOneInitialLanePlaceBothCentersAroundTheirMidpoint() {
        MultiKeyMapping mapping = new MultiKeyMapping(new int[]{60, 61},
                8, 4, 45, MultiKeyMapping.ADAPTIVE_LANES, true);

        assertEquals(Integer.valueOf(60), mapping.noteForTap(200, 1000, 100));
        assertEquals(Integer.valueOf(60), mapping.noteForTap(400, 1000, 200));
        assertEquals(Integer.valueOf(60), mapping.noteForTap(210, 1000, 300));
        assertEquals(Integer.valueOf(61), mapping.noteForTap(410, 1000, 400));

        assertEquals(0.205f, mapping.getCenterRatio(0), 0.001f);
        assertEquals(0.405f, mapping.getCenterRatio(1), 0.001f);
        assertEquals(0.305f, mapping.getDeadzoneCenterRatio(0), 0.001f);
        assertEquals(-0.195f, mapping.getLayoutShiftRatio(), 0.001f);
    }

    @Test
    public void oneTargetMovesInsideItsButtonWithoutMovingTheLaneLayout() {
        MultiKeyMapping mapping = new MultiKeyMapping(new int[]{60, 61},
                8, 4, 45, MultiKeyMapping.ADAPTIVE_LANES, true);
        mapping.noteForTap(200, 1000, 100);
        mapping.noteForTap(400, 1000, 200);
        mapping.noteForTap(210, 1000, 300);
        mapping.noteForTap(410, 1000, 400);
        float initialShift = mapping.getLayoutShiftRatio();

        mapping.noteForTap(120, 1000, 500);

        assertEquals(0.15f, mapping.getCenterRatio(0), 0.002f);
        assertEquals(0.405f, mapping.getCenterRatio(1), 0.001f);
        assertEquals(initialShift, mapping.getLayoutShiftRatio(), 0.001f);
    }

    @Test
    public void buttonsRecenterOnlyAfterTargetsShareTheSameDrift() {
        MultiKeyMapping mapping = new MultiKeyMapping(new int[]{60, 61},
                8, 4, 45, MultiKeyMapping.ADAPTIVE_LANES, true);
        mapping.noteForTap(200, 1000, 100);
        mapping.noteForTap(400, 1000, 200);
        mapping.noteForTap(210, 1000, 300);
        mapping.noteForTap(410, 1000, 400);
        float initialShift = mapping.getLayoutShiftRatio();

        mapping.noteForTap(80, 1000, 500);
        assertEquals(initialShift, mapping.getLayoutShiftRatio(), 0.001f);
        mapping.noteForTap(350, 1000, 600);

        assertEquals(initialShift - 0.0585f, mapping.getLayoutShiftRatio(), 0.002f);
        assertEquals(0.124f, mapping.getCenterRatio(0), 0.002f);
        assertEquals(0.369f, mapping.getCenterRatio(1), 0.002f);
    }

    @Test
    public void adaptiveMasterTargetRejectsFarMissesWithoutLearningThem() {
        MultiKeyMapping mapping = new MultiKeyMapping(new int[]{60, 61},
                8, 4, 5, MultiKeyMapping.ADAPTIVE_LANES, false, 10);

        assertNull(mapping.noteForTap(900, 1000, 100));
        assertEquals(0.25f, mapping.getCenterRatio(0), 0.001f);
        assertEquals(0.75f, mapping.getCenterRatio(1), 0.001f);
        assertEquals(10, mapping.getTargetWidthPercent());
        assertEquals(Integer.valueOf(61), mapping.noteForTap(800, 1000, 500));
    }

    @Test
    public void screenContextAcceptedZonesStopAtTheDeadzoneEvenWhenLaneLayoutDiffers() {
        MultiKeyMapping mapping = new MultiKeyMapping(new int[]{60, 61},
                11, 4, 45, MultiKeyMapping.ADAPTIVE_LANES, true, 7);
        mapping.noteForTap(370, 1000, 100);
        mapping.noteForTap(720, 1000, 200);
        mapping.noteForTap(400, 1000, 300);
        mapping.noteForTap(740, 1000, 400);

        assertEquals(0.485f, mapping.getAcceptedEndRatio(0), 0.002f);
        assertEquals(0.63f, mapping.getAcceptedStartRatio(1), 0.002f);
        assertEquals(Integer.valueOf(60), mapping.noteForTap(390, 1000, 500));
        assertNull(mapping.noteForTap(557, 1000, 600));
        assertEquals(Integer.valueOf(61), mapping.noteForTap(730, 1000, 700));
    }

    @Test
    public void repeatedTapsMoveTargetsAndRecenterAsHandShifts() {
        MultiKeyMapping mapping = new MultiKeyMapping(new int[]{60, 61},
                8, 4, 45, MultiKeyMapping.ADAPTIVE_LANES, true);
        mapping.noteForTap(200, 1000, 100);
        mapping.noteForTap(400, 1000, 200);
        mapping.noteForTap(210, 1000, 300);
        mapping.noteForTap(410, 1000, 400);

        float initialSpacing = mapping.getCenterRatio(1) - mapping.getCenterRatio(0);
        float initialLayoutShift = mapping.getLayoutShiftRatio();
        mapping.noteForTap(150, 1000, 500);
        mapping.noteForTap(450, 1000, 550);
        mapping.noteForTap(120, 1000, 600);
        mapping.noteForTap(480, 1000, 650);

        assertTrue(mapping.getCenterRatio(0) < 0.15f);
        assertTrue(mapping.getCenterRatio(1) > 0.45f);
        assertEquals(initialLayoutShift, mapping.getLayoutShiftRatio(), 0.001f);
        assertTrue(mapping.getCenterRatio(1) - mapping.getCenterRatio(0) > initialSpacing);
    }

    @Test
    public void repeatedTapsCatchUpAfterAnAbruptHandShift() {
        MultiKeyMapping mapping = new MultiKeyMapping(new int[]{60, 61},
                8, 4, 45, MultiKeyMapping.ADAPTIVE_LANES, true);
        mapping.noteForTap(200, 1000, 100);
        mapping.noteForTap(400, 1000, 200);
        mapping.noteForTap(210, 1000, 300);
        mapping.noteForTap(410, 1000, 400);

        mapping.noteForTap(400, 1000, 500);
        mapping.noteForTap(600, 1000, 550);
        mapping.noteForTap(400, 1000, 600);
        mapping.noteForTap(600, 1000, 650);
        mapping.noteForTap(400, 1000, 700);
        mapping.noteForTap(600, 1000, 750);

        assertEquals(0.4f, mapping.getCenterRatio(0), 0.03f);
        assertEquals(0.6f, mapping.getCenterRatio(1), 0.03f);
    }

    @Test
    public void deadzoneTapNearLearnedFingerIsTrackedWithoutMovingEitherTarget() {
        MultiKeyMapping mapping = new MultiKeyMapping(new int[]{60, 61},
                8, 4, 45, MultiKeyMapping.ADAPTIVE_LANES, true);
        mapping.noteForTap(200, 1000, 100);
        mapping.noteForTap(400, 1000, 200);
        mapping.noteForTap(210, 1000, 300);
        mapping.noteForTap(410, 1000, 400);

        assertNull(mapping.noteForTap(270, 1000, 500));

        assertEquals(0.205f, mapping.getCenterRatio(0), 0.001f);
        assertEquals(0.405f, mapping.getCenterRatio(1), 0.002f);
        assertFalse(mapping.hasDeadzoneCluster(0));
    }

    @Test
    public void repeatedDeadzoneTapsCreateATrackedClusterThatFiltersEdgeHits() {
        MultiKeyMapping mapping = new MultiKeyMapping(new int[]{60, 61},
                8, 4, 45, MultiKeyMapping.ADAPTIVE_LANES, true);
        mapping.noteForTap(200, 1000, 100);
        mapping.noteForTap(400, 1000, 200);
        mapping.noteForTap(210, 1000, 300);
        mapping.noteForTap(410, 1000, 400);

        assertNull(mapping.noteForTap(340, 1000, 500));
        assertNull(mapping.noteForTap(330, 1000, 600));

        assertTrue(mapping.hasDeadzoneCluster(0));
        assertEquals(0.335f, mapping.getDeadzoneClusterCenterRatio(0), 0.002f);
        assertNull(mapping.noteForTap(360, 1000, 700));
        assertEquals(0.205f, mapping.getCenterRatio(0), 0.001f);
        assertEquals(0.405f, mapping.getCenterRatio(1), 0.001f);

        mapping.noteForTap(400, 1000, 800);
        mapping.noteForTap(600, 1000, 900);
        assertEquals(mapping.getDeadzoneCenterRatio(0) + 0.035f,
                mapping.getDeadzoneClusterCenterRatio(0), 0.002f);
    }

    @Test
    public void repeatedNearEdgeTapsJoinDeadzoneClusterWithoutTeachingATarget() {
        MultiKeyMapping mapping = new MultiKeyMapping(new int[]{60, 61},
                8, 4, 45, MultiKeyMapping.ADAPTIVE_LANES, true);
        mapping.noteForTap(200, 1000, 100);
        mapping.noteForTap(400, 1000, 200);
        mapping.noteForTap(210, 1000, 300);
        mapping.noteForTap(410, 1000, 400);

        assertNull(mapping.noteForTap(360, 1000, 500));
        assertNull(mapping.noteForTap(360, 1000, 600));

        assertTrue(mapping.hasDeadzoneCluster(0));
        assertEquals(0.205f, mapping.getCenterRatio(0), 0.001f);
        assertEquals(0.405f, mapping.getCenterRatio(1), 0.001f);
        assertEquals(0.38f, mapping.getAcceptedStartRatio(1), 0.002f);
        assertEquals(Integer.valueOf(61), mapping.noteForTap(390, 1000, 700));
    }


    @Test
    public void tapsInLearnedDeadzoneDoNotDistortScreenClusters() {
        MultiKeyMapping mapping = new MultiKeyMapping(new int[]{60, 61},
                8, 4, 45, MultiKeyMapping.ADAPTIVE_LANES, true);
        mapping.noteForTap(200, 1000, 100);
        mapping.noteForTap(400, 1000, 200);
        mapping.noteForTap(210, 1000, 300);
        mapping.noteForTap(410, 1000, 400);

        assertNull(mapping.noteForTap(305, 1000, 500));
        assertEquals(0.205f, mapping.getCenterRatio(0), 0.001f);
        assertEquals(0.405f, mapping.getCenterRatio(1), 0.001f);
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
