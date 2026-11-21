package gay.nihil.lena.midikeypad;

import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

public class MultiKeyKeypadHandler extends KeypadHandler {
    private final MultiKeyMapping mapping;
    private final int midiChannel;
    private final Map<Integer, Integer> pointerNotes = new HashMap<>();

    public MultiKeyKeypadHandler(View view, int[] notes, int midiChannel,
                                 int deadzonePercent, int historySize,
                                 int maxShiftPercent, int layout) {
        super(view);
        mapping = new MultiKeyMapping(notes, deadzonePercent, historySize, maxShiftPercent, layout);
        this.midiChannel = midiChannel;
    }

    @Override
    public KeypadData HandleMotionEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            Integer note = mapping.noteForTap(event.getX(pointerIndex), view.getWidth());
            if (note == null) {
                pointerNotes.put(pointerId, -1);
                return KeypadData.consumedOnly();
            }
            pointerNotes.put(pointerId, note);
            return new KeypadData(true, true, note);
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            Integer note = pointerNotes.remove(pointerId);
            if (note == null) {
                return new KeypadData(false, false, 0);
            }
            if (note < 0) {
                return KeypadData.consumedOnly();
            }
            return new KeypadData(true, false, note);
        }

        return new KeypadData(false, false, 0);
    }

    @Override
    public void cancelTouches() {
        pointerNotes.clear();
    }

    @Override
    public void drawOverlay(Canvas canvas, int width, int height, float density) {
        TwoKeyOverlayRenderer.drawMultiKey(canvas, width, height, density, mapping,
                midiChannel, mapping.isAdaptive() ? "adaptive" : "static",
                getDeadzoneLabel());
    }

    private String getDeadzoneLabel() {
        return "gap " + getDeadzonePercent() + "% lane";
    }

    private int getDeadzonePercent() {
        return mapping.getDeadzonePercent();
    }
}
