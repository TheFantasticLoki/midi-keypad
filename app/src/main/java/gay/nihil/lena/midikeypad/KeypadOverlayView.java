package gay.nihil.lena.midikeypad;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

public class KeypadOverlayView extends View {
    private KeypadHandler handler;

    public KeypadOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setHandler(KeypadHandler handler) {
        this.handler = handler;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (handler != null) {
            handler.drawOverlay(canvas, getWidth(), getHeight(),
                    getResources().getDisplayMetrics().density);
        }
    }
}
