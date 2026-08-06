package de.hexenwoche.audiolex

import de.hexenwoche.audiolex.core.corpus.AudioRecording
import de.hexenwoche.audiolex.core.corpus.EntryKind
import de.hexenwoche.audiolex.core.corpus.LoadedCorpus
import de.hexenwoche.audiolex.core.corpus.RecordingSource
import de.hexenwoche.audiolex.core.corpus.mergeCorpus
import de.hexenwoche.audiolex.core.corpus.parseCorpus
import de.hexenwoche.audiolex.core.settings.CorpusSource
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
 * unfiltered (the dev channel test lists all entries). Every screen entry
 * loads fresh -- no caching across screens, at this corpus size reloading
 * two small JSON files is trivial.
 *
 * [source] decides *which side even gets loaded* (Backlog Eigen-Korpus
 * Batch C, AC3): the packed Compose resources are skipped entirely for
 * [CorpusSource.EIGENE], and [ownCorpusRepository] is never asked for
 * [de.hexenwoche.audiolex.OwnCorpusRepository.trainable] for
 * [CorpusSource.MITGELIEFERT] -- there's no point loading a side the caller
 * didn't select. [de.hexenwoche.audiolex.core.corpus.mergeCorpus] itself
 * stays unaware of [CorpusSource] entirely; see its doc for why. The dev
 * channel test doesn't pass [source]/[ownCorpusRepository] at all, so it
 * keeps loading exactly the mitgeliefert corpus it always has.
 */
suspend fun loadCorpus(
    kind: EntryKind? = null,
    source: CorpusSource = CorpusSource.MITGELIEFERT,
    ownCorpusRepository: OwnCorpusRepository? = null,
): LoadedCorpus {
    val builtIn = if (source == CorpusSource.EIGENE) {
        LoadedCorpus(emptyList(), emptyList())
    } else {
        parseCorpus(
            wordsJson = Res.readBytes("files/corpus/words.json").decodeToString(),
            recordingsJson = Res.readBytes("files/corpus/recordings.json").decodeToString(),
            kind = null,
        )
    }
    val ownEntries = if (source == CorpusSource.MITGELIEFERT) {
        emptyList()
    } else {
        ownCorpusRepository?.trainable() ?: emptyList()
    }
    return mergeCorpus(builtIn.words, builtIn.recordings, ownEntries, kind)
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
 * existing calm onError path ("Wiedergabe fehlgeschlagen: …"), the same
 * outcome a missing built-in resource would already hit today -- no second
 * failure mechanism needed for the second source.
 */
suspend fun readRecordingBytes(recording: AudioRecording, ownCorpusRepository: OwnCorpusRepository): ByteArray =
    when (recording.source) {
        RecordingSource.MITGELIEFERT -> Res.readBytes("files/corpus/${recording.fileRef}")
        RecordingSource.EIGEN -> ownCorpusRepository.recordingBytes(recording.fileRef)
            ?: error("Aufnahme „${recording.fileRef}“ nicht gefunden.")
    }

/**
 * Explains *why* the corpus came back empty (Backlog Eigen-Korpus Batch C,
 * AC7). The pre-existing text only ever pointed at the Wörter/Sätze switch
 * -- accurate as long as [CorpusSource.MITGELIEFERT] was the only possible
 * source. An empty result can now just as well mean "Eigene Aufnahmen" is
 * selected but nothing trainable has been recorded yet (AC4: text-only
 * entries and entries whose file has gone missing don't count) -- an
 * unrelated cause the old text would have misdescribed.
 */
fun emptyCorpusHint(source: CorpusSource): String = when (source) {
    CorpusSource.EIGENE ->
        "Für „Eigene Aufnahmen“ liegt noch kein trainierbarer Eintrag vor. " +
            "Neue Aufnahmen entstehen unter „Eigene Aufnahmen“ auf dem Start-Screen."
    CorpusSource.MITGELIEFERT, CorpusSource.BEIDE -> "Kein Wort im Korpus vorhanden."
}
