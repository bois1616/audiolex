package de.hexenwoche.audiolex

import de.hexenwoche.audiolex.core.corpus.AudioRecording
import de.hexenwoche.audiolex.core.corpus.CorpusLanguage
import de.hexenwoche.audiolex.core.corpus.EntryKind
import de.hexenwoche.audiolex.core.corpus.LoadedCorpus
import de.hexenwoche.audiolex.core.corpus.RecordingSource
import de.hexenwoche.audiolex.core.corpus.mergeCorpus
import de.hexenwoche.audiolex.core.corpus.parseCorpus
import de.hexenwoche.audiolex.core.i18n.Strings
import de.hexenwoche.audiolex.generated.resources.Res

/**
 * The one corpus loading site for all screens (Backlog "Code-Qualität":
 * three nearly identical load/parse blocks used to live in Lernmodus,
 * Prüfmodus and the dev channel test). Reading the Compose resources is the
 * only platform-bound part -- parsing/filtering/indexing is [parseCorpus]
 * in `:core`, which stays jvm-testable that way (same split as
 * `NoiseMixing.kt`: composeApp reads resources, core does the logic).
 *
 * [kind] filters entries by their [EntryKind]; null loads everything
 * unfiltered (the dev channel test lists all entries, and the speaker index
 * needs both kinds' counts, Backlog Eigen-Korpus Batch D AC4). Every screen
 * entry loads fresh -- no caching across screens, at this corpus size
 * reloading two small JSON files is trivial.
 *
 * Both sides are always read now (Backlog Eigen-Korpus Batch D, ADR-0012
 * Nachtrag "Die Quellentrennung wird durch Kontingente ersetzt"): the Batch C
 * `CorpusSource` switch that used to skip a side entirely is gone, replaced
 * by the [excludedSpeakers] contingent filter that [mergeCorpus] applies
 * *after* merging -- reading two small JSON files unconditionally was
 * already the accepted cost above, this just makes it the only path.
 * [ownCorpusRepository] stays nullable and defaults to null so the dev
 * channel test (which never passes it) keeps loading exactly the
 * mitgeliefert corpus it always has, unaffected by this batch.
 */
suspend fun loadCorpus(
    kind: EntryKind? = null,
    ownCorpusRepository: OwnCorpusRepository? = null,
    excludedSpeakers: Set<String> = emptySet(),
    language: CorpusLanguage? = null,
): LoadedCorpus {
    val builtIn = parseCorpus(
        wordsJson = Res.readBytes("files/corpus/words.json").decodeToString(),
        recordingsJson = Res.readBytes("files/corpus/recordings.json").decodeToString(),
        kind = null,
    )
    val ownEntries = ownCorpusRepository?.trainable() ?: emptyList()
    return mergeCorpus(builtIn.words, builtIn.recordings, ownEntries, kind, excludedSpeakers, language)
}

/**
 * Reads a recording's raw WAV bytes according to [AudioRecording.source]
 * (Backlog Eigen-Korpus Batch C, AC5) -- the one place all three training/
 * dev screens now read audio from, replacing three near-identical
 * `Res.readBytes("files/corpus/…")` call sites. [RecordingSource.MITGELIEFERT]
 * reads the packed Compose resource exactly as before; [RecordingSource.EIGEN]
 * reads via [OwnCorpusRepository]. Own recordings are PCM16 mono 22050 Hz,
 * identical to the built-in corpus (Batch A, ADR-0012 point 3), so nothing
 * downstream (noise mix, channel selection) needs to treat the two sources
 * differently once the bytes are in hand.
 *
 * A missing own recording (deleted between corpus load and playback, AC5)
 * throws rather than returning null: [de.hexenwoche.audiolex.core.session.PlaybackQueue]'s
 * producer already turns any exception from its `produce()` lambda into the
 * existing calm onError path ([Strings.playbackFailed]), the same
 * outcome a missing built-in resource would already hit today -- no second
 * failure mechanism needed for the second source.
 */
suspend fun readRecordingBytes(
    recording: AudioRecording,
    ownCorpusRepository: OwnCorpusRepository,
    strings: Strings,
): ByteArray =
    when (recording.source) {
        RecordingSource.MITGELIEFERT -> Res.readBytes("files/corpus/${recording.fileRef}")
        RecordingSource.EIGEN -> ownCorpusRepository.recordingBytes(recording.fileRef)
            ?: error(strings.recordingFileMissing(recording.fileRef))
    }

/**
 * Explains *why* the corpus came back empty (Backlog Eigen-Korpus Batch D,
 * AC6, extends Batch C AC7). `CorpusSource` is gone, so this no longer keys
 * off a fixed enum -- [excludedSpeakers] is the persisted exclusion,
 * [availableSpeakers] is every [AudioRecording.voiceId] the *unfiltered*
 * corpus currently has to offer (the same set [de.hexenwoche.audiolex.core.corpus.speakerContingents]
 * is derived from). Comparing the two tells apart the three causes a caller
 * only ever reaches this for once its own [LoadedCorpus] came back empty:
 *
 * - **Nothing selected**: [availableSpeakers] isn't empty, but every one of
 *   them is in [excludedSpeakers] -- the training screens have nothing to
 *   draw from because every contingent was deselected, not because the
 *   corpus itself is short of entries.
 * - **Selected, but not for this Wörter/Sätze setting**: an exclusion is
 *   active and not blanket (at least one contingent remains selected), yet
 *   still nothing came through -- e.g. only a Sätze-only speaker survived
 *   the exclusion while the training mode is set to Wörter.
 * - **Neither**: the pre-existing, generic text (Batch C AC7). An empty
 *   [excludedSpeakers] -- the AC7 bit-identical baseline -- always lands
 *   here, same as before this batch existed.
 *
 * [hasEntriesInLanguage] (ADR-0016) is checked before all of them: it says
 * whether the *unfiltered* corpus holds anything at all under the selected
 * training language. False means the drawer itself is empty, which no amount
 * of speaker selection can fix.
 */
fun emptyCorpusHint(
    excludedSpeakers: Set<String>,
    availableSpeakers: Set<String>,
    hasEntriesInLanguage: Boolean,
    strings: Strings,
): String = when {
    // Checked first because it is the most specific cause and the easiest to
    // walk into (ADR-0016): switching to a language nothing is filed under
    // yet used to end in the generic "Kein Wort im Korpus vorhanden", which
    // reads like a broken install rather than an empty drawer.
    !hasEntriesInLanguage -> strings.emptyForLanguage
    availableSpeakers.isNotEmpty() && (availableSpeakers - excludedSpeakers).isEmpty() -> strings.noContingentSelected
    excludedSpeakers.isNotEmpty() -> strings.nothingForSelectedContingents
    else -> strings.emptyCorpus
}
