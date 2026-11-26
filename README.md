# midi-keypad
An android app that turns an Android device into a MIDI keypad, usable to play games such as [osu!](https://github.com/ppy/osu) or other rhythm games in general.

## Two-key osu! modes

The original two-key modes and static osu!mania 4K mode remain available. The added modes are:

- **Fixed center deadzone:** Configurable gap centered on the split; touches in the gap do not send MIDI notes.
- **Adaptive split:** Learns left/right touch positions over a configurable history and moves the split between them, within a configured limit. An optional rapid-alternation strategy assigns taps outside the moving deadzone to opposite keys; other taps use touch position.
- **Adaptive buttons:** Starts with targets at one quarter and three quarters of the screen width, then moves each target toward recent taps. Target width, history size, maximum movement, deadzone, and optional rapid alternation are configurable.
- **Master mode:** Configure 2–8 keys, choose static or adaptive placement, set the deadzone and target width, assign a MIDI note to each key, and select a shared MIDI channel. Adaptive placement learns screen-wide tap clusters and moves equal-width lanes together while keeping a separate target and acceptance range for each key.

Deadzone, target width, and movement limits are percentages of screen width, so they scale with device size and orientation. Settings are retained between launches.

The fixed and adaptive modes display a live overlay of their active regions and deadzone. Adaptive overlays update as the split or button targets move.

Recognized key presses provide system haptic feedback when enabled on the device. Deadzone touches and key releases do not trigger feedback.

Master mode is selected by default and starts with two keys, adaptive placement, and an 8% screen-width deadzone on MIDI channel 3, using notes 60–61. Adaptive placement uses tap-position clusters from the whole screen, independent of which key each tap initially hits. Once distinct clusters are learned, target centers and the deadzone follow hand movement while lane widths remain equal. Taps in the deadzone do not send MIDI; other taps are accepted within the configured target width plus movement limit. The overlay shows the targets, acceptance ranges, and deadzone. The deadzone is capped as needed to leave a tappable area for every key. Key count, note assignments, channel, placement, target width, history size, movement limit, and deadzone width can all be changed from the main screen. The earlier modes remain available as legacy options.

Adaptive master lanes learn stable tap clusters from all tap locations, not zone-local histories or simultaneous tap pairs. This lets two fingers build separate clusters even when both initially land in the same key. After learning, each target follows its own tap history inside the button; the equal-width buttons move together only when multiple targets show a matching drift and are nearing their movement limits. The deadzone stays centered between learned targets, with a narrow edge margin that also filters taps. Repeated taps in or near the deadzone form a separate cluster that tracks relative to the moving gap without moving either target. The target width and movement limit define the acceptance range, so adjust them to balance forgiving taps with filtering.

