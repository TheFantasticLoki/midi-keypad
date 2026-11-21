# midi-keypad
An android app that turns an Android device into a MIDI keypad, usable to play games such as [osu!](https://github.com/ppy/osu) or other rhythm games in general.

## Two-key osu! modes

The original two-key modes and static osu!mania 4K mode remain available. The added modes are:

- **Fixed center deadzone:** Configurable gap centered on the split; touches in the gap do not send MIDI notes.
- **Adaptive split:** Learns left/right touch positions over a configurable history and moves the split between them, within a configured limit. An optional rapid-alternation strategy assigns taps outside the moving deadzone to opposite keys; other taps use touch position.
- **Adaptive buttons:** Starts with targets at one quarter and three quarters of the screen width, then moves each target toward recent taps. Target width, history size, maximum movement, deadzone, and optional rapid alternation are configurable.
- **Master mode:** Configure 2–8 keys, choose static or dynamic lane centers, set the deadzone, assign a MIDI note to each key, and select a shared MIDI channel. Dynamic placement learns each lane's recent touch position.

Deadzone, target width, and movement limits are percentages of screen width, so they scale with device size and orientation. Settings are retained between launches.

The fixed and adaptive modes display a live overlay of their active regions and deadzone. Adaptive overlays update as the split or button targets move.

Recognized key presses provide system haptic feedback when enabled on the device. Deadzone touches and key releases do not trigger feedback.

Master mode is selected by default and starts with two keys, dynamic placement, and an 8% screen-width deadzone on MIDI channel 3, using notes 60–61. Each dynamic lane learns its recent touch position while keeping the lanes ordered; deadzone touches still train the nearest lane but do not send MIDI. The overlay displays learned center positions to help diagnose adaptation. The deadzone is capped as needed to leave a tappable area for every key. Key count, note assignments, channel, placement, history size, movement limit, and deadzone width can all be changed from the main screen. The earlier modes remain available as legacy options.

