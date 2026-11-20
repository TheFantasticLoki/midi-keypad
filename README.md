# midi-keypad
An android app that turns an Android device into a MIDI keypad, usable to play games such as [osu!](https://github.com/ppy/osu) or other rhythm games in general.

## Two-key osu! modes

The original two-key modes remain available. The added modes are:

- **Fixed center deadzone:** Configurable gap centered on the split; touches in the gap do not send MIDI notes.
- **Adaptive split:** Learns left/right touch positions over a configurable history and moves the split between them, within a configured limit. An optional rapid-alternation strategy assigns taps outside the moving deadzone to opposite keys; other taps use touch position.
- **Adaptive buttons:** Starts with targets at one quarter and three quarters of the screen width, then moves each target toward recent taps. Target width, history size, maximum movement, deadzone, and optional rapid alternation are configurable.

Deadzone, target width, and movement limits are percentages of screen width, so they scale with device size and orientation. Settings are retained between launches.

The fixed and adaptive modes display a live overlay of their active regions and deadzone. Adaptive overlays update as the split or button targets move.

