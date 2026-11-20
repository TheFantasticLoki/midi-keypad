package gay.nihil.lena.midikeypad;

import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

public abstract class KeypadHandler {
    View view;

    public KeypadHandler(View view) {
        this.view = view;
    }

    public abstract KeypadData HandleMotionEvent(MotionEvent event);

    public void drawOverlay(Canvas canvas, int width, int height, float density) {
    }
}

