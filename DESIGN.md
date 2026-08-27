# DESIGN — UI/UX concept

This document describes the shape of the app: screens, navigation, visual principles. The technical architecture lives in `docs/architecture.md` and `docs/adr/` and is not duplicated here. UI stories are cut along the scenario catalogue in `docs/scenarios.md` (SDD); the stance behind every text and decision is in `SOUL.md`.

## Guiding principles

1. **Audio first.** The ear is the main channel, the eye is secondary. During playback the screen shows little — nothing may compete with the auditory stimulus.
2. **One action per step.** In training there is exactly one expected action at any moment: listen → (reveal) → rate. No parallel decisions, no menus inside the training flow.
3. **The output path is always visible.** The reference setup is the Bluetooth hearing aid on the left ear (ADR-0007): during training it is readable which level is being trained with; a channel selection (l/r/both, effective only on alternative setups) is never presented as effective on Bluetooth output.
4. **Big targets, one hand.** The app is operated one-handed on a phone; rating buttons and the reveal target sit in the lower half, within thumb reach.
5. **Calm over stimulus.** No animation without a function, no confetti, no badges. The one staged moment is revealing the card.
6. **Trainable immediately.** From app start to the first word: two taps at most — the last preset and channel setup are remembered.

## Screen structure (target picture)

```text
Start           → Cards due today, mode choice (learning/exam), preset shortcut
                  quiet zone at the bottom: language picker, quick guide, imprint, version
Learning mode   → hear the word + see the text; Repeat / Next                (M2)
Exam mode       → covered card → Reveal → 5 rating buttons                   (M3)
Settings        → channel/level, background noise/SNR (M4), presets, word filter (M5)
Statistics      → session list with date/time, figures per session           (M3, S12)
Quick guide     → the two modes, the rating scale, the settings explained    (ADR-0015)
```

Navigation stays flat: Start is the hub, training screens are dead ends with "Back" returning to the hub. Two levels at most.

## The training screens in detail

**Learning mode (M2):** the target word large and calm in the centre of the screen, always in the same place — the written form is half the association. Below it: Repeat (play the word again) and Next. Progress stays discreet ("7 / 18"), channel badge along the top edge.

**Exam mode (M3):** covered card at a constant size — the silhouette must not give away the word's length. Revealing happens through a large tap target (the card itself). After that, five rating buttons with their interval (labels in the selected UI language, ADR-0015):

```text
[ Sofort ]  [ Bald ]  [ Später ]  [ Gut ]   [ Perfekt ]     (de)
   1 min      10 min     1 Tag     1 Woche    1 Monat
[ Again  ]  [ Soon ]  [ Later  ]  [ Good ]  [ Perfect ]     (en)
   1 min      10 min     1 day     1 week     1 month
```

The buttons are designed as equals (no traffic-light red→green): the scale steers repetition, it does not grade (SOUL.md).

## Visual principles

- **Light/dark follows the system**, dark mode designed as an equal — training happens in the evening too.
- **High contrast, large typography** for the target word; secondary things (progress, badges) recede clearly.
- **Colour carries meaning, not decoration**: channel marking and status messages may use colour; there are no decorative colours.
- **UI texts in German and English** (ADR-0015), tone per SOUL.md — the same in both languages: terse and matter-of-fact. The language picker sits on the start screen, not in the settings: someone who cannot read the current language must not have to guess which button opens the settings. It sits in the quiet zone at the bottom, each language written in itself ("Deutsch", "English"), the active one in the accent colour. The **corpus** language is a separate setting of its own (ADR-0016) — reading the app and training in a language are two different questions.

## Components (target picture)

| Component | Purpose |
| --- | --- |
| `WordCard` | target word, large, position-stable (learning mode) |
| `RevealCard` | covered card of constant size, tap target to reveal (exam mode) |
| `RatingBar` | 5 equal rating buttons with their intervals |
| `ChannelBadge` | output path + level, always visible during training; channel selection shown as effective only on an alternative setup (ADR-0007) |
| `SessionProgress` | discreet progress ("7 / 18") |
| `NoiseControl` | noise scenario choice + SNR slider (M4) |

## Decided

- Stack: Compose Multiplatform, desktop as the dev target (ADR-0001) — no UI library on top of that.
- The channel-test UI reachable from the version line stays as the instrument for channel work; it is not part of the regular flow.
- Reference training setup: Bluetooth hearing aid, left ear; playback device is a smartphone, not tied to the A53 (ADR-0007). Channel separation is a setup option, not the core.
- Statistics are session-based: a list of finished sessions with date and time, several per day (scenario S12).
- Sessions end cleanly instead of pause/resume (scenario S5) — which fits the dead-end navigation with "Back".
- No screen holds content at the bottom edge with `Modifier.weight(1f)` alone: when the height runs out, content is clipped rather than scrolled. Since v0.34.0 the start screen combines `heightIn(min = viewport)` with `Arrangement.SpaceBetween` inside a scrolling column — anchored at the bottom while there is room, scrolling as soon as there is not. Same class of bug as the "Back" button that was unreachable three times.

## Still open

- [ ] Suggest a rating automatically (reaction time)? Backlog [P3] [PROP] — would extend `RatingBar` with a suggestion state.
- [ ] The concrete shape of the Bluetooth notice when a channel selection ≠ "both" is active over Bluetooth (M4 settings).
