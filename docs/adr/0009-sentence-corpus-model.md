# ADR-0009: Sentences as an entry kind of the generic corpus entry (no separate sentence type)

- **Status:** accepted (the author's decisions 2026-07-18, the model consequences from an Opus sharpening the same day)
- **Date:** 2026-07-18

## Context

The author wants to train whole sentences alongside single words (backlog M2 "whole sentences as a difficulty level", P1/"do it now"). The corpus model (`Word`/`AudioRecording`, `core/corpus`) was effectively cut for single words (`syllableCount`, `phoneticGroup` for minimal pairs), but the class documentation of `Word` was already deliberately kept generic (concept 3.4: later use as a vocabulary trainer). What was open was the model question: a separate type `Sentence` alongside `Word`, or a generic entry with a distinguishing attribute? The author's decisions of 2026-07-18 set the frame: sentences are **treated like words** (the same subjective rating in exam mode, the same SRS logic); the first sentence corpus comes **from TTS** (Piper, ADR-0006), not from own recordings; the content does **not** have to be faithful to any original — the criterion is "readable along and recognisable", short sentences, **no memory training**.

## Decision

1. **One generic corpus entry with an entry kind.** `Word` gets a field `kind: EntryKind { WORD, SENTENCE }` with the kotlinx-serialization default `WORD` — an existing `words.json` without `kind` stays valid, no migration. There is **no** new type `Sentence`.
2. **SRS unchanged.** `ReviewCard.wordId` references any corpus entry (word or sentence); due dates, the rating (five levels, ADR-0005) and the intervals are identical. Seeding (`allOrSeed`) picks sentences up automatically like words.
3. **The TTS pipeline unchanged.** `tools/generate_tts.py` reads `words.json` and voices `text` — sentence entries run through the same Piper pass, no new tooling.
4. **The corpus mode as a plain setting, not a preset.** The choice words/sentences becomes a persisted app setting (`CorpusMode` in `AppSettings`/`SettingsEntity`), deliberately **not** a `SettingsProfile` construct (the presets item stays independent and is thereby decoupled).
5. **Content paraphrased, not quoted.** The first sentence corpus: short sentences (≤ 8 words), thematically leaning on "The Hitchhiker's Guide to the Galaxy" (ch. 1) but freely composed — the criterion is acoustic recognisability when reading along, not fidelity to the work. The origin is documented in the corpus README.

## Alternatives

- **A separate type `Sentence` alongside `Word`:** rejected — it doubles the loading paths, the SRS seeding and the screen logic for behaviour that, per the author's decision, is supposed to be identical; a distinguishing field carries the same information at a fraction of the change surface.
- **Renaming `Word` to `CorpusEntry`:** rejected for now — a cosmetic large diff without a change in behaviour; the class documentation already says "generic corpus entry" and gets extended by the sentence relation. It stays an optional tidying note, not an item.
- **The sentence mode as a preset level (`SettingsProfile` easy/advanced):** rejected — a binary content switch is more honest than a preset construct that does not exist yet, and it stops coupling the sentence arc to the presets item.
- **Own (human) recordings as the first sentence corpus:** postponed by the author — TTS first; the self-recording item stays open for later, and its recording-route question (in-app microphone versus import) is therefore not urgent for now.

## Consequences

- **Easier:** no new model hierarchy; SRS, seeding and the TTS tooling untouched; sentences are usable in both modes after two small batches (model + content, then the mode switch + display); an old `words.json` stays compatible; the copyright question is defused (no verbatim book quotes in the repo).
- **Harder / deliberate debt:** `syllableCount` and `phoneticGroup` are word-specific and carry little meaning for sentences — the convention: `syllableCount` = the sentence's total syllables (which keeps numeric filters usable), `phoneticGroup` = `null`. The type name `Word` becomes semantically imprecise (cushioned by the documentation). The display in learning and exam mode has to be able to wrap across lines (previously single-line shrink-to-fit) — its own implementation batch; the DESIGN.md principle "position-stable" is read as meaning that words stay single-line and large as before and only sentences wrap inside the same stable area.
