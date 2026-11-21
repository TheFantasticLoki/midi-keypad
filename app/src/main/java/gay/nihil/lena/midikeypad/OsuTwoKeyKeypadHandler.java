package gay.nihil.lena.midikeypad;

import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

abstract class OsuTwoKeyKeypadHandler extends KeypadHandler {
    private final Map<Integer, Integer> pointerNotes = new HashMap<>();

    OsuTwoKeyKeypadHandler(View view) {
        super(view);
    }

    protected abstract Integer noteForTap(float x, float width, long eventTime);

    protected abstract void drawModeOverlay(Canvas canvas, int width, int height, float density);

    @Override
    public final void drawOverlay(Canvas canvas, int width, int height, float density) {
        drawModeOverlay(canvas, width, height, density);
    }

    @Override
    public KeypadData HandleMotionEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            Integer code = noteForTap(event.getX(pointerIndex), view.getWidth(), event.getEventTime());
            if (code == null) {
                pointerNotes.put(pointerId, -1);
                return KeypadData.consumedOnly();
            }
            pointerNotes.put(pointerId, code);
            return new KeypadData(true, true, code);
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            Integer code = pointerNotes.remove(pointerId);
            if (code == null) {
                return new KeypadData(false, false, 0);
            }
            if (code < 0) {
                return KeypadData.consumedOnly();
            }
            return new KeypadData(true, false, code);
        }

        return new KeypadData(false, false, 0);
    }

    @Override
    public void cancelTouches() {
        pointerNotes.clear();
    }
}
