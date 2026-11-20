package gay.nihil.lena.midikeypad;

import android.graphics.Canvas;
import android.view.View;

public class OsuDeadzoneKeypadHandler extends OsuTwoKeyKeypadHandler {
    private final int deadzonePercent;

    public OsuDeadzoneKeypadHandler(View view, int deadzonePercent) {
        super(view);
        this.deadzonePercent = deadzonePercent;
    }

    @Override
    protected Integer noteForTap(float x, float width, long eventTime) {
        return TwoKeyDeadzoneMapping.noteForTap(x, width, deadzonePercent);
    }

    @Override
    protected void drawModeOverlay(Canvas canvas, int width, int height, float density) {
        TwoKeyOverlayRenderer.drawSplit(canvas, width, height, density,
                0.5f, deadzonePercent, "Deadzone " + deadzonePercent + "% | split 50%");
    }
}
