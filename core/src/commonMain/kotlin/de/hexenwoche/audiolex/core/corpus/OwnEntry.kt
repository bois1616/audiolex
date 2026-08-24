package de.hexenwoche.audiolex.core.corpus

import kotlinx.serialization.Serializable

/**
 * One self-recorded corpus entry (Backlog Eigen-Korpus Batch B, ADR-0012):
 * metadata for a WAV file the user recorded and transcribed themselves.
 * Kept in a plain JSON file next to the recordings, not in Room (ADR-0012
 * Nachtrag) -- own recordings are the app's only irreplaceable user data,
 * and `createAudioLexDatabase`'s destructive-fallback migration must never
 * be able to touch them.
 *
 * [kind] mirrors [Word.kind] (word vs. sentence, ADR-0009). [speaker] is a
 * free-text voice-pool label (e.g. "männlich", "weiblich", "Dialekt";
 * ADR-0012 Nachtrag) rather than a fixed enum -- which pools exist is
 * discovered by use, not fixed at build time; empty is allowed. [fileName]
 * is derived from [id], never from [text] (Backlog AC2): a later text
 * correction must not rename the file on disk. A WAV file for [fileName]
 * may not exist yet (an entry saved before its first recording) or may have
 * gone missing since (Backlog AC5) -- callers treat both the same way, a
 * quiet "not available" rather than a crash.
 *
 * [language] is the drawer the speaker filed this entry under (ADR-0016), a
 * BCP-47 tag like [CorpusLanguage.languageTag]. A plain string, not the
 * enum: which tags exist is data, and an entry written by a later version
 * that knows more languages must still parse here rather than blow up the
 * whole file (same rationale as
 * [de.hexenwoche.audiolex.core.persistence.SettingsEntity.noiseScenario]).
 * The kotlinx default `"de-DE"` keeps every `eintraege.json` written before
 * this field valid -- and it is the factually right answer for them, since
 * the app could only speak German until v0.34.0.
 */
@Serializable
data class OwnEntry(
    val id: String,
    val text: String,
    val kind: EntryKind = EntryKind.WORD,
    val speaker: String = "",
    val language: String = "de-DE",
    val fileName: String,
    val createdAtEpochMillis: Long,
)
