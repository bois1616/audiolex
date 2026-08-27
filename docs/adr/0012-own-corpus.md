# ADR-0012: The own corpus — self-recorded entries as a second, writable source

- **Status:** accepted (the author's decisions 2026-08-06, the architecture from an Opus sharpening the same day) · **decisions 1 and 2 revised on 2026-08-06 before implementation, see the addendum**
- **Date:** 2026-08-06

> **Addendum 2026-08-06: the metadata sits as JSON next to the recordings, not in the database.**
>
> Originally the metadata (text, entry kind, file name) was intended for a Room table — consistent with the rest of the persistence layout. While sharpening batch B it turned out that this collides with an existing decision: `createAudioLexDatabase` sets `fallbackToDestructiveMigration(true)`, so **every** schema change deletes all tables. That had been without consequence so far, because only SRS due dates and settings were affected — both regenerable or trivial to set again. Own recordings are not: they are the only body of data in the app that is **not restorable**. A version jump would have left the WAV files as orphans — present, but worthless without their text and entry kind. The check was triggered by the author's report that a second person had already recorded: the stock is no longer hypothetical.
>
> **The revised decision (author 2026-08-06):** the text, entry kind, speaker contingent and timestamp sit in **one JSON file in the same directory as the WAVs** — the same pattern as the bundled corpus (`words.json` + `recordings.json` + WAVs). Room stays responsible for SRS, sessions and settings and keeps its destructive fallback there; the own corpus is simply unaffected by database jumps, because it does not live in the database.
>
> What that additionally solves: **backing up** is copying a folder (the question left open under "consequences"), and the stock is **self-describing** — with a wrong transcription, a text editor suffices in an emergency. For a system whose explicit weak point is the transcription (point 5) that is not a side effect but a safety net.
>
> The price: no transactions and no query optimiser. With a single-user stock in the order of ten to a few hundred entries, which is loaded into memory in full anyway, both are moot. The file is rewritten completely on every change.
>
> **Addendum 2026-08-06: speaker contingents.** The author had a second person (female) record — with a good result — and drew two conclusions from it: the question of a **second voice**, open since July (M1, where TTS had failed), is thereby solved without TTS, and the **dialect** from M5 is most easily recorded the same way. Named contingents are therefore planned (male, female, dialect, say) that can be filtered by later. Implemented as a **free text field** per entry rather than a fixed enum: nobody knows today which contingents will come into being, and an enum would have to be changed for every new one. Multilingualism could be represented the same way — the author explicitly defers that, and the existing `Word.language` field stays the intended place for it (the language arc).

## Context

The author wants to record his own words and sentences and assign the text manually — "every user can record and qualify their own sentences/words" (requirement 2026-08-06). The value lies less in quantity than in the **variety of voices**: the corpus today consists of 58 entries in a single synthetic voice (thorsten-medium), and the attempt to add a second TTS voice failed on quality in July (kerstin-low, backlog M1). Human recordings solve that without a detour.

The data model has carried this since M1: `AudioRecording` is separate from `Word` and allows several speakers per entry (`voiceId`); ADR-0009 made sentences regular corpus entries. **What is new is not the model but the path the data takes.** Three things are missing entirely:

1. **Microphone input.** There is only output (`AudioSink` as expect/actual, Android `AudioTrack`, desktop `paplay`). A counterpart does not exist.
2. **A writable corpus.** `loadCorpus` reads exclusively from the Compose resources — those are packed at build time and immutable at runtime. Own recordings cannot land there in principle.
3. **A permission.** The app requests **not a single one** today, and the shipped privacy page (v0.16.0) promises exactly that.

The author's decisions that set the frame (2026-08-06): **recording directly in the app** (not importing from outside), and the own recordings form a **separate area next to the bundled corpus** — own entries with their own text, not additional voices for existing corpus words.

Plus an explicit statement by the author that shapes this decision: "This is a garbage-in, garbage-out system. That is, if I do not transcribe the recorded words or sentences correctly, I get wrong results too."

## Decision

**1. The own corpus is a second source, not an extension of the first.** The metadata (text, entry kind, timestamp, file name) goes into a new Room table, and the audio data as WAV files into an app-private directory. The bundled corpus in the Compose resources stays untouched and read-only. `LoadedCorpus` gets both sources; the training screens choose what they work with, as with the words/sentences switch.

**2. Audio belongs in the file system, not in the database.** Room only holds the file name. PCM data as a BLOB would inflate the database by orders of magnitude and burden every read without offering an advantage. The platform-dependent path comes from the entry point, as it already does for the database (ADR-0004, `DatabaseBuilder.android.kt`/`.desktop.kt`) — `:core` stays context-free.

**3. The microphone input mirrors the `AudioSink`.** An `AudioSource` as expect/actual in `:core` (Android `AudioRecord`, desktop `javax.sound` `TargetDataLine`). Recording happens in **PCM16, mono, 22050 Hz** — the same format as the TTS corpus. That is not a matter of style: `mixWithNoise` requires the same sample rate and channel count, and ADR-0010 rules out resampling in code. A recording in a different format could not be combined with background noise. The existing WAV writer moves from `jvmMain` to `commonMain` for this.

**4. `RECORD_AUDIO` becomes the only permission — and the privacy text is corrected in the same go.** Today's promise "requests not a single Android permission" becomes untrue and has to be adjusted. What **stays true** and should be said more clearly: there is still no `INTERNET` permission, and recordings technically cannot leave the device.

**5. "Garbage in, garbage out" is named as a system boundary rather than engineered away.** The mapping sound → text is and stays manual and lies with the user; an automatic cross-check is not planned (see alternatives). Three requirements follow from that, which limit the damage without shifting the responsibility:

- **Listening before saving.** After recording, the sound and the text entered are accessible at the same time before the entry comes into being. The most likely error — a typo, a mishearing, the wrong word recorded — shows up exactly there.
- **Correctable afterwards.** The text has to be changeable without re-recording, the recording repeatable without losing the text, and the entry deletable. A typo must not make an entry worthless.
- **Errors stay contained.** Because the own corpus is a separate source (decision 1), a wrong entry does not contaminate the curated TTS corpus.

**6. Own entries take part in both training modes**, with the same SRS mechanics as bundled ones (cards through `allOrSeed`). Exam mode is where a wrong transcription is most expensive — there the wrong association gets practised **and** written into due dates. That is the price of own recordings being full members; the correctability from point 5 is the countermeasure.

## Alternatives

- **Importing finished audio files instead of recording in the app.** No microphone code, no permission — but a media break: record externally, transfer, assign. Rejected by the author on 2026-08-06 in favour of direct recording.
- **Own recordings as an additional `voiceId` for existing corpus words.** The model would carry it, and it would bring variety of voices for the same word. Rejected by the author: he wants his own content, not variants of prescribed content. A side effect of that refusal: the question "which of several recordings gets played?" does not even arise.
- **Writing own entries into `words.json`.** Technically impossible for a packed resource and conceptually wrong — it would mix the curated with the self-created in a file under version control.
- **An automatic cross-check by speech recognition.** It would actually catch "garbage in". Rejected: cloud recognition is out of the question (no network permission, the "strictly local" ADR line), and offline recognition would be a heavyweight new dependency for a secondary problem. It stays conceivable as a `[PROP]` should wrong transcriptions turn out to be a real nuisance in practice.
- **Audio as a BLOB in Room.** Rejected, see decision 2.

## Consequences

- **The app requests a permission for the first time.** That is a visible turning point for a project whose privacy promise rested on "none at all". The imprint text has to ship with the same version as the feature, or an untrue promise sits inside the app.
- **The corpus becomes two-sourced.** `CorpusLoader` and the screens have to merge two origins from now on. The language arc batch B adds a language filter at the same place — both undertakings touch `loadCorpus` and should not run at the same time.
- **User data comes into being that does not live in the repo.** Unlike the gitignored corpus WAVs, own recordings are irrecoverable if the device is lost or the app uninstalled. A backup is **not** part of this ADR and stays an open question. — **Answered on 2026-08-06 by ADR-0013:** an export as a ZIP into the documents directory, triggered by the user, plus an import to restore. It turned out along the way that the recordings were already travelling into the Google account unasked through Android's `allowBackup` — which contradicted this app's privacy promise and gets turned off with the same version.
- **From here on, the quality of the training also depends on care while recording.** That is intended, but it shifts a source of error from the software to the user — and errors of this kind only show up in exam mode, where they do the most damage.
- **The format is fixed, not negotiable.** 22050 Hz mono PCM16 is the condition for own recordings staying combinable with the background-noise overlay. If `AudioRecord` does not deliver that rate on a device, that is a real problem and not a detail. **Settled on the device (A53 listening check 2026-08-06, v0.21.0):** the sharp format check did not trip, and the recording is intelligible and undistorted — the A53 delivers the required format. The reservation is thereby cleared for the reference device; the check stays in the code as protection for other devices.

## Addendum 2026-08-06: The source split is replaced by contingents

Decision 1 and point 5 (third bullet) describe the own corpus as a **second source** next to the bundled one. Batch C implemented it that way: a setting `CorpusSource { MITGELIEFERT, EIGENE, BEIDE }`. Batch D takes that back — on the author's objection, and he is right.

**The finding:** the bundled corpus has exactly **one** voice (`thorsten`, all 68 recordings). A choice of "bundled / own / both" is therefore nothing other than a speaker selection with hard-wired groups. Two settings answer the same question — "what am I training from today?" — and the seam between them gets more uncomfortable with every additional speaker, not more harmless. The author on 2026-08-06: "the bundled corpus really does not need to be singled out from the others. The distinction own/bundled can really be reduced to a corpus selection."

**The decision:** `CorpusSource` goes away without replacement. In its place comes a multiple selection over the speaker contingents that actually exist, `thorsten` among them — without special status. "Bundled" is then `thorsten`, "both" is everything, "own only" is everything except `thorsten`. That last line is the reason for a **multiple** selection: with a single selection, "all own ones" could not be expressed.

**What gets stored is the deselection, not the selection.** That makes "all" the default without any names having to be stored anywhere, and a newly appearing contingent is automatically included instead of staying mute until someone ticks it. That was the only real loss against the open category "own" — and this storage direction removes it. It became necessary anyway through the "deselect all" button the author wanted: with a stored selection, "empty" would have to mean "everything" at the same time, and the button would visibly do nothing.

**Point 5, third bullet, still applies — more precisely, even.** The damage limitation never hung on the two-way split but on the unchecked being separable from the curated. Through contingents that works **more finely**: a single sloppily recorded speaker can be isolated instead of switching off "all own ones" wholesale. The curated corpus is still not contaminated — it is simply no longer a special case but the contingent `thorsten`.

**What the addendum brings and what it triggers:**

- An **index per contingent** (the number of trainable words and sentences) makes visible before the selection what a run will contain. A contingent without trainable entries cannot appear in the list in the first place, because the list is derived from the entries.
- The speaker field turns from **a label into structure**. That makes a typo expensive: from here on, "Stephan" and "Stepahn" stand side by side as two contingents permanently, and batch B **cannot** change an entry's speaker (only the text, the recording, deletion). Batch D prevents this at the root by having the recording mask suggest existing names; the cure (renaming, moving a single entry, deleting a contingent) is a backlog item of its own. The author's remark that led to it: "the only problem I see is in managing several corpora, changing them, extending them."
- The rollback costs a **table rebuild** in the migration (SQLite cannot drop columns) — the first real stress test of the migration infrastructure from batch C, and the reason to check it against the database again rather than on screen.
