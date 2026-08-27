# ADR-0013: Backing up your own recordings as a ZIP in the documents directory — and turning off the automatic system backup

- **Status:** accepted (the author's decision 2026-08-06)
- **Date:** 2026-08-06

## Context

ADR-0012 explicitly left backing up own recordings open: "a backup is **not** part of this ADR and stays an open question." With batches A–D (v0.21.0–v0.25.0) the own corpus has moved from a raw test to training content. So the stock grows, and it is still **the only body of data in the app that cannot be restored**: SRS due dates reseed themselves, the bundled corpus sits in the repository, settings are set again in a minute. A recording with a person who is not standing next to you right now is gone.

**The triggering finding (2026-08-06):** while checking what a backup without network access could even look like, it turned out that `AndroidManifest.xml` says `android:allowBackup="true"` — without any backup rules. So Android's auto-backup is active: app-private files, that is `files/eigene-aufnahmen/` including the WAVs and the metadata JSON as well as the Room database, are transferred into the user's Google account up to 25 MB when Google backup is enabled. The app needs no `INTERNET` permission for that; a system service does the transfer.

**The consequence:** the sentence in the imprint — "there is still no internet permission, and recordings technically cannot leave the device" — is **currently not accurate**. In fairness: from Android 9 on, the Google backup encrypts with a key derived from the screen lock, so Google itself cannot reach the content. But the data leaves the device, and the user never decided that. The project has so far **proven rather than claimed** its privacy promises (the microphone text was updated in batch A in the same batch as the permission, checked against the manifest) — that standard demands resolving the contradiction rather than explaining it.

## Decision

**1. The backup is explicit and triggered by the user, not automatic.** A **ZIP file** with the entire own corpus (the metadata JSON + all WAVs) gets written into the **documents directory**. What happens to the file afterwards — copying it onto a stick, pulling it to a computer over a cable, uploading it into a cloud manually — is the user's business and explicitly **not** the app's job. The author's decision: "backup as a zip into the documents directory. Further storage can be done manually by the user, e.g. by cloud upload."

**2. The documents directory, not the app storage.** The location is deliberately outside the app-private area: a backup that disappears with the uninstall is not one. On Android from API 29 (minSdk is 29) an app can write its own files there without any permission; the file stays after an uninstall and is visible in the Files app.

**3. `allowBackup` gets turned off — but together with the export, not before it.** After decision 1 the user has a self-determined route; the automatic, unasked upload falls away and the promise in the imprint is true again. **The order is part of the decision:** the system backup is currently the only — if unwanted — safety net. Turning it off before the export exists makes things worse. Both belong in the same version.

**4. No encryption of the ZIP.** The author's assessment: "the data is harmless enough that it does not need extra encryption." These are self-spoken everyday words and sentences for practice. A password prompt would be a hurdle on every export and a loss risk on top (a forgotten password = a lost backup), without meeting a real attack scenario. Where the file ends up therefore stays a real decision of the user — which is the point of the thing.

**5. Restoring is part of it.** A backup that cannot be played back protects nothing — it only creates the feeling of being protected. The import reads a ZIP and **merges**: entries with an unknown id are added, ids already present are skipped. That is possible without conflicts because ids are generated collision-free (`own-<timestamp>-<random>`, ADR-0012); it needs neither a conflict dialog nor overwrite semantics. An import only adds and never deletes — the non-destructive direction is the right rule when there is irrecoverable data on both sides.

**6. Only the own corpus gets backed up.** Not the database. SRS due dates and the session history are history data that rebuild themselves; settings are set again in a minute. A recording and its transcription are not. That keeps the backup small, comprehensible and free of schema questions — a backed-up database would have to be maintained on every future schema jump, a ZIP with WAVs and a JSON file would not.

## Alternatives

- **The Storage Access Framework with a chooser dialog** (`ACTION_CREATE_DOCUMENT`). The user picks the destination on every export — including a cloud provider, without the app knowing. The cleanest separation, but a dialog on every backup. Rejected in favour of the simpler fixed location; for the import, on the other hand, a chooser is unavoidable (the app cannot guess which ZIP is meant).
- **Leaving `allowBackup="true"` and correcting the imprint text instead.** It would be honest and cheaper. Rejected: the project has understood "strictly local" not as a description but as a property enforced by the absence of the network permission. A silent transfer into the Google account contradicts that, encrypted or not.
- **An automatic backup by the app itself** (on every start into the documents directory, say). Rejected: it produces files that grow unnoticed and takes away from the user the decision this ADR explicitly wants to give back.
- **An encrypted ZIP.** See decision 4 — rejected on the author's assessment of the content.
- **Backing up the database as well.** See decision 6 — rejected in favour of leanness and freedom from maintenance.

## Consequences

- **The privacy promise becomes true again, and enforced rather than claimed.** With `allowBackup="false"` and without an `INTERNET` permission, neither the app nor the system can transfer the recordings on its own. The imprint text has to ship with the same version as the change — the same obligation as with the microphone permission in ADR-0012.
- **Responsibility moves to the user, visibly.** Whoever never exports has *less* protection after this change than before (the automatic backup falls away). That is intended and has to be recognisable in the app — a backup nobody knows about is not one.
- **The ZIP sits unencrypted in the documents directory** and is therefore readable by other apps with file access and by anyone at the attached computer. Deliberately accepted (decision 4).
- **The import can break nothing, but it can also repair nothing.** It adds and never overwrites. An id deleted by accident *and* created anew since cannot be brought back that way — a case that practically does not exist, because ids are never reused.
- **A device change becomes possible from here on** without losing recordings: export, take the file along, import on the new device. That was not possible before.

## Addendum 2026-08-07: Decision 6 was half wrong — the session history belongs in the backup

**The trigger:** the device test for AC8 (export, uninstall, reinstall, import) proved the backup — and deleted the author's session history doing so. Only the own corpus was restored, as decision 6 prescribes. The author noticed the loss immediately afterwards ("the session history is reset. Does the backup include it?"). The concrete sessions were dispensable (the author 2026-08-07: "past histories are irrelevant"), the reasoning behind it is not.

**The error:** decision 6 justifies excluding the database with "SRS due dates and the session history are history data that rebuild themselves". That puts two unequal things under one word:

- **SRS due dates really do rebuild themselves.** A few rounds of rating and the state is back. Losing them costs scheduling, not information. For them, decision 6 stays right.
- **The session history does not rebuild itself.** It is the record of what actually happened — when practice took place, how it was rated, how that changed over weeks. In a training whose purpose is a slow neurological change over months, that is precisely the quantity progress can be read from. It is as irrecoverable as a recording, only less conspicuously so: its absence does not hurt immediately, only after a year.

The word "history data" carried the error. It sounds like "temporary" but means, here, "the record of a course of events" — the opposite.

**Corrected decision 6:** what gets backed up is what does not rebuild itself: **the own corpus and the session history**. What does not get backed up are the SRS cards and the settings — both restore themselves through use or in a minute, and both hung on the actual reasoning of decision 6, which stays valid to that extent: keeping the backup small and free of schema questions.

**Do not back up the database, back up the history.** The route stays the same as for the own corpus: a JSON file in the archive (`sitzungen/verlauf.json`), no `.db` file. A backed-up database would have to be maintained on every schema jump and would only be readable with a matching Room version; a list of session records is neither. The archive layout carries it without change — it was deliberately cut with one folder per content type the same day.

**Merging goes by the start time, not by the database id.** `SessionEntity.id` is `autoGenerate` and therefore device-local: on two devices the same number carries different sessions. The identity is `startedAtEpochMillis` — two sessions by the same user do not begin in the same millisecond. So the same rule applies as for recordings: an unknown start time is added, a known one is skipped, and nothing is overwritten or deleted.

**What this says about the process:** the error stood in the ADR from 2026-08-06 and nobody noticed, because the wording sounded plausible. It was only noticed when the test really deleted the data. Part of the lesson is the second half: before the destructive step the own corpus was pulled off the device, the database was not — although it would have been the same gesture. Whoever runs a test that deletes data pulls **everything** beforehand, not what they consider important.

## Addendum 2026-08-07 (the second): Own background noises belong in the backup

**The trigger:** the M4 item "record, import and delete your own background noises" brings the third kind of self-created, irrecoverable content (the author's requirement in the item). ADR-0014 additionally makes it part of the route to publication: a release of the app contains no bundled background noises any more — whatever noises the user has, they recorded or imported themselves. Losing them would not be a nuisance there but the loss of that app section's entire content.

**Corrected decision 6 (second stage):** what gets backed up is what does not rebuild itself — the own corpus, the session history and **own background noises**. The bundled catalogue stays out: it sits in the repository or gets generated at build time (ADR-0014) and is therefore the only noise inventory that can be restored.

**The format — the third folder in the archive:** the layout "one folder per content type" carries the extension without breaking the format: alongside `eigene-aufnahmen/` and `sitzungen/` comes `stoergeraeusche/` with `geraeusche.json` and the WAVs. Purely additive: the format marker stays `format = 1`, and an archive **without** the folder stays valid (every backup created before this change has none; a missing folder → an empty list). The distinction from the first addendum applies accordingly: *missing* noises are normal, *present but damaged* metadata makes the whole archive unreadable — a broken stock must not pass as "0 noises imported".

**Merging as with the recordings:** the identity is the id, generated collision-free; unknown is added, known is skipped, and nothing is overwritten or deleted (decision 5 applies accordingly). If the noise the setting `noiseScenario` selected is missing after an import, the existing fallback resolution from ADR-0010 applies — the import does not have to carry a selection state along.
