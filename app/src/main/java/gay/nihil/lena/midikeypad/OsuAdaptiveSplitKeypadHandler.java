package gay.nihil.lena.midikeypad;

import android.graphics.Canvas;
import android.view.View;

public class OsuAdaptiveSplitKeypadHandler extends OsuTwoKeyKeypadHandler {
    private final AdaptiveSplitMapping mapping;

    public OsuAdaptiveSplitKeypadHandler(View view, int deadzonePercent, int historySize,
                                         int maxShiftPercent, int strategy,
                                         int alternationIntervalMs) {
        super(view);
        this.deadzonePercent = deadzonePercent;
        mapping = new AdaptiveSplitMapping(deadzonePercent, historySize, maxShiftPercent,
                strategy, alternationIntervalMs);
    }

    @Override
    protected Integer noteForTap(float x, float width, long eventTime) {
        return mapping.noteForTap(x, width, eventTime);
    }

    @Override
    protected void drawModeOverlay(Canvas canvas, int width, int height, float density) {
        float split = mapping.getSplitRatio();
        TwoKeyOverlayRenderer.drawSplit(canvas, width, height, density,
                split, deadzonePercent, "Deadzone " + deadzonePercent + "% | split "
                        + Math.round(split * 100) + "%");
    }

    private final int deadzonePercent;
}
