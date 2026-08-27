package de.hexenwoche.audiolex.core.i18n

import de.hexenwoche.audiolex.core.audio.OutputSetup
import de.hexenwoche.audiolex.core.audio.OwnNoiseSource
import de.hexenwoche.audiolex.core.corpus.CorpusLanguage
import de.hexenwoche.audiolex.core.corpus.EntryKind
import de.hexenwoche.audiolex.core.settings.ChannelMode
import de.hexenwoche.audiolex.core.settings.CorpusMode
import de.hexenwoche.audiolex.core.settings.SettingsProfile
import de.hexenwoche.audiolex.core.settings.ThemeMode
import de.hexenwoche.audiolex.core.srs.ReviewRating

/**
 * Every piece of text the UI shows, as one typed contract per language
 * (ADR-0015). [GermanStrings] and [EnglishStrings] implement it; nothing
 * else does.
 *
 * **Why an interface and not resource files.** A missing translation here is
 * a compile error, not a blank label discovered on a device -- with two
 * catalogs maintained by one person, that guarantee is worth more than the
 * tooling around `strings.xml`. The other reason is switching at runtime:
 * Compose Resources 1.8.2 resolves the locale from the platform and offers
 * no supported override, so a language picker inside the app would have had
 * to fight the framework. Reading a value out of a [Strings] object costs a
 * recomposition and nothing else.
 *
 * **Why it lives in `:core`.** UI wording in a module described as
 * "plattformfreie Logik" is a stretch, and it was the deciding trade-off:
 * `:composeApp` has no test source set, `:core:jvmTest` is the fast loop
 * every DoD run already executes. Nothing here touches Compose -- these are
 * plain strings and `when` mappings over domain enums, and the mappings are
 * the part actually worth a test.
 *
 * **Plurals** are per-language functions, not format placeholders. German
 * keeps the "Aufnahme(n)" shorthand it already used before this existed;
 * English says "1 recording" / "3 recordings" properly. Each language
 * answers in its own idiom instead of a shared template that fits neither.
 *
 * Members carry no `Strings.` prefix in their names and are grouped by the
 * screen they serve. Anything used on more than one screen sits in the
 * shared block at the top.
 */
interface Strings {

    // ---- Shared across screens ----

    val back: String
    val backToStart: String
    val quit: String
    val cancel: String
    val save: String
    val delete: String
    val listen: String
    val repeatPlayback: String
    val settings: String
    val sessionHistory: String
    val ownRecordings: String
    val ownNoises: String

    fun playbackFailed(message: String?): String
    fun corpusLoadFailed(message: String?): String

    /** Delete confirmation body, shared word for word by the entry and the noise dialog. */
    fun deleteConfirmBody(name: String): String

    /** "a, b, c" -> "a, b und c" / "a, b and c". */
    fun joinLast(parts: List<String>): String

    // ---- Start screen ----

    val appSubtitle: String
    val startLearningMode: String
    val startExamMode: String
    val quickGuide: String
    val exitApp: String
    val imprintAndPrivacy: String

    /** Screen-reader label for the language row; the buttons themselves show [UiLanguage.nativeName]. */
    val languageChoice: String

    // ---- Lernmodus ----

    val learningFinished: String
    val previousEntry: String
    val nextEntry: String

    fun noRecordingFound(text: String): String

    /** Thrown when an own recording's file has vanished; surfaces through [playbackFailed]. */
    fun recordingFileMissing(fileRef: String): String

    // ---- Prüfmodus ----

    val noCardsToPractise: String
    val learningModeInstead: String
    val examFinished: String
    val newExamRound: String
    val nextCard: String
    val tapToReveal: String

    fun cardsRatedSentence(count: Int): String
    fun wordForCardNotFound(cardId: String): String
    fun ratingLabel(rating: ReviewRating): String
    fun intervalHint(rating: ReviewRating): String

    // ---- Sitzungshistorie ----

    val noSessionsYet: String
    val noRatings: String

    fun sessionModeLabel(mode: String): String
    fun sessionSummary(ratedCount: Int, ratingDetail: String): String

    // ---- Empty-corpus explanations (both training screens) ----

    val noContingentSelected: String
    val nothingForSelectedContingents: String
    val emptyCorpus: String

    /** Nothing is filed under the selected corpus language yet (ADR-0016). */
    val emptyForLanguage: String

    // ---- Einstellungen ----

    val sectionTrainingLevel: String
    val customLevel: String
    val sectionAppearance: String
    val sectionTrainingContent: String
    val sectionTrainingLanguage: String
    val trainingLanguageHint: String
    val sectionCorpus: String
    val noContingentAvailable: String
    val selectAll: String
    val deselectAll: String
    val sectionNoise: String
    val onOff: String
    val quieter: String
    val louder: String
    val sectionScenario: String
    val noNoiseYet: String
    val sectionOutput: String
    val sectionChannels: String
    val channelsIneffectiveHint: String
    val noSpeaker: String

    fun profileLabel(profile: SettingsProfile): String
    fun themeModeLabel(mode: ThemeMode): String
    fun corpusModeLabel(mode: CorpusMode): String

    /**
     * The language named in the language the reader is currently using
     * ("Englisch" in a German UI) -- unlike the UI picker, which names each
     * language in itself. Here the reader is choosing *content* and is by
     * definition reading the current UI language.
     */
    fun corpusLanguageLabel(language: CorpusLanguage): String
    fun channelModeLabel(mode: ChannelMode): String
    fun outputSetupDetected(setup: OutputSetup): String
    fun snrLabel(snrDb: Int): String
    fun wordCount(count: Int): String
    fun sentenceCount(count: Int): String

    // ---- Eigene Aufnahmen ----

    val sectionNewRecording: String
    val fieldText: String
    val fieldSpeakerOptional: String
    val fieldEntryLanguage: String
    val entryLanguageHint: String
    val untranscribedHint: String
    val sectionMyEntries: String
    val noOwnRecordingsYet: String
    val editText: String
    val reRecord: String
    val closeRecording: String
    val applyRecording: String
    val sectionBackup: String
    val backupExplainer: String
    val export: String
    val importAction: String
    val deleteEntryTitle: String

    fun kindLabel(kind: EntryKind): String
    fun noRecordingAvailable(text: String): String

    // ---- Sicherung: export ----

    val nothingToBackUp: String
    val backupWriteFailed: String

    fun recordingCount(count: Int): String
    fun noiseCount(count: Int): String
    fun sessionCount(count: Int): String
    fun entryCount(count: Int): String
    fun backupSaved(what: String, location: String): String
    fun backupSkippedWithoutRecording(count: Int): String

    // ---- Sicherung: import ----

    val archiveUnreadable: String
    val importNothingAdded: String

    fun importAdded(added: String): String
    fun importAlsoAlreadyPresent(known: String): String
    fun importOnlyAlreadyPresent(known: String): String
    fun importSkippedEntries(count: Int): String
    fun importSkippedNoises(count: Int): String

    // ---- Eigene Störgeräusche ----

    val sectionNewNoise: String
    val noiseRecordingHint: String
    val fieldLabel: String
    val sectionImport: String
    val wavImportHint: String
    val importWavFile: String
    val sectionMyNoises: String
    val noOwnNoisesYet: String
    val deleteNoiseTitle: String
    val wavNotReadable: String

    fun recordingLabelSuggestion(timestamp: String): String
    fun fileChosen(name: String): String
    fun noiseSourceLabel(source: OwnNoiseSource): String
    fun wavWrongFormat(sampleRate: Int, channels: Int): String
    fun channelCount(count: Int): String

    // ---- Aufnahme-Steuerung (in jeder Aufnahmemaske) ----

    val recorderIdle: String
    val recorderRecording: String
    val recorderProcessing: String
    val recorderNoSignal: String
    val record: String
    val stopRecording: String
    val micPermanentlyDenied: String
    val openSystemSettings: String
    val micNeeded: String
    val requestMicPermission: String

    fun recorderFinished(seconds: Int): String
    fun recorderFailed(message: String?): String

    // ---- Impressum & Datenschutz ----

    val imprintHeading: String
    val imprintResponsible: String
    val imprintNonCommercial: String
    val imprintNotMedical: String
    val imprintVibeCoded: String
    val imprintAsIs: String
    val privacyHeading: String
    val privacyNoTrackers: String
    val privacyLocalOnly: String
    val privacyOnePermission: String
    val privacyBackupIsYours: String
    val privacyNoSystemBackup: String

    // ---- Kurzanleitung ----

    val guideTitle: String
    val guideIntro: String
    val guideLearningHeading: String
    val guideLearningBody: String
    val guideExamHeading: String
    val guideExamBody: String
    val guideRatingBody: String
    val guideSettingsHeading: String
    val guideSettingsLevel: String
    val guideSettingsContent: String
    val guideSettingsNoise: String
    val guideSettingsOutput: String
    val guideOwnRecordingsHeading: String
    val guideOwnRecordingsBody: String
    val guideBackupHeading: String
    val guideBackupBody: String

    // ---- Kanaltest ----

    val channelTestTitle: String
    val channelTestExplainer: String
    val channelTestLoading: String
    val channelTestReady: String
    val channelTestTooFewWords: String

    val channelTestSettingInactive: String

    /**
     * The detection's own answer, verbatim: which setup it decided on and
     * which device types it read that from ("Ausgabe: Stereo-Kopfhörer
     * erkannt · USB_HEADSET (22)"). The type names stay in Android's
     * spelling in both languages -- they exist to survive a bug report, not
     * to read well.
     */
    fun channelTestOutput(setup: OutputSetup, devices: List<String>): String

    fun channelTestLeftEar(words: List<String>): String
    fun channelTestRightEar(words: List<String>): String
    fun channelTestPlaying(channels: String): String
    fun channelTestPlayingWord(word: String, speaker: String): String
}
