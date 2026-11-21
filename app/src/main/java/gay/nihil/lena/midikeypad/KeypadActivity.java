package gay.nihil.lena.midikeypad;

import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import gay.nihil.lena.midikeypad.databinding.ActivityKeypadBinding;

public class KeypadActivity extends AppCompatActivity {

    MidiInputPort port;

    MidiDevice device;

    KeypadHandler handler;

    private final MidiNoteTracker noteTracker = new MidiNoteTracker();
    private final Map<Integer, Integer> activePointerNotes = new HashMap<>();

    int midiChannel = 3;

    int mode = 0;

    @SuppressLint("ClickableViewAccessibility")
    View.OnTouchListener touchListener = (v, event) -> {
        if (port == null) {
            return false;
        }

        if (handler == null) {
            return false;
        }

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_CANCEL) {
            releaseActivePointerNotes();
            handler.cancelTouches();
            return true;
        }

        KeypadData data = handler.HandleMotionEvent(event);
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            v.invalidate();
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            int pointerId = event.getPointerId(event.getActionIndex());
            Integer note = activePointerNotes.remove(pointerId);
            if (note != null) {
                sendNote(false, note);
                return true;
            }
        }

        if (data != null && data.handled) {
            if (data.sendMidi) {
                if (data.on) {
                    int pointerId = event.getPointerId(event.getActionIndex());
                    activePointerNotes.put(pointerId, data.code);
                    performTapHapticFeedback(v);
                }
                sendNote(data.on, data.code);
            }
            return true;
        }

        return false;
    };

    private void performTapHapticFeedback(View view) {
        int feedbackConstant = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
                ? HapticFeedbackConstants.KEYBOARD_TAP
                : HapticFeedbackConstants.VIRTUAL_KEY;
        view.performHapticFeedback(feedbackConstant);
    }

    private static final byte[] buffer = new byte[3];

    void sendNote(boolean on, int code) {
        boolean shouldSend = on ? noteTracker.noteOn(code) : noteTracker.noteOff(code);
        if (!shouldSend) {
            return;
        }
        sendMidiMessage(on, code);
    }

    private void sendMidiMessage(boolean on, int code) {
        if (on) {
            buffer[0] = (byte) (0x90 + (midiChannel - 1)); // note on
        } else {
            buffer[0] = (byte) (0x80 + (midiChannel - 1)); // note off
        }

        buffer[1] = (byte) code; // pitch is middle C
        buffer[2] = (byte) 127; // max velocity

        int offset = 0;
        try {
            // post is non-blocking
            port.send(buffer, offset, 3);
            if (on) {
                noteTracker.noteOnSent(code);
            } else {
                noteTracker.noteOffSent(code);
            }
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Error while transmitting data", Toast.LENGTH_LONG).show();
        }
    }

    private void releaseActivePointerNotes() {
        for (int note : activePointerNotes.values()) {
            sendNote(false, note);
        }
        activePointerNotes.clear();
        for (int note : noteTracker.pendingNoteOffs()) {
            sendMidiMessage(false, note);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gay.nihil.lena.midikeypad.databinding.ActivityKeypadBinding binding =
                ActivityKeypadBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController windowInsetsController =
                    Objects.requireNonNull(binding.fullscreenContent.getWindowInsetsController());

            windowInsetsController.hide(WindowInsets.Type.statusBars() |
                    WindowInsets.Type.navigationBars());
        }
        ActionBar actionBar = Objects.requireNonNull(getSupportActionBar());
        actionBar.hide();

        mode = getIntent().getIntExtra("mode", KeypadModes.MASTER);
        if (mode == KeypadModes.MASTER) {
            midiChannel = Math.max(1, Math.min(16, getIntent().getIntExtra("midiChannel", 3)));
        }

        if (getIntent().getBooleanExtra("orientation", false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        Drawable drawable;
        switch (mode) {
            case KeypadModes.MASTER: {
                drawable = AppCompatResources.getDrawable(this, R.drawable.mode0);
                int keyCount = Math.max(MultiKeyMapping.MIN_KEYS,
                        Math.min(MultiKeyMapping.MAX_KEYS,
                                getIntent().getIntExtra("customKeyCount", MultiKeyMapping.MIN_KEYS)));
                int[] customNotes = getIntent().getIntArrayExtra("customNotes");
                if (customNotes == null || customNotes.length != keyCount) {
                    customNotes = new int[keyCount];
                    for (int i = 0; i < keyCount; i++) {
                        customNotes[i] = 60 + i;
                    }
                }
                handler = new MultiKeyKeypadHandler(binding.fullscreenContent, customNotes,
                        midiChannel, getIntent().getIntExtra("customDeadzonePercent", 8),
                        getIntent().getIntExtra("historySize", 12),
                        getIntent().getIntExtra("maxShiftPercent", 8),
                        getIntent().getIntExtra("customLayout", MultiKeyMapping.ADAPTIVE_LANES));
                break;
            }
            case KeypadModes.LEGACY_OSU_STATIC:
            default:
                drawable = AppCompatResources.getDrawable(this, R.drawable.mode0);
                handler = new OsuStaticKeypadHandler(binding.fullscreenContent);
                break;
            case KeypadModes.LEGACY_OSU_DYNAMIC:
                drawable = AppCompatResources.getDrawable(this, R.drawable.mode1);
                handler = new OsuDynamicKeypadHandler(binding.fullscreenContent);
                break;
            case KeypadModes.LEGACY_MANIA:
                drawable = AppCompatResources.getDrawable(this, R.drawable.mode2);
                handler = new ManiaStaticKeypadHandler(binding.fullscreenContent);
                break;
            case KeypadModes.LEGACY_DEADZONE:
                drawable = AppCompatResources.getDrawable(this, R.drawable.mode0);
                handler = new OsuDeadzoneKeypadHandler(binding.fullscreenContent,
                        getIntent().getIntExtra("deadzonePercent", 8));
                break;
            case KeypadModes.LEGACY_ADAPTIVE_SPLIT:
                drawable = AppCompatResources.getDrawable(this, R.drawable.mode0);
                handler = new OsuAdaptiveSplitKeypadHandler(binding.fullscreenContent,
                        getIntent().getIntExtra("deadzonePercent", 8),
                        getIntent().getIntExtra("historySize", 12),
                        getIntent().getIntExtra("maxShiftPercent", 8),
                        getIntent().getIntExtra("adaptiveStrategy", AdaptiveSplitMapping.POSITION_HISTORY),
                        getIntent().getIntExtra("alternationIntervalMs", 200));
                break;
            case KeypadModes.LEGACY_ADAPTIVE_TARGETS:
                drawable = AppCompatResources.getDrawable(this, R.drawable.mode0);
                handler = new OsuAdaptiveTargetsKeypadHandler(binding.fullscreenContent,
                        getIntent().getIntExtra("deadzonePercent", 8),
                        getIntent().getIntExtra("targetWidthPercent", 40),
                        getIntent().getIntExtra("historySize", 12),
                        getIntent().getIntExtra("maxShiftPercent", 8),
                        getIntent().getIntExtra("adaptiveStrategy", AdaptiveSplitMapping.POSITION_HISTORY),
                        getIntent().getIntExtra("alternationIntervalMs", 200));
                break;
        }
        binding.fullscreenContent.setImageDrawable(drawable);
        binding.keypadOverlay.setHandler(handler);
        binding.keypadOverlay.setOnTouchListener(touchListener);

        MidiManager m = (MidiManager) this.getSystemService(Context.MIDI_SERVICE);

        MidiDeviceInfo info = getIntent().getParcelableExtra("device");

        MidiDeviceInfo.PortInfo[] portInfos = Objects.requireNonNull(info).getPorts();
        boolean inputPortFound = false;
        for (int j = 0; j < portInfos.length; j++) {
            String portName = portInfos[j].getName();
            if (portInfos[j].getType() == MidiDeviceInfo.PortInfo.TYPE_INPUT) {
                inputPortFound = true;
                int finalJ = j;
                m.openDevice(info, dev -> {
                    if (dev == null) {
                        Toast.makeText(getApplicationContext(), "Could not open device " + portName, Toast.LENGTH_LONG).show();
                        finish();
                    } else if (isFinishing() || isDestroyed()) {
                        closeDevice(dev);
                    } else {
                        MidiInputPort openedPort = dev.openInputPort(finalJ);
                        if (openedPort == null) {
                            closeDevice(dev);
                            Toast.makeText(getApplicationContext(),
                                    "Could not open MIDI input port " + portName,
                                    Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            port = openedPort;
                            device = dev;
                        }
                    }
                }, new Handler(Looper.getMainLooper()));
                return;
            }
        }
        if (!inputPortFound) {
            Toast.makeText(getApplicationContext(),
                    "The selected MIDI device has no input port", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void closeDevice(MidiDevice midiDevice) {
        try {
            midiDevice.close();
        } catch (IOException e) {
            Log.e("KeypadActivity", "Failed to close MIDI device", e);
        }
    }

    @Override
    protected void onPause() {
        releaseActivePointerNotes();
        if (handler != null) {
            handler.cancelTouches();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        releaseActivePointerNotes();
        if (handler != null) {
            handler.cancelTouches();
        }
        super.onDestroy();
        if (port != null) {
            try {
                port.close();
            } catch (IOException e) {
                Log.e("KeypadActivity", "Failed to close MIDI input port", e);
            }
        }
        if (device != null) {
            try {
                device.close();
            } catch (IOException e) {
                Log.e("KeypadActivity", "Failed to close MIDI device", e);
            }
        }
    }
}
