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
 * The English catalog (ADR-0015), and the fallback for any device language
 * the app has no catalog for -- see [UiLanguage.resolve].
 *
 * Written as English, not translated word for word: the German original
 * addresses the reader informally with "du", and the plain second person
 * carries that over without the register sounding stiff. Plurals are spelled
 * out here ("1 recording" / "3 recordings") where [GermanStrings] keeps its
 * "Aufnahme(n)" shorthand -- the shorthand is idiomatic German officialese
 * and has no English equivalent worth imitating.
 *
 * The terminology decisions worth stating, since they recur:
 * *Störgeräusch* is "background noise", *Prüfmodus* is "exam mode" (it is a
 * self-test, not a graded exam -- but "test mode" reads as a developer
 * feature), and *Kontingent* is just "speaker": the German word is a
 * modelling term (ADR-0012 Nachtrag) that never earned its keep in the UI.
 */
internal object EnglishStrings : Strings {

    // ---- Shared ----

    override val back = "Back"
    override val backToStart = "Back to start"
    override val quit = "End session"
    override val cancel = "Cancel"
    override val save = "Save"
    override val delete = "Delete"
    override val listen = "Listen"
    override val repeatPlayback = "Play again"
    override val settings = "Settings"
    override val sessionHistory = "Session history"
    override val ownRecordings = "My recordings"
    override val ownNoises = "My background noises"

    override fun playbackFailed(message: String?) = "Playback failed: $message"

    override fun corpusLoadFailed(message: String?) = "The corpus could not be loaded: $message"

    override fun deleteConfirmBody(name: String) =
        "“$name” will be removed for good, including the recording. This cannot be undone."

    override fun joinLast(parts: List<String>) = when (parts.size) {
        0, 1 -> parts.joinToString(", ")
        else -> parts.dropLast(1).joinToString(", ") + " and " + parts.last()
    }

    // ---- Start screen ----

    override val appSubtitle = "Hearing training: sound → word → meaning"
    override val startLearningMode = "Start learning mode"
    override val startExamMode = "Start exam mode"
    override val quickGuide = "Quick guide"
    override val exitApp = "Close app"
    override val imprintAndPrivacy = "Legal notice & privacy"
    override val languageChoice = "Language"

    // ---- Learning mode ----

    override val learningFinished = "Done — that was every word."
    override val previousEntry = "Previous"
    override val nextEntry = "Next"

    override fun noRecordingFound(text: String) = "No recording found for “$text”."

    override fun recordingFileMissing(fileRef: String) = "Recording “$fileRef” not found."

    // ---- Exam mode ----

    override val noCardsToPractise = "There are no cards to practise yet."
    override val learningModeInstead = "Learning mode instead"
    override val examFinished = "Done!"
    override val newExamRound = "Another round"
    override val nextCard = "Next"
    override val tapToReveal = "Tap to reveal"

    override fun cardsRatedSentence(count: Int) =
        if (count == 1) "1 card rated." else "$count cards rated."

    override fun wordForCardNotFound(cardId: String) = "No word found for card “$cardId”."

    override fun ratingLabel(rating: ReviewRating) = when (rating) {
        ReviewRating.AGAIN -> "Again"
        ReviewRating.SOON -> "Soon"
        ReviewRating.LATER -> "Later"
        ReviewRating.GOOD -> "Good"
        ReviewRating.PERFECT -> "Perfect"
    }

    override fun intervalHint(rating: ReviewRating) = when (rating) {
        ReviewRating.AGAIN -> "1 min"
        ReviewRating.SOON -> "10 min"
        ReviewRating.LATER -> "1 day"
        ReviewRating.GOOD -> "1 week"
        ReviewRating.PERFECT -> "1 month"
    }

    // ---- Session history ----

    override val noSessionsYet = "No sessions yet."
    override val noRatings = "no ratings"

    override fun sessionModeLabel(mode: String) = when (mode) {
        "PRUEFMODUS" -> "Exam mode"
        else -> mode
    }

    override fun sessionSummary(ratedCount: Int, ratingDetail: String) =
        if (ratedCount == 1) "1 card rated — $ratingDetail" else "$ratedCount cards rated — $ratingDetail"

    // ---- Empty corpus ----

    override val noContingentSelected =
        "No speaker selected. Pick at least one under “Corpus” in the settings."
    override val nothingForSelectedContingents =
        "The speakers you selected have nothing to train right now. " +
            "Adjust the selection under “Corpus”, or switch the training content in the settings."
    override val emptyCorpus = "There is no word in the corpus."
    override val emptyForLanguage =
        "Nothing is filed under this language yet. Pick another training language in the settings, " +
            "or record something in this one yourself."

    // ---- Settings ----

    override val sectionTrainingLevel = "Training level"
    override val customLevel = "Set by hand"
    override val sectionAppearance = "Appearance"
    override val sectionTrainingContent = "Training content"
    override val sectionTrainingLanguage = "Training language"
    override val trainingLanguageHint =
        "Applies to the words and sentences you train — not to the language of the interface."
    override val sectionCorpus = "Corpus"
    override val noContingentAvailable = "No speaker available."
    override val selectAll = "Select all"
    override val deselectAll = "Deselect all"
    override val sectionNoise = "Background noise"
    override val onOff = "On/off"
    override val quieter = "quieter"
    override val louder = "louder"
    override val sectionScenario = "Scenario"
    override val noNoiseYet = "No sound yet — playback stays clean."
    override val sectionOutput = "Output"
    override val sectionChannels = "Channels"
    override val channelsIneffectiveHint =
        "No effect on a hearing aid: it sums stereo to mono by itself. " +
            "Available with stereo headphones."
    override val noSpeaker = "No speaker"

    override fun profileLabel(profile: SettingsProfile) = when (profile) {
        SettingsProfile.EINFACH -> "Easy"
        SettingsProfile.SCHWIERIG -> "Hard"
        SettingsProfile.FORTGESCHRITTEN -> "Advanced"
    }

    override fun themeModeLabel(mode: ThemeMode) = when (mode) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }

    override fun corpusModeLabel(mode: CorpusMode) = when (mode) {
        CorpusMode.WOERTER -> "Words"
        CorpusMode.SAETZE -> "Sentences"
    }

    override fun corpusLanguageLabel(language: CorpusLanguage) = when (language) {
        CorpusLanguage.DEUTSCH -> "German"
        CorpusLanguage.ENGLISCH -> "English"
    }

    override fun channelModeLabel(mode: ChannelMode) = when (mode) {
        ChannelMode.BEIDE -> "Both"
        ChannelMode.NUR_LINKS -> "Left only"
        ChannelMode.NUR_RECHTS -> "Right only"
    }

    override fun outputSetupDetected(setup: OutputSetup) = when (setup) {
        OutputSetup.STEREO_KOPFHOERER -> "Stereo headphones detected"
        OutputSetup.HOERGERAET -> "Hearing aid detected"
    }

    override fun snrLabel(snrDb: Int) = if (snrDb >= 0) "SNR: +$snrDb dB" else "SNR: $snrDb dB"

    override fun wordCount(count: Int) = if (count == 1) "1 word" else "$count words"

    override fun sentenceCount(count: Int) = if (count == 1) "1 sentence" else "$count sentences"

    // ---- My recordings ----

    override val sectionNewRecording = "New recording"
    override val fieldText = "Text"
    override val fieldSpeakerOptional = "Speaker (optional)"
    override val fieldEntryLanguage = "Language"
    override val entryLanguageHint =
        "Decides which training language the entry shows up under — not what is spoken in it."
    override val untranscribedHint =
        "Without a text the entry cannot be trained. You can add it later via “Edit text”."
    override val sectionMyEntries = "My entries"
    override val noOwnRecordingsYet = "No recordings of your own yet."
    override val editText = "Edit text"
    override val reRecord = "Record again"
    override val closeRecording = "Close recorder"
    override val applyRecording = "Use this take"
    override val sectionBackup = "Backup"
    override val backupExplainer =
        "Otherwise your recordings, your background noises and your session history sit on this device only. " +
            "The export writes all of it into your documents as a ZIP file — what happens to it after that is yours to decide."
    override val export = "Export"
    override val importAction = "Import"
    override val deleteEntryTitle = "Delete entry?"

    override fun kindLabel(kind: EntryKind) = when (kind) {
        EntryKind.WORD -> "Word"
        EntryKind.SENTENCE -> "Sentence"
    }

    override fun noRecordingAvailable(text: String) = "No recording available for “$text”."

    // ---- Backup: export ----

    override val nothingToBackUp = "Nothing to back up: there is no recording, no sound and no session yet."
    override val backupWriteFailed = "Backup failed — the file could not be written."

    override fun recordingCount(count: Int) = if (count == 1) "1 recording" else "$count recordings"

    override fun noiseCount(count: Int) = if (count == 1) "1 sound" else "$count sounds"

    override fun sessionCount(count: Int) = if (count == 1) "1 session" else "$count sessions"

    override fun entryCount(count: Int) = if (count == 1) "1 entry" else "$count entries"

    override fun backupSaved(what: String, location: String) = "$what saved to $location."

    override fun backupSkippedWithoutRecording(count: Int) =
        if (count == 1) {
            " 1 entry without a recording is not included."
        } else {
            " $count entries without a recording are not included."
        }

    // ---- Backup: import ----

    override val archiveUnreadable =
        "This file is not an AudioLex backup, or it is damaged. Nothing was changed."
    override val importNothingAdded = "Nothing added."

    override fun importAdded(added: String) = "$added taken over"

    override fun importAlsoAlreadyPresent(known: String) = ", $known were already here"

    override fun importOnlyAlreadyPresent(known: String) =
        "Nothing added — $known from the backup are already here."

    override fun importSkippedEntries(count: Int) =
        if (count == 1) {
            " 1 entry in the backup was incomplete and was skipped."
        } else {
            " $count entries in the backup were incomplete and were skipped."
        }

    override fun importSkippedNoises(count: Int) =
        if (count == 1) {
            " 1 sound in the backup was incomplete and was skipped."
        } else {
            " $count sounds in the backup were incomplete and were skipped."
        }

    // ---- My background noises ----

    override val sectionNewNoise = "New sound"
    override val noiseRecordingHint =
        "Start the recording with the sound itself; silence at the beginning is audible before every word later on."
    override val fieldLabel = "Name"
    override val sectionImport = "Import"
    override val wavImportHint = "Existing WAV files can be taken over — format: PCM, mono, 22050 Hz."
    override val importWavFile = "Import WAV file"
    override val sectionMyNoises = "My sounds"
    override val noOwnNoisesYet = "No sounds of your own yet."
    override val deleteNoiseTitle = "Delete sound?"
    override val wavNotReadable =
        "This file could not be read as a WAV. " +
            "Pick a WAV file (PCM, 16 bit) — or record the sound here directly."

    override fun recordingLabelSuggestion(timestamp: String) = "Recording $timestamp"

    override fun fileChosen(name: String) = "$name selected"

    override fun noiseSourceLabel(source: OwnNoiseSource) = when (source) {
        OwnNoiseSource.IMPORT -> "Imported"
        OwnNoiseSource.AUFNAHME -> "Recorded"
    }

    override fun wavWrongFormat(sampleRate: Int, channels: Int) =
        "This file has $sampleRate Hz and ${channelCount(channels)}. " +
            "AudioLex takes background noise only as WAV with 22050 Hz, mono — " +
            "convert the file, or record the sound here directly."

    override fun channelCount(count: Int) = if (count == 1) "1 channel" else "$count channels"

    // ---- Recorder controls ----

    override val recorderIdle = "Nothing recorded yet."
    override val recorderRecording = "Recording…"
    override val recorderProcessing = "Processing the recording…"
    override val recorderNoSignal = "Nothing recorded (no signal came in)."
    override val record = "Record"
    override val stopRecording = "Stop"
    override val micPermanentlyDenied =
        "Microphone access was refused permanently. To record, allow it in the system settings."
    override val openSystemSettings = "Open settings"
    override val micNeeded = "To record, AudioLex needs brief access to the microphone."
    override val requestMicPermission = "Ask for microphone permission"

    override fun recorderFinished(seconds: Int) = "Recording done (~$seconds s)."

    override fun recorderFailed(message: String?) = "The recording failed: $message"

    // ---- Legal notice & privacy ----

    override val imprintHeading = "Legal notice"
    override val imprintResponsible =
        "Responsible for this app:\n\nStephan Reindl\nEmail: audiolex26@proton.me"
    override val imprintNonCommercial =
        "A non-commercial project. The source code is open (Apache-2.0) and the app " +
            "is provided without any financial interest."
    override val imprintNotMedical =
        "AudioLex is a private practice tool, not a professional or medical product. " +
            "It replaces neither the advice of a hearing-aid audiologist nor an examination " +
            "by an ear, nose and throat specialist."
    override val imprintVibeCoded =
        "AudioLex was built together with Claude Code — vibe-coded: the idea, the domain " +
            "concept, the decisions and the sign-off come from the author, a large part of " +
            "the source code from an AI. You can check that in the open repository: every " +
            "substantial decision is written down there as an ADR, every implementation step " +
            "in the journal."
    override val imprintAsIs =
        "Use it as you find it: the app is provided as it is — without warranty, and without " +
            "any promise that the training works, that the app runs without faults, or that " +
            "your data survives. You use it at your own risk; no liability is accepted for " +
            "damage arising from it. The Apache-2.0 licence the source code stands under says " +
            "the same."
    override val privacyHeading = "Privacy"
    override val privacyNoTrackers = "AudioLex has no trackers, no analytics services and no advertising."
    override val privacyLocalOnly =
        "Vocabulary, ratings and session history stay on this device alone. There is no cloud, " +
            "no account, no transfer to the internet."
    override val privacyOnePermission =
        "AudioLex asks for a single Android permission: the microphone, and only for recordings " +
            "of your own words and sentences that you start yourself. There is no internet " +
            "permission. The app cannot transmit anything by itself."
    override val privacyBackupIsYours =
        "Your own recordings and your session history can be backed up: at the press of a button " +
            "AudioLex writes both into your documents as a ZIP file. That happens only when you " +
            "trigger it. What happens to the file afterwards — copying it, passing it on, putting " +
            "it in a cloud — is your decision."
    override val privacyNoSystemBackup =
        "Android's automatic backup is switched off for AudioLex, because it would have " +
            "transferred the recordings into a Google account unasked. That has a flip side: " +
            "without an export of your own there is no copy."

    // ---- Quick guide ----

    override val guideTitle = "Quick guide"
    override val guideIntro =
        "The app plays a word, you match it, and how well it landed decides when it comes back. " +
            "You need headphones or your hearing aid and a quiet minute, nothing else."
    override val guideLearningHeading = "Learning mode"
    override val guideLearningBody =
        "Hear the word, read along. “Play again” repeats it, “Next” moves on, “Previous” steps " +
            "back. Nothing is rated here — this mode builds the link between the sound and the " +
            "written word. Start here while a word is still unfamiliar."
    override val guideExamHeading = "Exam mode"
    override val guideExamBody =
        "The word plays while the card stays covered. Tap it to see the text, then rate yourself. " +
            "The card is always the same size, so its silhouette gives nothing away about the " +
            "length of the word."
    override val guideRatingBody =
        "Five steps decide when the card returns: Again after 1 minute, Soon after 10 minutes, " +
            "Later after a day, Good after a week, Perfect after a month. These are not marks. " +
            "“Again” is not a bad result — it says you want to hear the word once more, right away."
    override val guideSettingsHeading = "Settings"
    override val guideSettingsLevel =
        "The training level is the one-tap preset: Easy switches the background noise off, Hard " +
            "puts it audibly below the speech (SNR +5 dB), Advanced above it (SNR −5 dB). Move the " +
            "slider yourself afterwards and the line reads “Set by hand”."
    override val guideSettingsContent =
        "Training content switches between words and sentences. Under “Corpus” you pick which " +
            "speakers take part — the bundled voice is one of them."
    override val guideSettingsNoise =
        "Background noise lays a loop under the speech. The slider sets the distance in decibels; " +
            "further to the right means louder noise. Until you record or import a sound of your " +
            "own, playback stays clean."
    override val guideSettingsOutput =
        "“Output” shows what the app currently detects. On a Bluetooth hearing aid the channel " +
            "choice has no effect — it sums stereo to mono — which is why it is greyed out there " +
            "rather than hidden."
    override val guideOwnRecordingsHeading = "My recordings"
    override val guideOwnRecordingsBody =
        "You can speak words and sentences yourself and write the matching text. Without a text an " +
            "entry cannot be trained, but you can add it any time. The speaker name keeps voices " +
            "apart; in the settings you switch individual ones on or off."
    override val guideBackupHeading = "Backup"
    override val guideBackupBody =
        "Nothing leaves this device by itself — the app has no internet permission. The flip side: " +
            "without an export of your own there is no copy. “Export” writes recordings, sounds and " +
            "session history into your documents as a ZIP, “Import” reads them back and adds only " +
            "what is missing."

    // ---- Channel test ----

    override val channelTestTitle = "Channel test"
    override val channelTestExplainer =
        "Both ears get three different words at the same time. “Left only” sets the right channel to " +
            "exactly zero — whatever you still hear on the right is not coming from the app."
    override val channelTestLoading = "Loading corpus…"
    override val channelTestReady = "Ready"
    override val channelTestTooFewWords =
        "Not enough words for the channel test: it needs six with a recording in the training " +
            "language you picked."

    override val channelTestSettingInactive =
        "The channel selection in the settings has no effect with this — the test below still separates the ears."

    override fun channelTestOutput(setup: OutputSetup, devices: List<String>): String {
        val reported = if (devices.isEmpty()) "no devices reported" else devices.joinToString(", ")
        return "Output: ${outputSetupDetected(setup)} · $reported"
    }

    override fun channelTestLeftEar(words: List<String>) = "Left: ${words.joinToString(" · ")}"

    override fun channelTestRightEar(words: List<String>) = "Right: ${words.joinToString(" · ")}"

    override fun channelTestPlaying(channels: String) = "Channel test: $channels…"

    override fun channelTestPlayingWord(word: String, speaker: String) = "Playing “$word” ($speaker)…"
}
