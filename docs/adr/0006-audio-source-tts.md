# ADR-0006: Audio source for the MVP — local TTS (Piper), dialects as a prepared data field

- **Status:** accepted
- **Date:** 2026-07-07

## Context

Concept section 8.1 left the audio source for the word corpus open: own recordings (several speakers, natural) versus a TTS start for a fast prototype. For M1 there is the additional wish for several voice types (male/female) and — as an advanced difficulty level — for dialects.

## Decision

1. **TTS as the starting source**, not own recordings: a fast corpus build, and no recording setup needed to begin M1.
2. **Local/offline (Piper)** instead of cloud TTS: it fits the strict locality (concept 4.5, AGENTS.md §5 "no network or cloud code"). The audio is generated once by a script and stored as WAV — the app itself needs no TTS engine and no network connection at runtime.
3. **M1 starts with one voice (`de_DE-thorsten-medium`, male, standard German)** instead of the two originally planned. `de_DE-kerstin-low` (female) was tested as well: with isolated single words (no sentence context) kerstin spoke measurably too fast and compressed — "Ball" on its own took 0.24 s against 0.52 s with thorsten-medium on identical text, while a full test sentence in the same voice was normal and clearly intelligible. A workaround (embedding the word in a carrier sentence and trimming it back) was tried but produced no reliable word boundaries across all the test words and was rejected. For German single female speakers Piper only offers the quality tiers `low`/`x_low` (kerstin, ramona, eva_k) — presumably the cause of the effect, since the only `medium` model (thorsten) does not show the problem. A second voice stays an open backlog point (M1) until a better quality tier is available or another source is used (a different TTS engine, a real recording).
4. **Dialects are prepared in the data model but not filled in M1**: `AudioRecording` gets a `locale` field (BCP-47 with a region tag, e.g. `de-DE`, `de-AT`, `de-CH`, and in the longer run `de-DE-bar` for Bavarian or similar) instead of a plain `speaker` string. Piper offers no dependable German dialect voices — filling the advanced mode with real dialects moves to a later phase (own recordings or a TTS source still to be found), but it does not block M1.

## Alternatives

- **Cloud TTS (Azure/Google) for a wider dialect choice:** it breaks the locality requirement while building the corpus (an API key, costs, a network connection at generation time) and was rejected for the MVP. It stays conceivable as a `[PROP]` should dialect voices later be sourced from a cloud service after all.
- **Looking for dialect-capable local TTS straight away:** it would have delayed M1 without any good German dialect TTS being in sight. Rejected in favour of "prepare the field, fill it later".
- **Own recordings right away:** they deliver the best quality and dialect fidelity, but the recording logistics (speakers, equipment) are too slow for M1. It stays the goal for later (concept 4.3: "priority on real speech recordings").

## Consequences

- `AudioRecording.locale` replaces `speaker` as free text; the speaker's identity becomes a field of its own (`voiceId`), so that voice type and dialect/region can be filtered independently (learning mode: a neutral standard-German voice; advanced: a dialect pool on purpose).
- `tools/generate_tts.py` produces the WAV files once, offline; Piper itself is not a runtime dependency of the app.
- Should a usable dialect TTS be found later, or real dialect recordings come into being, only `corpus-data` has to be filled — no model or architecture change needed.
- The test corpus is single-voiced for the time being (thorsten only). That is uncritical for M1 (the WAV loader, the sinks, channel control), since those building blocks are voice-independent; adding a second voice later changes only `corpus-data`, no code.
- The sample rate of 22050 Hz (thorsten-medium) is now uniform across the corpus — the initially feared problem of different rates per voice (kerstin-low ran at 16000 Hz) does not arise for now.
