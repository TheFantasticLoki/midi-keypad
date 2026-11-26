package gay.nihil.lena.midikeypad;

import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

public class OsuStaticKeypadHandler extends KeypadHandler {
    private final Map<Integer, Integer> pointerNotes = new HashMap<>();

    public OsuStaticKeypadHandler(View view) {
        super(view);
    }

    @Override
    public KeypadData HandleMotionEvent(MotionEvent event) {
        int a = event.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN) {
            int pointerIndex = event.getActionIndex();
            int pointerId = event.getPointerId(pointerIndex);
            int code = event.getX(pointerIndex) > ((float) view.getWidth() / 2) ? 61 : 60;
            pointerNotes.put(pointerId, code);
            return new KeypadData(true, true, code);
        } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_POINTER_UP) {
            int pointerId = event.getPointerId(event.getActionIndex());
            Integer code = pointerNotes.remove(pointerId);
            if (code == null) {
                return new KeypadData(false, false, 0);
            }
            return new KeypadData(true, false, code);
        } else {
            return new KeypadData(false, false, 0);
        }
    }

    @Override
    public void cancelTouches() {
        pointerNotes.clear();
    }
}
