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
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Toast;

import java.io.IOException;
import java.util.Objects;

import gay.nihil.lena.midikeypad.databinding.ActivityKeypadBinding;

public class KeypadActivity extends AppCompatActivity {

    MidiInputPort port;

    MidiDevice device;

    KeypadHandler handler;

    int mode = 0;

    @SuppressLint("ClickableViewAccessibility")
    View.OnTouchListener touchListener = (v, event) -> {
        if (port == null) {
            return false;
        }
        Log.i("a", "" + event.getEventTime());

        if (handler == null) {
            return false;
        }

        KeypadData data = handler.HandleMotionEvent(event);
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            v.invalidate();
        }

        if (data != null && data.handled) {
            if (data.sendMidi) {
                sendNote(data.on, data.code);
            }
            return true;
        }

        return false;
    };

    private static final byte[] buffer = new byte[3];

    void sendNote(boolean on, int code) {
        int channel = 3; // MIDI channels 1-16 are encoded as 0-15.

        if (on) {
            buffer[0] = (byte) (0x90 + (channel - 1)); // note on
        } else {
            buffer[0] = (byte) (0x80 + (channel - 1)); // note off
        }

        buffer[1] = (byte) code; // pitch is middle C
        buffer[2] = (byte) 127; // max velocity

        int offset = 0;
        try {
            // post is non-blocking
            port.send(buffer, offset, 3);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Error while transmitting data", Toast.LENGTH_LONG).show();
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

        mode = getIntent().getIntExtra("mode", 0);

        if (getIntent().getBooleanExtra("orientation", false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        Drawable drawable;
        switch (mode) {
            default:
                drawable = AppCompatResources.getDrawable(this, R.drawable.mode0);
                handler = new OsuStaticKeypadHandler(binding.fullscreenContent);
                break;
            case 1:
                drawable = AppCompatResources.getDrawable(this, R.drawable.mode1);
                handler = new OsuDynamicKeypadHandler(binding.fullscreenContent);
                break;
            case 2:
                drawable = AppCompatResources.getDrawable(this, R.drawable.mode2);
                handler = new ManiaStaticKeypadHandler(binding.fullscreenContent);
                break;
            case 3:
                drawable = AppCompatResources.getDrawable(this, R.drawable.mode0);
                handler = new OsuDeadzoneKeypadHandler(binding.fullscreenContent,
                        getIntent().getIntExtra("deadzonePercent", 8));
                break;
            case 4:
                drawable = AppCompatResources.getDrawable(this, R.drawable.mode0);
                handler = new OsuAdaptiveSplitKeypadHandler(binding.fullscreenContent,
                        getIntent().getIntExtra("deadzonePercent", 8),
                        getIntent().getIntExtra("historySize", 12),
                        getIntent().getIntExtra("maxShiftPercent", 8),
                        getIntent().getIntExtra("adaptiveStrategy", AdaptiveSplitMapping.POSITION_HISTORY),
                        getIntent().getIntExtra("alternationIntervalMs", 200));
                break;
            case 5:
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
        for (int j = 0; j < portInfos.length; j++) {
            String portName = portInfos[j].getName();
            if (portInfos[j].getType() == MidiDeviceInfo.PortInfo.TYPE_INPUT) {
                int finalJ = j;
                m.openDevice(info, dev -> {
                    if (dev == null) {
                        Toast.makeText(getApplicationContext(), "Could not open device " + portName, Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        port = dev.openInputPort(finalJ);
                        device = dev;
                    }
                }, new Handler(Looper.getMainLooper()));
                return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (port != null) {
                port.close();
            }
            if (device != null) {
                device.close();
            }
        } catch (IOException e) {
            // ignored
        }
    }
}
