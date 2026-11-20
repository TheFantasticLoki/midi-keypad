package gay.nihil.lena.midikeypad;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TwoKeyMappingTest {
    @Test
    public void fixedDeadzoneIgnoresTouchesAroundTheSplit() {
        assertNull(TwoKeyDeadzoneMapping.noteForTap(500, 1000, 8));
        assertEquals(Integer.valueOf(60), TwoKeyDeadzoneMapping.noteForTap(450, 1000, 8));
        assertEquals(Integer.valueOf(61), TwoKeyDeadzoneMapping.noteForTap(550, 1000, 8));
        assertEquals(Integer.valueOf(61), TwoKeyDeadzoneMapping.noteForTap(500, 1000, 0));
    }

    @Test
    public void positionHistoryMovesAdaptiveSplitBetweenRecentTouchClusters() {
        AdaptiveSplitMapping mapping = new AdaptiveSplitMapping(0, 1, 20,
                AdaptiveSplitMapping.POSITION_HISTORY, 200);

        assertEquals(Integer.valueOf(60), mapping.noteForTap(400, 1000, 100));
        assertEquals(0.575f, mapping.getSplitRatio(), 0.001f);
        assertEquals(Integer.valueOf(61), mapping.noteForTap(600, 1000, 300));
        assertEquals(0.5f, mapping.getSplitRatio(), 0.001f);
    }

    @Test
    public void rapidAlternationKeepsFastTapsOnOppositeKeys() {
        AdaptiveSplitMapping mapping = new AdaptiveSplitMapping(8, 8, 8,
                AdaptiveSplitMapping.RAPID_ALTERNATION, 200);

        assertEquals(Integer.valueOf(60), mapping.noteForTap(250, 1000, 100));
        assertEquals(Integer.valueOf(61), mapping.noteForTap(250, 1000, 250));
    }

    @Test
    public void rapidAlternationDoesNotBypassTheDeadzone() {
        AdaptiveSplitMapping mapping = new AdaptiveSplitMapping(8, 8, 8,
                AdaptiveSplitMapping.RAPID_ALTERNATION, 200);

        assertEquals(Integer.valueOf(60), mapping.noteForTap(250, 1000, 100));
        assertNull(mapping.noteForTap(500, 1000, 250));
        assertEquals(Integer.valueOf(61), mapping.noteForTap(250, 1000, 300));
    }

    @Test
    public void adaptiveTargetsMoveTowardRecentTouchesAndRejectFarMisses() {
        AdaptiveTargetsMapping mapping = new AdaptiveTargetsMapping(8, 30, 1, 20,
                AdaptiveSplitMapping.POSITION_HISTORY, 200);

        assertEquals(Integer.valueOf(60), mapping.noteForTap(350, 1000, 100));
        assertEquals(0.35f, mapping.getLeftCenterRatio(), 0.001f);
        assertEquals(0.55f, mapping.getDeadzoneCenterRatio(), 0.001f);

        AdaptiveTargetsMapping narrowMapping = new AdaptiveTargetsMapping(8, 10, 4, 5,
                AdaptiveSplitMapping.POSITION_HISTORY, 200);
        assertNull(narrowMapping.noteForTap(980, 1000, 100));
    }

    @Test
    public void rapidTargetAlternationDoesNotRequireTheTapToHitTheOppositeTarget() {
        AdaptiveTargetsMapping mapping = new AdaptiveTargetsMapping(8, 10, 4, 5,
                AdaptiveSplitMapping.RAPID_ALTERNATION, 200);

        assertEquals(Integer.valueOf(60), mapping.noteForTap(250, 1000, 100));
        assertEquals(Integer.valueOf(61), mapping.noteForTap(250, 1000, 250));
    }

    @Test
    public void adaptiveTargetsCanReachScreenEdgesWithoutCrossing() {
        AdaptiveTargetsMapping mapping = new AdaptiveTargetsMapping(8, 20, 1, 45,
                AdaptiveSplitMapping.POSITION_HISTORY, 200);

        assertEquals(Integer.valueOf(60), mapping.noteForTap(0, 1000, 100));
        assertEquals(0f, mapping.getLeftCenterRatio(), 0.001f);
        assertEquals(Integer.valueOf(61), mapping.noteForTap(1000, 1000, 300));
        assertEquals(1f, mapping.getRightCenterRatio(), 0.001f);
        assertTrue(mapping.getLeftCenterRatio() < mapping.getRightCenterRatio());
    }
}
