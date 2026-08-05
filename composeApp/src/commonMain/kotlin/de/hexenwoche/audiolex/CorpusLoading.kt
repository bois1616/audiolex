package de.hexenwoche.audiolex

import de.hexenwoche.audiolex.core.corpus.EntryKind
import de.hexenwoche.audiolex.core.corpus.LoadedCorpus
import de.hexenwoche.audiolex.core.corpus.parseCorpus
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
 */
suspend fun loadCorpus(kind: EntryKind? = null): LoadedCorpus =
    parseCorpus(
        wordsJson = Res.readBytes("files/corpus/words.json").decodeToString(),
        recordingsJson = Res.readBytes("files/corpus/recordings.json").decodeToString(),
        kind = kind,
    )
