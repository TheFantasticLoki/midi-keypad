package gay.nihil.lena.midikeypad;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    Button startButton;
    Button showMeButton;
    Spinner mode_spinner;
    Spinner device_spinner;
    Spinner adaptiveStrategySpinner;
    CheckBox landscapeCheckBox;
    SharedPreferences settings;
    SeekBar deadzoneSeekBar;
    SeekBar historySeekBar;
    SeekBar maxShiftSeekBar;
    SeekBar alternationIntervalSeekBar;
    SeekBar targetWidthSeekBar;
    TextView deadzoneLabel;
    TextView historyLabel;
    TextView maxShiftLabel;
    TextView alternationIntervalLabel;
    TextView targetWidthLabel;
    LinearLayout settingsPanel;
    LinearLayout adaptiveSettingsGroup;
    LinearLayout alternationSettingsGroup;
    LinearLayout targetWidthSettingsGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = getSharedPreferences("keypad_settings", MODE_PRIVATE);
        startButton = findViewById(R.id.start_button);
        showMeButton = findViewById(R.id.show_me_button);
        mode_spinner = findViewById(R.id.mode_spinner);
        device_spinner = findViewById(R.id.device_spinner);
        landscapeCheckBox = findViewById(R.id.landscape_checkbox);
        adaptiveStrategySpinner = findViewById(R.id.adaptive_strategy_spinner);
        deadzoneSeekBar = findViewById(R.id.deadzone_seekbar);
        historySeekBar = findViewById(R.id.history_seekbar);
        maxShiftSeekBar = findViewById(R.id.max_shift_seekbar);
        alternationIntervalSeekBar = findViewById(R.id.alternation_interval_seekbar);
        targetWidthSeekBar = findViewById(R.id.target_width_seekbar);
        deadzoneLabel = findViewById(R.id.deadzone_label);
        historyLabel = findViewById(R.id.history_label);
        maxShiftLabel = findViewById(R.id.max_shift_label);
        alternationIntervalLabel = findViewById(R.id.alternation_interval_label);
        targetWidthLabel = findViewById(R.id.target_width_label);
        settingsPanel = findViewById(R.id.settings_panel);
        adaptiveSettingsGroup = findViewById(R.id.adaptive_settings_group);
        alternationSettingsGroup = findViewById(R.id.alternation_settings_group);
        targetWidthSettingsGroup = findViewById(R.id.target_width_settings_group);

        startButton.setOnClickListener(startOnClickListener);
        showMeButton.setOnClickListener(showMeOnClickListener);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.modes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mode_spinner.setAdapter(adapter);
        mode_spinner.setSelection(settings.getInt("mode", 0));

        ArrayAdapter<CharSequence> strategyAdapter = ArrayAdapter.createFromResource(this,
                R.array.adaptive_strategies, android.R.layout.simple_spinner_item);
        strategyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adaptiveStrategySpinner.setAdapter(strategyAdapter);
        adaptiveStrategySpinner.setSelection(settings.getInt("adaptiveStrategy", 0));

        configureSeekBar(deadzoneSeekBar, "deadzonePercent",
                value -> deadzoneLabel.setText(getString(R.string.deadzone_width_label, value)));
        configureSeekBar(historySeekBar, "historySize",
                value -> historyLabel.setText(getString(R.string.history_samples_label, value + 4)));
        configureSeekBar(maxShiftSeekBar, "maxShiftPercent",
                value -> updateMaxShiftLabel(value + 5));
        configureSeekBar(alternationIntervalSeekBar, "alternationInterval",
                value -> alternationIntervalLabel.setText(
                        getString(R.string.alternation_interval_label, value + 100)));
        configureSeekBar(targetWidthSeekBar, "targetWidth",
                value -> targetWidthLabel.setText(getString(R.string.target_width_label, value + 10)));

        mode_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                settings.edit().putInt("mode", position).apply();
                updateSettingsPanel(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateSettingsPanel(0);
            }
        });
        adaptiveStrategySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                settings.edit().putInt("adaptiveStrategy", position).apply();
                updateSettingsPanel(mode_spinner.getSelectedItemPosition());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateSettingsPanel(mode_spinner.getSelectedItemPosition());
            }
        });
        updateSettingsPanel(mode_spinner.getSelectedItemPosition());
    }

    private void configureSeekBar(SeekBar seekBar, String preference, ProgressCallback callback) {
        seekBar.setProgress(settings.getInt(preference, seekBar.getProgress()));
        callback.onProgressChanged(seekBar.getProgress());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                settings.edit().putInt(preference, progress).apply();
                callback.onProgressChanged(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
    }

    private void updateSettingsPanel(int mode) {
        settingsPanel.setVisibility(mode >= 3 ? View.VISIBLE : View.GONE);
        adaptiveSettingsGroup.setVisibility(mode >= 4 ? View.VISIBLE : View.GONE);
        alternationSettingsGroup.setVisibility(mode >= 4
                && adaptiveStrategySpinner.getSelectedItemPosition() == AdaptiveSplitMapping.RAPID_ALTERNATION
                ? View.VISIBLE : View.GONE);
        targetWidthSettingsGroup.setVisibility(mode == 5 ? View.VISIBLE : View.GONE);
        updateMaxShiftLabel(maxShiftSeekBar.getProgress() + 5);
    }

    private void updateMaxShiftLabel(int value) {
        int label = mode_spinner.getSelectedItemPosition() == 5
                ? R.string.max_target_shift_label : R.string.max_split_shift_label;
        maxShiftLabel.setText(getString(label, value));
    }

    private interface ProgressCallback {
        void onProgressChanged(int progress);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // if MIDI is supported, fill up the spinner, otherwise hide everything and change the text
        if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {

            MidiManager m = (MidiManager)getApplicationContext().getSystemService(Context.MIDI_SERVICE);
            MidiDeviceInfo[] infos = m.getDevices();

            if (infos.length == 0) {
                device_spinner.setEnabled(false);
                String[] data = new String[]{getString(R.string.midi_no_devices)};

                ArrayAdapter<String> adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, data);
                adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                device_spinner.setAdapter(adapter1);
            } else {
                device_spinner.setEnabled(true);
                MidiDeviceSpinnerInfo[] spinfos = new MidiDeviceSpinnerInfo[infos.length];
                int def = 0;
                for (int i = 0; i < infos.length; i++) {
                    MidiDeviceInfo info = infos[i];
                    String manufacturer = info.getProperties().getString("manufacturer");

                    if (manufacturer.equals("Android")) {
                        def = i;
                    }
                    spinfos[i] = new MidiDeviceSpinnerInfo(info);
                }

                ArrayAdapter<MidiDeviceSpinnerInfo> adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinfos);
                adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                device_spinner.setAdapter(adapter1);
                device_spinner.setSelection(def);
            }

        } else {
            TextView text = findViewById(R.id.connect_device_text);
            text.setText(R.string.midi_unsupported_text);
            LinearLayout layout = findViewById(R.id.main_layout);
            layout.setVisibility(View.GONE);
        }
    }

    View.OnClickListener startOnClickListener = v -> {
        if (!device_spinner.isEnabled()) {
            Toast.makeText(getApplicationContext(), getText(R.string.no_devices_cant_start_text), Toast.LENGTH_LONG).show();
            return;
        }

        MidiDeviceSpinnerInfo selected = (MidiDeviceSpinnerInfo)device_spinner.getSelectedItem();
        if (selected == null) {
            Toast.makeText(getApplicationContext(), getText(R.string.device_null_error_text), Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(getApplicationContext(), KeypadActivity.class);
        int selectedMode = mode_spinner.getSelectedItemPosition();
        intent.putExtra("mode", selectedMode);
        intent.putExtra("orientation", landscapeCheckBox.isChecked());
        intent.putExtra("device", selected.getInner());
        intent.putExtra("deadzonePercent", deadzoneSeekBar.getProgress());
        intent.putExtra("adaptiveStrategy", adaptiveStrategySpinner.getSelectedItemPosition());
        intent.putExtra("historySize", historySeekBar.getProgress() + 4);
        intent.putExtra("maxShiftPercent", maxShiftSeekBar.getProgress() + 5);
        intent.putExtra("alternationIntervalMs", alternationIntervalSeekBar.getProgress() + 100);
        intent.putExtra("targetWidthPercent", targetWidthSeekBar.getProgress() + 10);
        startActivity(intent);
    };

    View.OnClickListener showMeOnClickListener = v -> {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/adryzz/midi-keypad/wiki/Enabling-USB-MIDI")));
    };
}

class MidiDeviceSpinnerInfo {
    MidiDeviceInfo inner;
    String name;
    public MidiDeviceSpinnerInfo(MidiDeviceInfo info) {
        this.inner = info;
        this.name = info.getProperties().getString("name");
    }

    public MidiDeviceInfo getInner() {
        return inner;
    }

    @Override
    public String toString() {
        return name;
    }
}