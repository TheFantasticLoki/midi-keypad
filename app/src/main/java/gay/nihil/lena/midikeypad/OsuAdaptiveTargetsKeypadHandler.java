package gay.nihil.lena.midikeypad;

import android.graphics.Canvas;
import android.view.View;

public class OsuAdaptiveTargetsKeypadHandler extends OsuTwoKeyKeypadHandler {
    private final AdaptiveTargetsMapping mapping;

    public OsuAdaptiveTargetsKeypadHandler(View view, int deadzonePercent, int targetWidthPercent,
                                           int historySize, int maxShiftPercent, int strategy,
                                           int alternationIntervalMs) {
        super(view);
        this.deadzonePercent = deadzonePercent;
        this.targetWidthPercent = targetWidthPercent;
        this.maxShiftPercent = maxShiftPercent;
        mapping = new AdaptiveTargetsMapping(deadzonePercent, targetWidthPercent, historySize,
                maxShiftPercent, strategy, alternationIntervalMs);
    }

    @Override
    protected Integer noteForTap(float x, float width, long eventTime) {
        return mapping.noteForTap(x, width, eventTime);
    }

    @Override
    protected void drawModeOverlay(Canvas canvas, int width, int height, float density) {
        TwoKeyOverlayRenderer.drawTargets(canvas, width, height, density,
                mapping.getLeftCenterRatio(), mapping.getRightCenterRatio(),
                mapping.getDeadzoneCenterRatio(), deadzonePercent, targetWidthPercent,
                maxShiftPercent, "Deadzone " + deadzonePercent + "% @ "
                        + Math.round(mapping.getDeadzoneCenterRatio() * 100) + "% | target "
                        + targetWidthPercent + "% | L " + Math.round(mapping.getLeftCenterRatio() * 100)
                        + "% R " + Math.round(mapping.getRightCenterRatio() * 100) + "%");
    }

    private final int deadzonePercent;
    private final int targetWidthPercent;
    private final int maxShiftPercent;
}
