# midi-keypad
An android app that turns an Android device into a MIDI keypad, usable to play games such as [osu!](https://github.com/ppy/osu) or other rhythm games in general.

## Two-key osu! modes

The original two-key modes and static osu!mania 4K mode remain available. The added modes are:

- **Fixed center deadzone:** Configurable gap centered on the split; touches in the gap do not send MIDI notes.
- **Adaptive split:** Learns left/right touch positions over a configurable history and moves the split between them, within a configured limit. An optional rapid-alternation strategy assigns taps outside the moving deadzone to opposite keys; other taps use touch position.
- **Adaptive buttons:** Starts with targets at one quarter and three quarters of the screen width, then moves each target toward recent taps. Target width, history size, maximum movement, deadzone, and optional rapid alternation are configurable.
- **Custom MIDI keypad:** Configure 2–10 keys, choose equal-width static lanes or adaptive lane centers, assign a MIDI note to each key, and select a shared MIDI channel.

Deadzone, target width, and movement limits are percentages of screen width, so they scale with device size and orientation. Settings are retained between launches.

The fixed and adaptive modes display a live overlay of their active regions and deadzone. Adaptive overlays update as the split or button targets move.

The custom keypad defaults to four keys on MIDI channel 3, using notes 60–63. In adaptive placement, each lane learns its own recent touch position while keeping the lanes ordered; the gaps follow the moving lane centers. Key count, note assignments, channel, placement, history size, movement limit, and lane gap can all be changed from the main screen.

