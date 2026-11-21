package gay.nihil.lena.midikeypad;

import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;

public class ManiaStaticKeypadHandler extends KeypadHandler {
    public ManiaStaticKeypadHandler(View view) {
        super(view);
    }

    // Store each pointer's original note so moving a finger does not change its release.
    private final HashMap<Integer, Integer> motions = new HashMap<>();

    @Override
    public KeypadData HandleMotionEvent(MotionEvent event) {
        // https://android-developers.googleblog.com/2010/06/making-sense-of-multitouch.html
        final int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = event.getPointerId(pointerIndex);
        int actionType = event.getActionMasked();

        if (actionType == MotionEvent.ACTION_DOWN || actionType == MotionEvent.ACTION_POINTER_DOWN) {
            float x = event.getX(pointerIndex);
            float width = view.getWidth();
            if (width <= 0) {
                return KeypadData.consumedOnly();
            }
            float keyWidth = width / 4;

            int lane = Math.max(0, Math.min(3, (int) (x / keyWidth)));
            int code = 60 + lane;
            motions.put(pointerId, code);

            return new KeypadData(true, true, code);
        } else if (actionType == MotionEvent.ACTION_UP || actionType == MotionEvent.ACTION_POINTER_UP) {
            Integer code = motions.remove(pointerId);
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
        motions.clear();
    }
}
