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
import android.widget.NumberPicker;
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
    Spinner customLayoutSpinner;
    CheckBox landscapeCheckBox;
    SharedPreferences settings;
    SeekBar deadzoneSeekBar;
    SeekBar historySeekBar;
    SeekBar maxShiftSeekBar;
    SeekBar alternationIntervalSeekBar;
    SeekBar targetWidthSeekBar;
    SeekBar customKeyCountSeekBar;
    NumberPicker customChannelPicker;
    TextView deadzoneLabel;
    TextView historyLabel;
    TextView maxShiftLabel;
    TextView alternationIntervalLabel;
    TextView targetWidthLabel;
    TextView settingsTitle;
    TextView customKeyCountLabel;
    LinearLayout settingsPanel;
    LinearLayout deadzoneSettingsGroup;
    LinearLayout adaptiveSettingsGroup;
    LinearLayout adaptiveStrategySettingsGroup;
    LinearLayout alternationSettingsGroup;
    LinearLayout targetWidthSettingsGroup;
    LinearLayout masterSettingsGroup;
    LinearLayout customSettingsGroup;
    LinearLayout customNoteMappingContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = getSharedPreferences("keypad_settings", MODE_PRIVATE);
        migrateModeSetting();
        startButton = findViewById(R.id.start_button);
        showMeButton = findViewById(R.id.show_me_button);
        mode_spinner = findViewById(R.id.mode_spinner);
        device_spinner = findViewById(R.id.device_spinner);
        landscapeCheckBox = findViewById(R.id.landscape_checkbox);
        adaptiveStrategySpinner = findViewById(R.id.adaptive_strategy_spinner);
        customLayoutSpinner = findViewById(R.id.custom_layout_spinner);
        deadzoneSeekBar = findViewById(R.id.deadzone_seekbar);
        historySeekBar = findViewById(R.id.history_seekbar);
        maxShiftSeekBar = findViewById(R.id.max_shift_seekbar);
        alternationIntervalSeekBar = findViewById(R.id.alternation_interval_seekbar);
        targetWidthSeekBar = findViewById(R.id.target_width_seekbar);
        customKeyCountSeekBar = findViewById(R.id.custom_key_count_seekbar);
        customChannelPicker = findViewById(R.id.custom_channel_picker);
        deadzoneLabel = findViewById(R.id.deadzone_label);
        historyLabel = findViewById(R.id.history_label);
        maxShiftLabel = findViewById(R.id.max_shift_label);
        alternationIntervalLabel = findViewById(R.id.alternation_interval_label);
        targetWidthLabel = findViewById(R.id.target_width_label);
        settingsTitle = findViewById(R.id.settings_title);
        customKeyCountLabel = findViewById(R.id.custom_key_count_label);
        settingsPanel = findViewById(R.id.settings_panel);
        deadzoneSettingsGroup = findViewById(R.id.deadzone_settings_group);
        adaptiveSettingsGroup = findViewById(R.id.adaptive_settings_group);
        adaptiveStrategySettingsGroup = findViewById(R.id.adaptive_strategy_settings_group);
        alternationSettingsGroup = findViewById(R.id.alternation_settings_group);
        targetWidthSettingsGroup = findViewById(R.id.target_width_settings_group);
        masterSettingsGroup = findViewById(R.id.master_settings_group);
        customSettingsGroup = findViewById(R.id.custom_keypad_settings_group);
        customNoteMappingContainer = findViewById(R.id.custom_note_mapping_container);

        startButton.setOnClickListener(startOnClickListener);
        showMeButton.setOnClickListener(showMeOnClickListener);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.modes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mode_spinner.setAdapter(adapter);
        mode_spinner.setSelection(settings.getInt("mode", KeypadModes.MASTER));

        ArrayAdapter<CharSequence> strategyAdapter = ArrayAdapter.createFromResource(this,
                R.array.adaptive_strategies, android.R.layout.simple_spinner_item);
        strategyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adaptiveStrategySpinner.setAdapter(strategyAdapter);
        adaptiveStrategySpinner.setSelection(settings.getInt("adaptiveStrategy", 0));

        ArrayAdapter<CharSequence> customLayoutAdapter = ArrayAdapter.createFromResource(this,
                R.array.custom_layouts, android.R.layout.simple_spinner_item);
        customLayoutAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        customLayoutSpinner.setAdapter(customLayoutAdapter);
        customLayoutSpinner.setSelection(settings.getInt(
                "customLayout", MultiKeyMapping.ADAPTIVE_LANES));

        customChannelPicker.setMinValue(1);
        customChannelPicker.setMaxValue(16);
        customChannelPicker.setWrapSelectorWheel(false);
        customChannelPicker.setValue(settings.getInt("customMidiChannel", 3));
        customChannelPicker.setOnValueChangedListener((picker, oldValue, newValue) ->
                settings.edit().putInt("customMidiChannel", newValue).apply());

        configureDeadzoneSeekBar();
        configureSeekBar(historySeekBar, "historySize",
                value -> historyLabel.setText(getString(R.string.history_samples_label, value + 4)));
        configureSeekBar(maxShiftSeekBar, "maxShiftPercent",
                value -> updateMaxShiftLabel(value + 5));
        configureSeekBar(alternationIntervalSeekBar, "alternationInterval",
                value -> alternationIntervalLabel.setText(
                        getString(R.string.alternation_interval_label, value + 100)));
        migrateTargetWidthSetting();
        configureSeekBar(targetWidthSeekBar, "targetWidth",
                value -> targetWidthLabel.setText(getString(R.string.target_width_label, value + 2)));
        configureSeekBar(customKeyCountSeekBar, "customKeyCountProgress", value -> {
            int keyCount = value + MultiKeyMapping.MIN_KEYS;
            customKeyCountLabel.setText(getString(R.string.custom_key_count_label, keyCount));
            rebuildMidiNotePickers(keyCount);
        });

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
        customLayoutSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                settings.edit().putInt("customLayout", position).apply();
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

    private void migrateModeSetting() {
        if (settings.getInt("modeOrderVersion", 0) >= 1) {
            return;
        }

        SharedPreferences.Editor editor = settings.edit();
        if (settings.contains("mode")) {
            editor.putInt("mode",
                    KeypadModes.fromPreviousModeOrder(settings.getInt("mode", 0)));
        } else {
            editor.putInt("mode", KeypadModes.MASTER);
        }
        editor.putInt("modeOrderVersion", 1).apply();
    }

    private void migrateTargetWidthSetting() {
        if (settings.getBoolean("targetWidthMinimumUpdated", false)) {
            return;
        }

        SharedPreferences.Editor editor = settings.edit();
        if (settings.contains("targetWidth")) {
            editor.putInt("targetWidth", settings.getInt("targetWidth", 0) + 8);
        }
        editor.putBoolean("targetWidthMinimumUpdated", true).apply();
    }

    private void configureDeadzoneSeekBar() {
        deadzoneSeekBar.setProgress(settings.getInt(deadzonePreferenceForCurrentMode(), 8));
        updateDeadzoneLabel(deadzoneSeekBar.getProgress());
        deadzoneSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                settings.edit().putInt(deadzonePreferenceForCurrentMode(), progress).apply();
                updateDeadzoneLabel(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
    }

    private String deadzonePreferenceForCurrentMode() {
        return mode_spinner.getSelectedItemPosition() == KeypadModes.MASTER
                ? "customLaneDeadzonePercent" : "deadzonePercent";
    }

    private void updateSettingsPanel(int mode) {
        boolean masterMode = mode == KeypadModes.MASTER;
        boolean hasDeadzoneSettings = masterMode || mode >= KeypadModes.LEGACY_DEADZONE;
        settingsPanel.setVisibility(hasDeadzoneSettings ? View.VISIBLE : View.GONE);
        boolean standardAdaptive = mode == KeypadModes.LEGACY_ADAPTIVE_SPLIT
                || mode == KeypadModes.LEGACY_ADAPTIVE_TARGETS;
        boolean customAdaptive = masterMode
                && customLayoutSpinner.getSelectedItemPosition() == MultiKeyMapping.ADAPTIVE_LANES;
        deadzoneSettingsGroup.setVisibility(hasDeadzoneSettings ? View.VISIBLE : View.GONE);
        adaptiveSettingsGroup.setVisibility(standardAdaptive || customAdaptive
                ? View.VISIBLE : View.GONE);
        adaptiveStrategySettingsGroup.setVisibility(standardAdaptive ? View.VISIBLE : View.GONE);
        alternationSettingsGroup.setVisibility(standardAdaptive
                && adaptiveStrategySpinner.getSelectedItemPosition() == AdaptiveSplitMapping.RAPID_ALTERNATION
                ? View.VISIBLE : View.GONE);
        targetWidthSettingsGroup.setVisibility(
                mode == KeypadModes.LEGACY_ADAPTIVE_TARGETS ? View.VISIBLE : View.GONE);
        masterSettingsGroup.setVisibility(masterMode ? View.VISIBLE : View.GONE);
        customSettingsGroup.setVisibility(masterMode ? View.VISIBLE : View.GONE);
        settingsTitle.setText(masterMode
                ? R.string.master_settings_title : R.string.two_key_settings_title);
        deadzoneSeekBar.setProgress(settings.getInt(
                masterMode ? "customLaneDeadzonePercent" : "deadzonePercent", 8));
        updateDeadzoneLabel(deadzoneSeekBar.getProgress());
        updateMaxShiftLabel(maxShiftSeekBar.getProgress() + 5);
    }

    private void updateMaxShiftLabel(int value) {
        int mode = mode_spinner.getSelectedItemPosition();
        int label = mode == KeypadModes.LEGACY_ADAPTIVE_TARGETS ? R.string.max_target_shift_label
                : mode == KeypadModes.MASTER ? R.string.max_lane_shift_label
                : R.string.max_split_shift_label;
        maxShiftLabel.setText(getString(label, value));
    }

    private void updateDeadzoneLabel(int value) {
        deadzoneLabel.setText(getString(R.string.deadzone_width_label, value));
    }

    private void rebuildMidiNotePickers(int keyCount) {
        customNoteMappingContainer.removeAllViews();
        for (int i = 0; i < keyCount; i++) {
            final int keyIndex = i;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            TextView label = new TextView(this);
            label.setText(getString(R.string.custom_key_note_label, keyIndex + 1));
            label.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.addView(label, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            NumberPicker notePicker = new NumberPicker(this);
            notePicker.setMinValue(0);
            notePicker.setMaxValue(127);
            notePicker.setWrapSelectorWheel(false);
            notePicker.setValue(settings.getInt("customMidiNote" + keyIndex, 60 + keyIndex));
            notePicker.setOnValueChangedListener((picker, oldValue, newValue) ->
                    settings.edit().putInt("customMidiNote" + keyIndex, newValue).apply());
            row.addView(notePicker, new LinearLayout.LayoutParams(
                    (int) (112 * getResources().getDisplayMetrics().density),
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            customNoteMappingContainer.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }
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
            Object selectedItem = device_spinner.getSelectedItem();
            int previousDeviceId = selectedItem instanceof MidiDeviceSpinnerInfo
                    ? ((MidiDeviceSpinnerInfo) selectedItem).getInner().getId() : -1;

            if (infos.length == 0) {
                device_spinner.setEnabled(false);
                String[] data = new String[]{getString(R.string.midi_no_devices)};

                ArrayAdapter<String> adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, data);
                adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                device_spinner.setAdapter(adapter1);
            } else {
                device_spinner.setEnabled(true);
                MidiDeviceSpinnerInfo[] spinfos = new MidiDeviceSpinnerInfo[infos.length];
                int defaultIndex = 0;
                int previousIndex = -1;
                for (int i = 0; i < infos.length; i++) {
                    MidiDeviceInfo info = infos[i];
                    String manufacturer = info.getProperties().getString("manufacturer");

                    if ("Android".equals(manufacturer)) {
                        defaultIndex = i;
                    }
                    if (info.getId() == previousDeviceId) {
                        previousIndex = i;
                    }
                    spinfos[i] = new MidiDeviceSpinnerInfo(info);
                }

                ArrayAdapter<MidiDeviceSpinnerInfo> adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinfos);
                adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                device_spinner.setAdapter(adapter1);
                device_spinner.setSelection(previousIndex >= 0 ? previousIndex : defaultIndex);
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
        intent.putExtra("targetWidthPercent", targetWidthSeekBar.getProgress() + 2);
        if (selectedMode == KeypadModes.MASTER) {
            int keyCount = customKeyCountSeekBar.getProgress() + MultiKeyMapping.MIN_KEYS;
            int[] notes = new int[keyCount];
            for (int i = 0; i < keyCount; i++) {
                notes[i] = settings.getInt("customMidiNote" + i, 60 + i);
            }
            intent.putExtra("customKeyCount", keyCount);
            intent.putExtra("customNotes", notes);
            intent.putExtra("customLayout", customLayoutSpinner.getSelectedItemPosition());
            intent.putExtra("customDeadzonePercent", deadzoneSeekBar.getProgress());
            intent.putExtra("midiChannel", customChannelPicker.getValue());
        }
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