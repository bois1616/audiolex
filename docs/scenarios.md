# AudioLex scenario catalogue (SDD)

As of 2026-07-08. UI stories are **cut along scenarios** (scenario-driven development), not along entities: "recognise and rate a word in exam mode" is a scenario, "ReviewCard CRUD" is not. When a scenario is implemented it becomes the source of its story's acceptance criteria. The stance behind it: `SOUL.md` · the shape: `DESIGN.md` · the technique: `docs/architecture.md`.

Format:

```text
As someone training I want to [in state S] [achieve intent],
so that [observable result].
```

A scenario is path-oriented (one way through the app), stateful (tied to a domain or technical state) and observable (it ends in something visible or audible, not in an internal state). There is only one role: the person training.

## Mandatory check per UI story

Before every training UI story, check which of these path classes apply — the ones that do become acceptance criteria, the ones that do not are explicitly justified:

| Path class | Guiding question |
| --- | --- |
| **Happy path** | The successful main course of the intent. |
| **Empty state** | Nothing due / corpus empty — what does the user see, which action remains? |
| **Audio output disturbed** | Sink fails, hearing aid disconnected, level muted — a message rather than a silent failure. |
| **Error state** | File missing or corrupt, persistence error — a message rather than a crash. |
| **Interruption** | Leaving the app, or a phone call mid-session — a defined way back in. |

**Structurally not applicable** (dropped without individual justification for as long as phase 1 holds): *offline/degraded* (the app is entirely local, no backend), *missing permission* (single user, no roles), *concurrency* (one user, one device). To be revisited if cloud sync or multiple users ever arrive (concept 4.5).

## Catalogue

### Training

- **S1 · Work through a learning session** (M2, happy path)
  As someone training I want to start a session in learning mode and hear and read each word at the same time, so that the association sound → written form builds up and I see at the end how many words I went through.

- **S2 · Listen to a word again** (M2)
  As someone training I want to replay a word I have not understood yet as often as I like before moving on, so that I set the pace myself. (Configurable number of repetitions before identification: concept 3.3.)

- **S3 · Recognise and rate a word in exam mode** (M3, happy path)
  As someone training I want to only hear a word when cards are due, check my guess against the revealed card and rate it on five levels, so that the next due date is set visibly and the next word follows.

- **S4 · Nothing due** (M3, empty state)
  As someone training I want to see, when the review queue is empty, when the next card is due and what I can do instead (learning mode), so that the app never ends in a dead end.

- **S5 · Leave a session** (M2/M3, interruption)
  As someone training I want to be able to leave a session at any time (a phone call, everyday life), so that it **ends cleanly**: ratings already given are persisted, the session is logged as finished, and there is no pause/resume state — the next start is a new session. (Author's decision 2026-07-08.)

### Audio setup

- **S6 · Reach the trained ear** (M1→M2, the core concern)
  As someone training I want the training to arrive intelligibly and at the right level in the reference setup (Bluetooth hearing aid, left ear — ADR-0007), so that I can concentrate on recognition rather than on the technology. The channel choice left/right/both (`StereoGain`) remains an option for alternative setups (wired headphones), but it has no effect over Bluetooth (summed to mono) — the UI must not present it as effective there (M4).

- **S7 · No audible output** (M1→M2, audio disturbed)
  As someone training I want an understandable message with a next step when playback fails or stays silent (sink error, hearing aid disconnected, device volume), so that I am not left guessing whether the app, the device or my ear is the cause. (Real experience from M1: "no sound" turned out to be a separate Bluetooth volume setting.)

- **S8 · Raise the difficulty with background noise** (M4)
  As someone training I want to switch on a noise scenario (pub/weather/traffic) with an adjustable SNR, so that the training comes closer to real listening situations and the setting is traceable in the session log.

- **S9 · Pick a preset** (M4)
  As someone training I want to activate a preset (easy/hard/advanced) with a single tap instead of adjusting individual parameters, so that the session starts with coherent settings right away.

### Frame

- **S10 · First start** (M2, empty/initial state)
  As someone training I want to reach a working training state on first start without any setup (corpus loaded, sensible defaults for channel and level), so that the first session is possible without a configuration hurdle.

- **S11 · Narrow the word selection** (M5)
  As someone training I want to filter the word pool by syllable count, category and phonetic similarity (minimal pairs), so that I train deliberately at my current threshold.

- **S12 · Review session history** (M3)
  As someone training I want to see finished sessions as a list with date and time (several per day too), each with its mode and figures (words, distribution of ratings), so that I can follow my own training over time. (Author's decision 2026-07-08; the domain basis is the `Session` entity with `parameterSnapshot` and `results[]`.)

## Scenario questions that have been settled

The author decided all originally open questions (S-OPEN-1…3) on 2026-07-08:

| Question | Decision |
| --- | --- |
| Reference training setup | Bluetooth hearing aid, left ear; playback device is a smartphone, not tied to the A53 — **ADR-0007**. Channel separation becomes an option rather than the core (→ S6). |
| Progress/statistics | Session-based: a list with date and time, several sessions per day possible (→ S12). |
| Behaviour on interruption | End cleanly, no pause/resume (→ S5). |
