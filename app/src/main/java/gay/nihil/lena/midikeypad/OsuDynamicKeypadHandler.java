package gay.nihil.lena.midikeypad;

import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

public class OsuDynamicKeypadHandler extends KeypadHandler {
    long lastEventTime;
    boolean lastKey;
    private final Map<Integer, Integer> pointerNotes = new HashMap<>();

    public OsuDynamicKeypadHandler(View view) {
        super(view);
        this.lastKey = false;
        this.lastEventTime = 0;
    }

    @Override
    public KeypadData HandleMotionEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            if (event.getEventTime() - lastEventTime < 200) {
                lastKey = !lastKey;
            }
            lastEventTime = event.getEventTime();
            int code = lastKey ? 61 : 60;
            pointerNotes.put(pointerId, code);
            return new KeypadData(true, true, code);
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
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
